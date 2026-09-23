package com.mb.modules.virtualthread.basics;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Minimal, runnable tour of the virtual thread API (Java 21+). No Spring or database involved.
 *
 * <pre>
 * mvn -q compile
 * java -cp target/classes com.mb.modules.virtualthread.basics.VirtualThreadBasicsDemo
 * </pre>
 *
 * @author pravin.sahu
 */
public final class VirtualThreadBasicsDemo {

  private static final int EXECUTOR_TASKS = 5;

  private VirtualThreadBasicsDemo() {}

  public static void main(String[] args) throws InterruptedException, ExecutionException {
    run(System.out);
  }

  public static void run(PrintStream out) throws InterruptedException, ExecutionException {
    out.println("=== VIRTUAL THREAD BASICS ===");

    // 1. Platform thread: a thin wrapper around an OS thread.
    Thread platform =
        Thread.ofPlatform()
            .name("demo-platform")
            .start(() -> out.println("[1] Platform thread -> " + describe(Thread.currentThread())));
    platform.join();

    // 2. Virtual thread: a JVM-managed thread that runs on top of a carrier (platform) thread.
    Thread virtual =
        Thread.ofVirtual()
            .name("demo-virtual")
            .start(() -> out.println("[2] Virtual thread  -> " + describe(Thread.currentThread())));
    virtual.join();

    // 3. Shorthand for an unnamed virtual thread.
    Thread.startVirtualThread(
            () -> out.println("[3] startVirtualThread -> " + describe(Thread.currentThread())))
        .join();

    // 4. Mount/unmount: blocking unmounts the virtual thread from its carrier. When it resumes it
    //    is mounted again, possibly on a different carrier (visible in the toString output).
    Thread.ofVirtual()
        .name("demo-mount")
        .start(
            () -> {
              out.println("[4] Before blocking -> " + Thread.currentThread());
              try {
                Thread.sleep(10);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
              out.println("[4] After blocking  -> " + Thread.currentThread());
            })
        .join();

    // 5. Executor that starts a new virtual thread for every submitted task. No pooling needed:
    //    virtual threads are cheap, so they are created per task and discarded afterwards.
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<String>> futures = new ArrayList<>();
      for (int i = 0; i < EXECUTOR_TASKS; i++) {
        int taskId = i;
        futures.add(
            executor.submit(() -> "task-" + taskId + " -> " + describe(Thread.currentThread())));
      }
      for (Future<String> future : futures) {
        out.println("[5] newVirtualThreadPerTaskExecutor " + future.get());
      }
    }

    out.println("[6] main thread -> " + describe(Thread.currentThread()));
  }

  private static String describe(Thread thread) {
    return "name='" + thread.getName() + "', isVirtual=" + thread.isVirtual() + ", " + thread;
  }
}
