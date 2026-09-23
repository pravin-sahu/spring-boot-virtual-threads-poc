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

### 10.2 Benchmarks (plain Java, no Spring, no DB)

```bash
mvn -q compile
# I/O-bound: 1,000 / 5,000 / 10,000 tasks, 100 ms simulated wait, platform pool of 100 (≈ 65 s)
java -cp target/classes com.mb.modules.virtualthread.benchmark.VirtualThreadBenchmarkApp \
     mode=io tasks=1000,5000,10000 ioWaitMs=100 poolSize=100

# CPU-bound: 2 × cores tasks, count primes up to 2,000,000, platform pool = cores (≈ 15 s)
java -cp target/classes com.mb.modules.virtualthread.benchmark.VirtualThreadBenchmarkApp mode=cpu
```

| Option | Default | Meaning |
| :--- | :--- | :--- |
| `mode` | `all` | `io`, `cpu` or `all` |
| `tasks` | `1000,5000,10000` | I/O task counts (comma-separated) |
| `ioWaitMs` | `100` | Simulated wait per I/O task |
| `poolSize` | `100` | Platform pool size for I/O |
| `cpuTasks` | `2 × cores` | CPU task count |
| `primeLimit` | `2000000` | CPU work per task |
| `cpuPoolSize` | `cores` | Platform pool size for CPU |
| `warmups` / `runs` | `1` / `3` | Discarded warm-up runs / measured runs per mode |

### 10.3 Spring Boot application

The app itself needs PostgreSQL (the boilerplate's JPA/Liquibase). The benchmarks above do not.

**One-time setup:** create `src/main/resources/application-local.yml`. It is gitignored, so each
developer keeps their own credentials out of git.

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
java -jar target/spring-rest-0.0.1.jar --spring.profiles.active=local,virtual-thread-poc
# or: mvn spring-boot:run -Dspring-boot.run.profiles=local,virtual-thread-poc

# Virtual threads OFF, for comparison (PoC endpoints still available)
java -jar target/spring-rest-0.0.1.jar --spring.profiles.active=local,virtual-thread-poc \
     --spring.threads.virtual.enabled=false

# Without the PoC profile the app runs exactly as before, and /v1/virtual-threads/** returns 404
java -jar target/spring-rest-0.0.1.jar
```

The measurements in §12.3 were taken with an equivalent setup: a throwaway PostgreSQL container, with
the datasource and CORS passed as environment variables / arguments and `--server.port=8081`.

| Endpoint | Purpose |
| :--- | :--- |
| `GET /v1/virtual-threads/info` | Is the request thread virtual? Name, id, `toString()` (shows the carrier) |
| `GET /v1/virtual-threads/io?delayMs=200` | Simulated I/O wait (0–10,000 ms) on the request thread; returns elapsed time and thread info |
| `GET /v1/virtual-threads/concurrent?mode=VIRTUAL&tasks=1000&delayMs=100&poolSize=100` | Runs a batch of simulated I/O waits inside the app on `PLATFORM` or `VIRTUAL` threads. `tasks` ≤ 10,000, `delayMs` ≤ 5,000. Platform runs whose theoretical duration exceeds 60 s are rejected with 400 |

### 10.4 Tests

```bash
mvn test     # includes unit + @WebMvcTest + Testcontainers integration tests for the PoC
mvn verify   # + Spotless, SpotBugs, PMD, JaCoCo
```

Tests only check correctness (task counts, thread type, concurrency bounds, checksums), never timing,
so they are not flaky under the parallel Surefire configuration.

## 11. Benchmark methodology

```
VirtualThreadBenchmarkApp (main, key=value args)
          │
          ▼
   BenchmarkRunner.runRepeated(mode, tasks, pool, task, warmups, runs)
          │   for each run: build N instrumented copies of the SAME Callable
          │   start timer ─▶ create executor ─▶ invokeAll ─▶ sum results ─▶ close ─▶ stop timer
          ▼
   ┌───────────────────────────────┐     ┌──────────────────────────────────────┐
   │ PLATFORM                      │     │ VIRTUAL                              │
   │ Executors.newFixedThreadPool  │     │ Executors.newVirtualThreadPerTask... │
   └───────────────────────────────┘     └──────────────────────────────────────┘
          │  each task: ConcurrencyTracker.enter() → isVirtual()? → workload → exit()
          ▼
   BenchmarkRuns (avg / min / max / throughput / max concurrency) ─▶ BenchmarkReportFormatter
```

- **Same workload in both modes.** The same `Callable` is used. Each task returns a value
  (`1` for I/O, the prime count for CPU), and the summed **checksum must match** between modes. The
  report prints "Checksums match: yes".
- **Warm-up + repetitions.** 1 discarded warm-up run, then 3 measured runs per mode. Average, min and
  max are reported, never a single run.
- **What is timed.** Wall-clock (`System.nanoTime`) from executor creation until every task has
  finished and the executor is closed. Thread creation cost is included for both modes.
- **Instrumentation.** `ConcurrencyTracker` records the maximum number of tasks in flight. Each task
  records whether it ran on a virtual thread.
- **I/O workload.** `Thread.sleep(ioWaitMs)`. **This is a simulation of waiting, not real database or
  network I/O.**
- **CPU workload.** Deterministic trial-division prime count up to `primeLimit` (148,933 primes below
  2,000,000). About 285 ms per task when run alone on this machine. It is bounded: 16 tasks, about
  2 s per run.
- **Plain Java.** No Spring context, no database, no JMH. This is a controlled learning benchmark, not
  a rigorous microbenchmark.

**Known methodology limitations**
- Within each scenario the platform mode always runs **before** the virtual mode. On a laptop that
  heats up and throttles, the second mode can be slightly penalised. This matters for the CPU
  benchmark, where the difference between modes was within the noise. Alternating the order would
  remove this bias.
- The HTTP load generator in §12.3 ran on the **same machine** as the server, so they shared CPU.
- The "All 9 runs" CPU averages in §12.2 were calculated from the per-run values printed by the
  program. They were not printed directly.

## 12. Benchmark results

All numbers below were produced by actually running the code on:

| | |
| :--- | :--- |
| Date | 2026-09-22 |
| CPU | Intel Core i7-1185G7 — 4 physical cores / 8 hardware threads, turbo up to 4.8 GHz (laptop) |
| RAM | 30 GB |
| OS | Linux 7.0 |
| JDK | OpenJDK 25 (Temurin, 2025-09-16), default JVM flags |
| Background load | Normal desktop use plus another JVM application running (load average 2–5) |

### 12.1 I/O-bound (simulated wait 100 ms, platform pool 100, 1 warm-up + 3 measured runs)

| Tasks | Mode | Runs (ms) | Avg total time | Avg throughput | Max concurrency | Theoretical min |
| ---: | :--- | :--- | ---: | ---: | ---: | ---: |
| 1,000 | Platform (100) | 1006, 1006, 1006 | 1,006.8 ms | 993 tasks/s | 100 | 1,000 ms |
| 1,000 | Virtual | 103, 102, 102 | 102.9 ms | 9,715 tasks/s | 1,000 | ~100 ms |
| 5,000 | Platform (100) | 5010, 5013, 5008 | 5,011.0 ms | 998 tasks/s | 100 | 5,000 ms |
| 5,000 | Virtual | 103, 105, 103 | 104.1 ms | 48,037 tasks/s | 5,000 | ~100 ms |
| 10,000 | Platform (100) | 10043, 10025, 10017 | 10,028.6 ms | 997 tasks/s | 100 | 10,000 ms |
| 10,000 | Virtual | 114, 107, 106 | 109.5 ms | 91,398 tasks/s | 10,000 | ~100 ms |

Platform / virtual ratio: **9.8×** (1,000), **48.1×** (5,000), **91.6×** (10,000). Checksums matched in
every scenario.

**Control — platform pool as large as the task count** (same settings, 3 measured runs):

| Tasks | Mode | Avg total time [min–max] | Max concurrency |
| ---: | :--- | :--- | ---: |
| 1,000 | Platform (1,000 threads) | 161.0 ms [157–164] | 1,000 |
| 1,000 | Virtual | 103.1 ms [102–103] | 1,000 |
| 10,000 | Platform (10,000 threads) | 2,518.9 ms [2,395–2,692] | **2,736** |
| 10,000 | Virtual | 121.9 ms [109–133] | 10,000 |

### 12.2 CPU-bound (16 tasks, primes up to 2,000,000, 8 logical CPUs, 1 warm-up + 3 measured runs)

The benchmark was run as three separate invocations, because a single one was too noisy to draw
conclusions from.

| Invocation | Platform pool 8 — runs (ms) | Avg | Virtual — runs (ms) | Avg | Platform / virtual |
| :--- | :--- | ---: | :--- | ---: | ---: |
| 1 | 1115, 1478, 2004 | 1,532.5 | 1852, 1757, 1733 | 1,781.2 | 0.86× |
| 2 | 2021, 1817, 1795 | 1,878.2 | 1958, 1723, 1741 | 1,807.6 | 1.04× |
| 3 | 1662, 1757, 1621 | 1,680.4 | 1779, 1637, 1798 | 1,738.7 | 0.97× |
| **All 9 runs** | range 1,115–2,021 | **1,696.7** | range 1,637–1,958 | **1,775.3** | **0.96×** |

- Max observed concurrency: platform **8** (pool size), virtual **8** (number of carriers), even
  though 16 virtual threads were created.
- Control with a 16-thread platform pool: platform 1,703.0 ms [1,609–1,762] vs virtual 1,752.9 ms
  [1,624–1,903] (0.97×). The platform pool reached concurrency 16, virtual stayed at 8. More threads
  did not add CPU either.
- Checksum 2,382,928 (= 16 × 148,933) in both modes.

### 12.3 Spring Boot — Tomcat on virtual vs platform threads

Same application and endpoint, started once with the `virtual-thread-poc` profile and once with the
same profile plus `--spring.threads.virtual.enabled=false`. Load: **400 requests to
`/v1/virtual-threads/io?delayMs=1000` released at the same instant** (JDK `HttpClient`, after 50
warm-up requests), repeated 3 times.

| Request threads | Run | Wall time | p50 | p95 | max |
| :--- | :---: | ---: | ---: | ---: | ---: |
| Virtual (`tomcat-handler-N`) | 1 | 1,379 ms | 1,145 ms | 1,254 ms | 1,271 ms |
| | 2 | 1,184 ms | 1,107 ms | 1,159 ms | 1,177 ms |
| | 3 | 1,097 ms | 1,042 ms | 1,088 ms | 1,091 ms |
| Platform (`http-nio-8081-exec-N`, max 200) | 1 | 2,271 ms | 1,930 ms | 2,179 ms | 2,234 ms |
| | 2 | 2,127 ms | 2,014 ms | 2,104 ms | 2,121 ms |
| | 3 | 2,089 ms | 2,004 ms | 2,072 ms | 2,081 ms |

A simpler `curl` + `xargs -P 400` run showed the same direction, but less clearly: p95 1.01–1.12 s
(virtual) vs 1.33–1.58 s (platform). Spawning 400 `curl` processes staggers the arrivals.

Endpoint checks (both modes): `/info` reported `virtual=true` / `tomcat-handler-1` with the PoC
profile and `virtual=false` / `http-nio-8081-exec-2` with it disabled. Invalid parameters returned
400, the too-long platform batch returned 400 `INVALID_REQUEST`, and `/v1/users/{uuid}` still
returned **401**. `/concurrent` inside the app: `VIRTUAL` 5,000 × 100 ms in 162 ms vs `PLATFORM`
(pool 100) in 5,046 ms.

## 13. Observations and conclusions

1. **I/O-bound: the platform run is capped by pool size. The virtual run is capped by the wait
   itself.** Platform times matched `ceil(tasks / 100) × 100 ms` within 0.3%, and throughput
   plateaued at ~1,000 tasks/s regardless of task count. Virtual times stayed at ~100–110 ms, and
   throughput grew with the task count, because every task waited at the same time
   (max concurrency = task count).
2. **The gain comes from affordable concurrency, not faster threads.** When the platform pool was
   as large as the task count, 1,000 platform threads came close (161 ms vs 103 ms). At 10,000 the
   platform run took 2.5 s and never had more than 2,736 tasks in flight. The most likely cause is
   the cost of creating real OS threads: early tasks finished before the last threads had even
   been created. This is inferred from the concurrency numbers, not measured directly. Virtual
   threads handled 10,000 in ~120 ms.
3. **CPU-bound: no improvement.** Across 9 measured runs per mode the averages were within ~5%
   (1,697 vs 1,775 ms), and the run ranges overlapped heavily. Neither mode won consistently.
   Virtual threads never exceeded 8 concurrent tasks — the carrier count — so they cannot
   add CPU capacity.
4. **Spring Boot: one property changes the request-thread model.** With 200 Tomcat platform threads,
   400 simultaneous 1-second requests were served in two waves (p50 ≈ 2.0 s). With virtual threads
   all 400 were served in one wave (p50 ≈ 1.04–1.15 s). Controller and service code did not change.

**Unexpected or noteworthy results**
- **CPU runs were noisy** (one platform invocation ranged 1,115–2,004 ms). This is a laptop with
  turbo/thermal frequency scaling, 4 physical cores exposed as 8 hardware threads, and other load.
  That is why the CPU benchmark was repeated and no winner is claimed.
- **CPU parallel speed-up was lower than "8 cores" suggests.** 16 × ~285 ms would take ~570 ms on
  8 real cores. The observed ~1.7 s means an effective ~2.7× speed-up, consistent with 4 physical
  cores plus hyper-threading and lower all-core clocks. Both modes are limited in exactly the same way.
- **10,000 platform threads were created successfully** but slowly (see conclusion 2). On a machine
  with a lower `ulimit -u` this configuration could fail with `OutOfMemoryError: unable to create
  native thread`.

### Limitations of this PoC and possible improvements

- `Thread.sleep()` only simulates waiting. A follow-up could call a real endpoint (for example
  WireMock or a local stub) or a real database. That would show the connection-pool bound from §8.
- No JMH. Results are affected by OS scheduling, CPU frequency scaling and background load.
  Numbers are indicative, not publishable microbenchmarks.
- Memory footprint was not measured. Adding RSS / heap sampling (or JFR) would show the second big
  advantage of virtual threads: memory per thread.
- The HTTP load test used a throwaway generator. A dedicated tool (`wrk`, `k6`, Gatling) would give
  more robust latency distributions.
- Pinning was not demonstrated. `-Djdk.tracePinnedThreads` was removed in JDK 24+. JFR's
  `jdk.VirtualThreadPinned` event could be used to show native-frame pinning.
