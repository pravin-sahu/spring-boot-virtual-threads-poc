# Demo script — Virtual Threads in about 10 minutes

A runbook for demonstrating this PoC to a team. The deep detail lives in
[`virtual-threads-poc.md`](virtual-threads-poc.md); this file is what you actually read from while
presenting.

---

## The one sentence to open with

> "Virtual threads don't make code faster. They make it affordable to have one thread per task, so
> tasks that only *wait* stop queueing behind a thread pool."

## The analogy (use it before any code)

A coffee shop with **100 baristas** and **10,000 customers**. Each order takes 100 ms of actual work
and then one second of waiting for the machine.

- **Platform threads:** a barista stands next to the machine doing nothing while it brews. 100
  customers at a time, so 100 rounds.
- **Virtual threads:** the barista starts the machine, serves someone else, and comes back when it
  beeps. Everyone's order is brewing at once.
- **The CPU case:** if each order required a barista to *grind beans by hand* for a second, hiring
  10,000 "virtual baristas" changes nothing. You only have 8 grinders.

That last point is the one that earns trust. Say it early.

---

## Before you start

```bash
cd <project root>
export JAVA_HOME=~/.sdkman/candidates/java/25-tem
export PATH=$JAVA_HOME/bin:$PATH
mvn clean package -DskipTests      # build ahead of time; never compile on stage
lsof -ti:8081 | xargs -r kill      # free the port

# terminal 2: start the app and leave it running
mvn spring-boot:run -Dspring-boot.run.profiles=local,virtual-thread-poc
```

Two terminals: one for `./demo.sh`, one for the app. Font size up.

---

## Running it

```bash
./demo.sh          # all four steps, pausing between each
./demo.sh 3        # just the comparison step
./demo.sh 3 4      # the comparison plus the Tomcat load test
```

Each step prints its own heading and what to look for, so the script is your teleprompter.

| Step | Duration | Shows |
| :--- | :--- | :--- |
| 1 | ~1 s | The API: `isVirtual()`, carrier threads, `newVirtualThreadPerTaskExecutor()` |
| 2 | instant | `/info`: which thread serves an HTTP request |
| 3 | ~7 s | `/compare`: the same workload on both modes, first waiting, then CPU |
| 4 | ~3 s | 400 concurrent requests through Tomcat |

Steps 2–4 need the app running in the other terminal.

---

## What to say at each step

**Step 1 — "it's just a Thread"**
Point at the two lines:
```
[1] Platform thread -> isVirtual=false, Thread[#25,demo-platform,5,main]
[2] Virtual thread  -> isVirtual=true,  VirtualThread[#27,demo-virtual]/runnable@ForkJoinPool-1-worker-1
```
> "Same `Thread` class, same blocking code. The bit after the `@` is the carrier: the real OS thread
> it's borrowing right now."

**Step 2 — who serves the request**
```json
"virtual": true, "threadName": "tomcat-handler-1"
```
> "One virtual thread per request. Without the property this says `http-nio-8081-exec-2`, a thread
> borrowed from Tomcat's pool of 200."

**Step 3 — the main event**
Point at four fields in the response:
```json
"platform": { "elapsedMs": 2020, "maxObservedConcurrency": 100  }
"virtual":  { "elapsedMs":  112, "maxObservedConcurrency": 2000 }
"sameWorkVerified": true,
"speedup": 18.0
```
> "Same 2,000 tasks, same 100 ms wait, same checksum, so identical work. The pool never gets past
> 100 at a time and takes two seconds. Virtual threads run all 2,000 at once and it's over in about
> one wait."

Then get ahead of the obvious objection:
> "And no, this isn't real database I/O. It's `Thread.sleep`, which parks the thread the same way,
> but it doesn't model drivers or connection pools. The docs say so."

**Step 3b — the counter-example that earns trust**
> "Same endpoint, `workload=CPU`: prime counting instead of waiting. The speed-up lands around 1.0,
> and virtual concurrency never passes 8, my core count. Virtual threads don't create CPU capacity.
> The numbers bounce around by a few hundred milliseconds, which is why I repeat the call."

**Step 4 — the payoff for the team**
> "One property. Our controllers, services and JPA code don't change. 400 concurrent one-second
> requests: about one second on virtual threads, about two on the default 200-thread pool, because
> that's two waves."

---

## Slide outline (if you need slides)

1. **Title** — Virtual threads: one thread per task
2. **The problem** — thread-per-request + blocking I/O = pool size is your concurrency ceiling
3. **The analogy** — 100 baristas, 10,000 waiting customers
4. **What changes** — platform vs virtual table (§2 of the main doc)
5. **Mount / unmount** — the diagram from §4
6. **Demo** — steps 1 and 3 live
7. **The numbers** — the table from §12.1
8. **The honest slide** — CPU-bound: no gain; 10,000 platform threads also nearly work
9. **Spring Boot** — the one property, plus the 400-request result
10. **Caveats** — connection pools, pinning, no time-slicing, don't pool virtual threads
11. **When to use it** — the lists from §7
12. **Questions**

---

## Q&A cheat sheet

| Question | Answer |
| :--- | :--- |
| "Is `Thread.sleep` real I/O?" | No. It parks the virtual thread and frees the carrier like a socket read would, but it doesn't model drivers, pools or network variance. Real I/O won't scale this cleanly. |
| "So we should switch everything?" | Only where we're thread-bound on waiting. And it removes the thread limit, not the others. |
| "What about the database pool?" | Unchanged. 10 connections still means 10 concurrent queries; virtual threads just queue there instead. That's the main caveat. |
| "Pinning?" | `synchronized` pinned the carrier before Java 24; JEP 491 fixed that and we're on 25. Native/JNI frames still pin. |
| "Is it faster than reactive?" | Different trade-off. Similar scalability for I/O, with ordinary blocking code and normal stack traces. |
| "Does this touch the boilerplate?" | No. The property lives in the `virtual-thread-poc` profile, and the endpoints are `@Profile`-gated, so they 404 elsewhere. `/v1/users` still returns 401. |
| "How do I trust the benchmark?" | The same `Callable` object goes to both modes and the checksums must match (`sameWorkVerified`). `runs=3` shows every run, not just an average. Known biases are listed in §11: it measures inside the app, has no warm-up, and always runs platform first. |
| "Memory use?" | Not measured here. That's a stated limitation and a good follow-up. |
| "Tests?" | `mvn verify`: 202 tests, SpotBugs, PMD and coverage gates all pass. |

---

## If it breaks live

| Symptom | Fix |
| :--- | :--- |
| `Port 8081 already in use` | `lsof -ti:8081 \| xargs -r kill`, or `--server.port=8082` |
| `Failed to configure a DataSource` | Profile missing: add `-Dspring-boot.run.profiles=local,virtual-thread-poc` |
| `UnsupportedClassVersionError` or the script's Java check fails | Wrong JDK: re-export `JAVA_HOME` to JDK 25 |
| `/v1/virtual-threads/...` returns 404 | App started without the `virtual-thread-poc` profile |
| Numbers look odd (laptop busy) | Repeat the call; say the CPU case is within noise, and fall back to the recorded results in §12 of the main doc |

---

## Close on this

> "Three things. For work that waits, concurrency stops being capped by pool size. For CPU work,
> nothing changes, because cores are the limit. And in Spring Boot it's one property with the same
> blocking code. The catch is that every other limit, especially the connection pool, is still there."
