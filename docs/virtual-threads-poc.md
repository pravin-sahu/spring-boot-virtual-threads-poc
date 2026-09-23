# Virtual Threads PoC — Learn & Apply Virtual Threads in Spring Boot

A small, self-contained proof of concept inside this boilerplate showing **how Java virtual threads
differ from platform threads**. It shows that they help **I/O-bound** (waiting-heavy) workloads and
that they **do not** speed up **CPU-bound** workloads.

- Code: `src/main/java/com/mb/modules/virtualthread/`
- Tests: `src/test/java/com/mb/modules/virtualthread/`
- Profile: `src/main/resources/application-virtual-thread-poc.yml`

> **PoC-only, profile-gated:** the controller, service and config are annotated
> `@Profile("virtual-thread-poc")`. Without that profile the `/v1/virtual-threads/**` endpoints do not
> exist (404). With it, they are `permitAll()` in `SecurityConfig` so they can be called without a
> token. The rule matches nothing in other profiles. Remove the rule together with
> `modules/virtualthread` before using this boilerplate for a real project.

---

## 1. What virtual threads are

Virtual threads (JEP 444, final in Java 21) are `java.lang.Thread` instances that are **scheduled by
the JVM instead of the operating system**. They have the same API as ordinary threads (`Thread`,
`ExecutorService`, `ThreadLocal`, blocking calls), but they are cheap: a few hundred bytes of
metadata, plus a stack that lives on the heap and grows only as needed. That makes it realistic to
create **one thread per task** — thousands or millions of them — instead of sharing a small pool.

## 2. Platform threads vs virtual threads

| | Platform thread | Virtual thread |
| :--- | :--- | :--- |
| Backed by | One OS thread for its whole life | Borrowed carrier thread, only while running |
| Scheduled by | OS kernel | JVM (`ForkJoinPool`, FIFO) |
| Creation cost | High (OS thread, ~1 MB reserved stack) | Low (heap object) |
| Practical count | Thousands | Millions |
| Pool it? | Yes — expensive to create | No — create per task, discard afterwards |
| Blocking call | Blocks the OS thread | Unmounts; carrier is freed for other work |
| Good for | Anything, but concurrency is capped by pool size | High-concurrency, mostly-waiting tasks |
| `isVirtual()` | `false` | `true` |

## 3. Carrier threads (high level)

A virtual thread cannot run on its own. The JVM **mounts** it on a platform thread called a **carrier
thread**, taken from a dedicated `ForkJoinPool` whose parallelism defaults to the number of available
processors (8 on the benchmark machine). You can see the carrier in `Thread.toString()`:

```
VirtualThread[#69,tomcat-handler-1]/runnable@ForkJoinPool-1-worker-3
             └── virtual thread ──┘           └──── carrier ────┘
```

## 4. Mount / unmount (high level)

```
     virtual threads (many, cheap)               carrier threads (= CPU cores)
  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐           ┌───────────────┐
  │ V1 │ │ V2 │ │ V3 │ │ V4 │ │... │  mount ─▶ │ worker-1  (V1)│──▶ CPU
  └────┘ └────┘ └────┘ └────┘ └────┘           │ worker-2  (V3)│──▶ CPU
     ▲                                         │ ...           │
     │  unmount on blocking call               └───────────────┘
     │  (sleep, socket read, lock wait):
     │  stack is copied to the heap and the carrier is free for another virtual thread.
     └─ when the blocking operation completes, the virtual thread is re-scheduled and mounted
        again, possibly on a different carrier.
```

- **Mount:** the scheduler copies the virtual thread's stack frames onto a carrier and runs it.
- **Unmount:** when the virtual thread blocks in a JDK operation that supports it, its frames move to
  the heap and the carrier picks up the next runnable virtual thread.
- **Pinning:** situations where the virtual thread *cannot* unmount and holds its carrier while
  blocked. Since **Java 24 (JEP 491)** `synchronized` no longer pins. Native frames / JNI calls
  and a few class-loading paths still do. This project runs on **Java 25**.

## 5. Why virtual threads help I/O-bound workloads

An I/O-bound task spends most of its time **waiting** (database, HTTP call, file). With platform
threads each waiting task holds an expensive OS thread, so concurrency is capped by the pool size.
Everything beyond that queues:

```
time needed ≈ ceil(tasks / poolSize) × wait
```

With virtual threads a waiting task holds only a small heap object. The carrier is released, so
*all* tasks can wait at the same time:

```
time needed ≈ wait  (+ small scheduling overhead)
```

Virtual threads do not make a single wait shorter. They increase **how many waits can overlap**,
which raises **throughput**. Latency improves only when requests were previously **queuing for a
free thread**, as in §12.3. The time spent in the wait itself is unchanged.

## 6. Why they do not improve CPU-bound workloads

A CPU-bound task never blocks, so it never unmounts. Only as many virtual threads as there are
carriers (≈ cores) can run at once, which is exactly the limit a right-sized platform pool has.
Virtual threads **do not create CPU capacity**: the work still has to be executed by the same cores.
At best they perform about the same. The FIFO scheduler also does not time-slice, so one long
computation can delay other virtual threads that share its carrier.

## 7. When virtual threads are appropriate

**Good fit**
- Thread-per-request servers with many concurrent requests that mostly wait (REST APIs calling DBs or
  other services).
- Fan-out: calling several downstream services in parallel for one request.
- Replacing asynchronous/reactive code whose only goal was "don't block threads" with simple blocking
  code.

**Poor fit / no benefit**
- CPU-heavy computation (image processing, crypto, number crunching). Use a pool sized to the cores.
- Low concurrency. If you never have more concurrent tasks than a normal pool can hold, nothing
  changes.

## 8. Limitations and caveats

- **Downstream limits still apply.** Virtual threads remove the *thread* bottleneck, not the others.
  A HikariCP pool of 10 connections still allows 10 concurrent queries. Thousands of virtual threads
  will just queue on the pool. Rate limits, sockets and the database's own capacity are unchanged.
  Use semaphores or connection pools to bound access to scarce resources.
- **Do not pool virtual threads.** Create one per task (`newVirtualThreadPerTaskExecutor()`).
- **`ThreadLocal`** works, but with millions of threads a large per-thread cache multiplies memory.
  Prefer passing context explicitly. Scoped values (final in Java 25) are the lighter alternative.
- **Pinning:** native/JNI frames still pin (see §4). `synchronized` no longer does on Java 24+.
- **CPU-bound work:** no improvement (§6), and there is no time-slicing between virtual threads.
- **Observability:** thread dumps can contain huge numbers of threads. Use
  `jcmd <pid> Thread.dump_to_file -format=json <file>`.
- **`Thread.sleep()` in this PoC is a simulation.** Like a blocking socket read, it parks the virtual
  thread and frees the carrier. Socket reads additionally go through the JDK's I/O poller. It does
  not model drivers, connection pools, network latency variance or server load. Real I/O will not
  scale as perfectly as the numbers below.

## 9. How Spring Boot enables virtual threads

Spring Boot (3.2+, here 4.1.0 on Java 25) needs a single property:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

When it is set, Spring Boot:
- runs Tomcat request handling on virtual threads (thread names `tomcat-handler-N` instead of
  `http-nio-<port>-exec-N`), which removes the `server.tomcat.threads.max` (default 200) cap,
- uses a virtual-thread `SimpleAsyncTaskExecutor` for `@Async` and a virtual-thread scheduler for
  `@Scheduled`.

In this project the property lives **only** in the `virtual-thread-poc` profile
(`application-virtual-thread-poc.yml`). The PoC beans are also gated by that profile. The default
threading of the boilerplate is unchanged.

`application.yml` also sets `spring.profiles.default: local`. Starting the app with **no** profile
(e.g. from the IDE) therefore uses the `local` profile. Deployed environments always set a profile
explicitly, so they are unaffected.

## 10. How to run the PoC

All commands run from the project root on **JDK 25** (the version configured in `pom.xml`). If your
default JDK is older, set `JAVA_HOME`, e.g. `export JAVA_HOME=~/.sdkman/candidates/java/25-tem`.

### 10.1 Basics demo (plain Java, no Spring, no DB)

```bash
mvn -q compile
java -cp target/classes com.mb.modules.virtualthread.basics.VirtualThreadBasicsDemo
```

It shows `Thread.ofPlatform()`, `Thread.ofVirtual()`, `Thread.startVirtualThread()`, `isVirtual()`, a
virtual thread before and after blocking (mount/unmount), and `Executors.newVirtualThreadPerTaskExecutor()`.
Sample output from this machine:

```
[1] Platform thread -> name='demo-platform', isVirtual=false, Thread[#25,demo-platform,5,main]
[2] Virtual thread  -> name='demo-virtual', isVirtual=true, VirtualThread[#27,demo-virtual]/runnable@ForkJoinPool-1-worker-1
[5] newVirtualThreadPerTaskExecutor task-1 -> name='', isVirtual=true, VirtualThread[#34]/runnable@ForkJoinPool-1-worker-2
[6] main thread -> name='main', isVirtual=false, Thread[#3,main,5,main]
```

### 10.2 The application and its endpoints

The app needs PostgreSQL (the boilerplate's JPA/Liquibase). **One-time setup:** create
`src/main/resources/application-local.yml`. It is gitignored, so credentials stay out of git.

```yaml
server:
  port: 8081            # optional; default is 8001
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/<your_db>
    username: <user>
    password: <password>
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  liquibase:
    contexts: local
app:
  cors:
    allowed:
      origins: http://localhost:3000
```

```bash
mvn clean package -DskipTests

# Virtual threads ON (PoC profile). List local too: setting any profile disables the default one.
mvn spring-boot:run -Dspring-boot.run.profiles=local,virtual-thread-poc
# or: java -jar target/spring-rest-0.0.1.jar --spring.profiles.active=local,virtual-thread-poc

# Virtual threads OFF, for comparison (the PoC endpoints stay available)
java -jar target/spring-rest-0.0.1.jar --spring.profiles.active=local,virtual-thread-poc \
     --spring.threads.virtual.enabled=false

# Without the PoC profile the app runs exactly as before, and /v1/virtual-threads/** returns 404
java -jar target/spring-rest-0.0.1.jar
```

| Endpoint | Purpose |
| :--- | :--- |
| `GET /v1/virtual-threads/info` | Is this request on a virtual thread? Name, id and `toString()` (which names the carrier) |
| `GET /v1/virtual-threads/io?delayMs=200` | One simulated I/O wait on the request thread (0–10,000 ms). Used for the Tomcat load test |
| `GET /v1/virtual-threads/compare?workload=IO&tasks=2000&delayMs=100&poolSize=100&runs=3` | **The PoC in one call:** runs the same workload on a platform pool and on virtual threads, and returns both sides plus a verdict |

`/compare` parameters:

| Parameter | Default | Range | Meaning |
| :--- | :--- | :--- | :--- |
| `workload` | `IO` | `IO`, `CPU` | Waiting (`Thread.sleep`) or computing (prime counting) |
| `tasks` | 2000 for IO, 2 × cores for CPU | 1–5000, CPU max 64 | Number of tasks |
| `delayMs` | 100 | 0–2000 | Simulated wait per task (IO) |
| `primeLimit` | 2000000 | 1000–3000000 | Work per task (CPU) |
| `poolSize` | 100 for IO, cores for CPU | 1–500 | Platform pool size |
| `runs` | 1 | 1–3 | Repetitions per mode; the times are averaged |

Requests whose platform side would take longer than 60 seconds are rejected with 400
`INVALID_REQUEST`, because the endpoint is public while the PoC profile is active.

### 10.3 Guided demo

```bash
./demo.sh          # all steps, pausing between each
./demo.sh 3        # only the comparison step
```
Step 1 runs the basics demo; steps 2–4 call the running app. See
[`demo-script.md`](demo-script.md) for the talk runbook.

### 10.4 Tests

```bash
mvn test     # unit + @WebMvcTest + Testcontainers integration tests
mvn verify   # + Spotless, SpotBugs, PMD, JaCoCo
```

Tests only check correctness (task counts, thread type, concurrency bounds, checksums), never
timings, so they are not flaky under the parallel Surefire configuration.

## 11. Methodology

```
GET /v1/virtual-threads/compare?workload=io&tasks=2000&delayMs=100&poolSize=100&runs=3
          │
          ▼
VirtualThreadController            validates the parameters (@Min / @Max)
          ▼
VirtualThreadServiceImpl           builds ONE Callable for the chosen workload,
          │                        then runs it in both modes, `runs` times each
          ▼
WorkloadRunner.run(mode, tasks, poolSize, task)
          │   wrap each task:  tracker.enter() → isVirtual()? → work → tracker.exit()
          │   start timer ─▶ create executor ─▶ invokeAll ─▶ sum results ─▶ close ─▶ stop timer
          ▼
   ┌───────────────────────────────┐     ┌──────────────────────────────────────┐
   │ PLATFORM                      │     │ VIRTUAL                              │
   │ Executors.newFixedThreadPool  │     │ Executors.newVirtualThreadPerTask... │
   └───────────────────────────────┘     └──────────────────────────────────────┘
          ▼
RunResult per run → averaged into the response, with a plain-English summary
```

- **Same workload in both modes.** The *same* `Callable` instance is passed to both. Each task
  returns a value (`1` for I/O, the prime count for CPU) and the sums are compared:
  `sameWorkVerified` is `true` only when both checksums match.
- **What is timed.** Wall-clock (`System.nanoTime`) from executor creation until every task has
  finished and the executor is closed. Thread creation is included for both modes.
- **Repetitions.** `runs=3` repeats each mode three times; the response shows every run in `runsMs`
  plus the average in `elapsedMs`.
- **Instrumentation.** `ConcurrencyTracker` records the peak number of tasks in flight, each task
  records whether it ran on a virtual thread, and the first task's `Thread.toString()` is returned
  as `sampleThread`.
- **I/O workload.** `Thread.sleep(delayMs)`. **This simulates waiting, not real database or network
  I/O.**
- **CPU workload.** Deterministic trial-division prime count (148,933 primes below 2,000,000),
  about 285 ms per task on this machine. Bounded to 64 tasks so the machine stays responsive.

**Known methodology limitations**
- Measurements happen **inside the running application**, so Tomcat, the JIT compiler and GC add
  noise. This matters for the CPU comparison, where the real difference is within that noise.
- There is **no warm-up run**, and the platform mode always runs **before** the virtual mode. On a
  laptop that throttles as it heats up, the second mode can be penalised. Use `runs=3` and repeat
  the call a few times before drawing conclusions.
- The HTTP load generator in §12.3 ran on the **same machine** as the server, so they shared CPU.

## 12. Results

All numbers below came from actually calling the endpoints on:

| | |
| :--- | :--- |
| Date | 2026-09-23 |
| CPU | Intel Core i7-1185G7 — 4 physical cores / 8 hardware threads, turbo up to 4.8 GHz (laptop) |
| RAM | 30 GB |
| OS | Linux 7.0 |
| JDK | OpenJDK 25 (Temurin), default JVM flags |
| Profile | `local,virtual-thread-poc` |
| Background load | Normal desktop use plus another JVM (load average 2–4) |

### 12.1 I/O-bound — `?workload=IO&tasks=2000&delayMs=100&poolSize=100&runs=3`

| Mode | Runs (ms) | Average | Throughput | Max concurrency | Tasks on virtual threads |
| :--- | :--- | ---: | ---: | ---: | ---: |
| Platform (pool 100) | 2034, 2014, 2011 | 2,020 ms | 990 tasks/s | 100 | 0 / 2000 |
| Virtual | 117, 109, 110 | 112 ms | 17,857 tasks/s | 2,000 | 2000 / 2000 |

**Speed-up 18.0×**, `sameWorkVerified: true` (checksum 2000 in both). Two repeat calls gave 19.2×
and 19.5×, so the effect is stable.

The theoretical platform time is `ceil(2000 / 100) × 100 ms = 2000 ms`; measured 2,020 ms, which is
1% off. The theoretical virtual time is one wait, about 100 ms; measured 112 ms.

`sampleThread` shows what ran the first task:
```
platform: Thread[#272,pool-4-thread-1,5,VirtualThreads]
virtual:  VirtualThread[#4376]/runnable@ForkJoinPool-1-worker-5
```
(The platform thread's *thread group* is called `VirtualThreads` merely because the request thread
that created the pool was virtual. It is still an ordinary platform thread: no `VirtualThread[`
prefix, and `tasksOnVirtualThreads` is 0.)

### 12.2 CPU-bound — `?workload=CPU&runs=3` (16 tasks, primes to 2,000,000, 8 logical CPUs)

The call was repeated four times, because one call is not enough to conclude anything.

| Call | Platform runs (ms) | Avg | Virtual runs (ms) | Avg | Speed-up |
| :--- | :--- | ---: | :--- | ---: | ---: |
| 1 | 1070, 1095, 1454 | 1,206 | 1697, 1619, 1906 | 1,741 | 0.69× |
| 2 | 1090, 1122, 1083 | 1,098 | 1073, 1075, 1575 | 1,241 | 0.88× |
| 3 | 1748, 1663, 1822 | 1,744 | 1643, 1590, 1896 | 1,710 | 1.02× |
| 4 | 1620, 1592, 1784 | 1,665 | 1598, 1677, 1749 | 1,675 | 0.99× |

Platform runs ranged 1,070–1,822 ms and virtual runs 1,073–1,906 ms: the ranges overlap almost
completely, and no mode wins consistently. Both modes reported **max concurrency 8**, the core
count, even though 16 virtual threads were created. Checksum 2,382,928 (= 16 × 148,933) in every
call.

### 12.3 Spring Boot — Tomcat on virtual vs platform threads

Same app, started once with the `virtual-thread-poc` profile and once with the same profile plus
`--spring.threads.virtual.enabled=false`. Load: **400 requests to `/v1/virtual-threads/io?delayMs=1000`
released at the same instant** (JDK `HttpClient`, after 50 warm-up requests), three times.

| Request threads | Run | Wall time | p50 | p95 | max |
| :--- | :---: | ---: | ---: | ---: | ---: |
| Virtual (`tomcat-handler-N`) | 1 | 1,196 ms | 1,082 ms | 1,122 ms | 1,146 ms |
| | 2 | 1,129 ms | 1,059 ms | 1,113 ms | 1,119 ms |
| | 3 | 1,078 ms | 1,029 ms | 1,046 ms | 1,055 ms |
| Platform (`http-nio-8083-exec-N`, max 200) | 1 | 2,236 ms | 1,979 ms | 2,178 ms | 2,218 ms |
| | 2 | 2,124 ms | 2,005 ms | 2,093 ms | 2,101 ms |
| | 3 | 2,070 ms | 2,000 ms | 2,050 ms | 2,062 ms |

Endpoint checks in both modes: `/info` reported `virtual=true` / `tomcat-handler-1` with the PoC
profile and `virtual=false` / `http-nio-8083-exec-2` with it disabled. Invalid parameters returned
400, an over-long run returned 400 `INVALID_REQUEST`, and `/v1/users/{uuid}` still returned **401**.
Without the PoC profile, `/v1/virtual-threads/info` returned **404**.

### 12.4 Recorded earlier: the platform pool as large as the task count

Measured with a standalone harness that is no longer part of the PoC, on the same machine
(10,000 tasks, 100 ms wait, 1 warm-up + 3 measured runs):

| Tasks | Mode | Average [min–max] | Max concurrency |
| ---: | :--- | :--- | ---: |
| 1,000 | Platform, 1,000 threads | 161 ms [157–164] | 1,000 |
| 1,000 | Virtual | 103 ms [102–103] | 1,000 |
| 10,000 | Platform, 10,000 threads | 2,519 ms [2,395–2,692] | **2,736** |
| 10,000 | Virtual | 122 ms [109–133] | 10,000 |

This is kept because it answers the obvious objection: "why not just make the pool bigger?"

## 13. Observations and conclusions

1. **I/O-bound: the platform run is capped by pool size, the virtual run by the wait itself.** The
   platform time matched `ceil(tasks / poolSize) × delay` to within 1%, and throughput sat at about
   990 tasks/s. Virtual threads finished in roughly one wait, with peak concurrency equal to the
   task count: 18–19.5× faster in three calls.
2. **The gain is affordable concurrency, not faster threads.** With a pool as large as the task
   count, 1,000 platform threads came close (161 ms vs 103 ms). At 10,000 the platform run took
   2.5 s and never had more than 2,736 tasks in flight. The most likely cause is the cost of
   creating real OS threads: early tasks finished before the last threads had even been created.
   This is inferred from the concurrency numbers, not measured directly (§12.4).
3. **CPU-bound: no improvement.** Across four calls the speed-up was 0.69×, 0.88×, 1.02× and 0.99×,
   with heavily overlapping run ranges. Virtual threads never exceeded 8 concurrent tasks, the
   carrier count, so they cannot add CPU capacity.
4. **Spring Boot: one property changes the request-thread model.** With 200 Tomcat platform threads,
   400 simultaneous one-second requests were served in two waves (p50 ≈ 2.0 s). With virtual threads
   all 400 were served in one wave (p50 ≈ 1.03–1.08 s). No controller or service code changed.

**Unexpected or noteworthy results**
- **The CPU numbers are noisy**, and the first call looked like a 31% loss for virtual threads
  (0.69×) before repeats showed it was noise. This is a laptop with turbo/thermal scaling, 4
  physical cores exposed as 8 hardware threads, and other load. Never conclude from one call.
- **CPU parallel speed-up is lower than "8 cores" suggests.** 16 × ~285 ms would be ~570 ms on 8
  real cores; the observed ~1.2–1.7 s implies an effective ~2.7× speed-up, consistent with 4
  physical cores plus hyper-threading. Both modes are limited in exactly the same way.
- **A platform thread created by a virtual thread reports the thread group `VirtualThreads`**
  (§12.1). It is still a platform thread; only the group name is inherited.

### Limitations of this PoC and possible improvements

- `Thread.sleep()` only simulates waiting. A follow-up could call a real endpoint or database, which
  would show the connection-pool bound described in §8.
- No JMH, and the measurements run inside the web application. Results are indicative, not
  publishable microbenchmarks.
- Memory footprint was not measured. Adding RSS/heap sampling or JFR would show the other big
  advantage of virtual threads.
- The HTTP load test used a throwaway generator on the same machine. A dedicated tool (`wrk`, `k6`,
  Gatling) on a separate host would give more robust latency distributions.
- Pinning was not demonstrated. `-Djdk.tracePinnedThreads` was removed in JDK 24+; JFR's
  `jdk.VirtualThreadPinned` event could be used instead.
