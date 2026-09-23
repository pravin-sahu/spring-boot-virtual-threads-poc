#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Guided virtual threads demo (about 5 minutes).
#
#   ./demo.sh          # all steps, pausing between each
#   ./demo.sh 2        # only step 2
#   ./demo.sh 2 3      # steps 2 and 3
#
# Step 1 is a plain Java program. Steps 2-4 call the running application:
#
#   mvn spring-boot:run -Dspring-boot.run.profiles=local,virtual-thread-poc
#
# Override the address with BASE_URL=http://localhost:8082 ./demo.sh
# ---------------------------------------------------------------------------
set -u
cd "$(dirname "$0")"

BASICS=com.mb.modules.virtualthread.basics.VirtualThreadBasicsDemo
BASE_URL=${BASE_URL:-http://localhost:8081}
VT=$BASE_URL/v1/virtual-threads

if [ -t 1 ]; then bold() { printf '\033[1m%s\033[0m\n' "$*"; }; else bold() { printf '%s\n' "$*"; }; fi
rule() { printf '%s\n' "------------------------------------------------------------"; }
pause() { [ -t 0 ] && { printf '\n(press Enter to continue)'; read -r _; } || true; printf '\n'; }
step_header() { printf '\n'; rule; bold "STEP $1 — $2"; printf '%s\n' "$3"; rule; printf '\n'; }

# Pretty-print JSON if python is available, otherwise print it raw.
show() { if command -v python3 > /dev/null; then python3 -m json.tool; else cat; fi; }

# --- prerequisites ---------------------------------------------------------
# The project needs Java 25. JAVA_HOME often points at an older default (e.g. sdkman's
# "current"), so check the version actually on PATH and switch to a JDK 25 if one is installed.
java_major() { java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/'; }
if [ "$(java_major)" -lt 25 ] 2>/dev/null; then
  for candidate in "$HOME"/.sdkman/candidates/java/25* /usr/lib/jvm/*-25-*; do
    if [ -x "$candidate/bin/java" ]; then
      export JAVA_HOME="$candidate"
      export PATH="$JAVA_HOME/bin:$PATH"
      break
    fi
  done
fi
if [ "$(java_major)" -lt 25 ] 2>/dev/null; then
  echo "This project needs Java 25, but 'java' is $(java_major). Set JAVA_HOME to a JDK 25."
  exit 1
fi

steps=("$@")
[ ${#steps[@]} -eq 0 ] && steps=(1 2 3 4)
wants() { for s in "${steps[@]}"; do [ "$s" = "$1" ] && return 0; done; return 1; }

require_app() {
  curl -sf -m 3 "$BASE_URL/actuator/health" > /dev/null 2>&1 && return 0
  echo "The application is not answering on $BASE_URL. Start it in another terminal:"
  echo
  echo "  mvn spring-boot:run -Dspring-boot.run.profiles=local,virtual-thread-poc"
  echo
  return 1
}

# --- step 1: what a virtual thread is --------------------------------------
if wants 1; then
  step_header "1/4" "A virtual thread is just a Thread" \
    "Same API as always. Watch isVirtual, and the carrier thread after the '@'."
  [ -d target/classes ] || mvn -q compile || exit 1
  java -cp target/classes $BASICS
  pause
fi

# --- step 2: who serves the request ----------------------------------------
if wants 2 && require_app; then
  step_header "2/4" "Which thread serves an HTTP request?" \
    "tomcat-handler-N = one virtual thread per request. http-nio-...-exec-N = pooled platform thread."
  curl -s "$VT/info" | show
  pause
fi

# --- step 3: the comparison, both workloads ---------------------------------
if wants 3 && require_app; then
  step_header "3/4" "The same work on platform threads and on virtual threads" \
    "2000 tasks that only WAIT 100 ms each. A pool of 100 vs one virtual thread per task.
Thread.sleep SIMULATES waiting for a database or an HTTP call. It is not real I/O."
  curl -s "$VT/compare?workload=IO&tasks=2000&delayMs=100&poolSize=100&runs=3" | show
  bold "Read: same checksum = same work. Platform concurrency stops at the pool size;"
  bold "virtual concurrency equals the task count."
  pause

  step_header "3b/4" "Now the same comparison with CPU work" \
    "Counting primes instead of waiting. Nobody blocks, so nobody gives up a carrier."
  curl -s "$VT/compare?workload=CPU&runs=3" | show
  bold "Read: speedup near 1.0, and virtual concurrency never exceeds the core count."
  bold "Virtual threads do not add CPU capacity."
  pause
fi

# --- step 4: Tomcat under load ---------------------------------------------
if wants 4 && require_app; then
  step_header "4/4" "400 concurrent requests through Tomcat" \
    "Each request waits 1 second. Tomcat's default platform pool is 200 threads."
  echo "> 400 parallel requests to /io?delayMs=1000"
  time (seq 1 400 | xargs -P 400 -I{} curl -s -o /dev/null "$VT/io?delayMs=1000")
  echo
  bold "Virtual threads: about 1 second (one wave). Platform threads: about 2 seconds"
  bold "(two waves of 200). Restart the app with"
  bold "  --spring.threads.virtual.enabled=false"
  bold "and run ./demo.sh 4 again to see the difference."
fi

printf '\n'; rule; bold "Takeaways"
cat <<'EOF'
  1. I/O-bound: concurrency is capped by pool size, not by the work itself.
     Virtual threads let every task wait at the same time.
  2. The win is affordable concurrency, not faster threads.
  3. CPU-bound: no gain. Cores are the limit.
  4. Spring Boot: one property, same blocking code.
  5. Caveat: other limits remain. A 10-connection pool still allows 10 queries.
EOF
rule
