package com.avacuoss.TickScheduler;

import net.minecraft.server.level.ServerPlayer;
import java.nio.file.Path;

public class TickSchedulerTest {

    private static boolean started = false;

    public static void run() {
        if (started) return;
        started = true;

        System.out.println("========= TickScheduler TESTS START =========");

        testDelay();
        testRepeat();
        testCancel();
        testPriority();
        testAsync();
        testProfiler();
        testCondition();
        testPersistentSnapshot();

        System.out.println("========= TickScheduler TESTS RUNNING =======");
    }

    private static void testDelay() {
        SchedulerAPI.after(40, ctx ->
                System.out.println("[TEST] DELAY PASS (tick=" + ctx.tick + ")"));
    }

    private static void testRepeat() {
        SchedulerAPI.repeat(20, 20, 3, ctx ->
                System.out.println("[TEST] REPEAT PASS (tick=" + ctx.tick + ")"));
    }

    private static void testCancel() {
        Types.Handle h = SchedulerAPI.after(200, ctx ->
                System.out.println("[TEST] CANCEL FAIL — should not run"));

        SchedulerAPI.after(5, ctx ->
                System.out.println("[TEST] CANCEL PASS=" + h.cancel()));
    }

    private static void testPriority() {
        SchedulerAPI.builder().delay(40).priority(Types.Priority.LOW)
                .submit(ctx -> System.out.println("[TEST] PRIORITY LOW"));

        SchedulerAPI.builder().delay(40).priority(Types.Priority.HIGH)
                .submit(ctx -> System.out.println("[TEST] PRIORITY HIGH"));
    }

    // ASYNC TEST
    private static void testAsync() {
        SchedulerAPI.runAsync(() ->
                System.out.println("[TEST] ASYNC PASS (thread=" + Thread.currentThread().getName() + ")"));
    }

    // PROFILER TEST
    private static void testProfiler() {
        SchedulerAPI.enableProfiler(true);

        SchedulerAPI.repeat(1, 1, 10, ctx -> {
            double dummy = Math.sqrt(Math.random() * 1000);
        });

        SchedulerAPI.after(15, ctx -> {
            SchedulerAPI.enableProfiler(false);
            Types.ProfilerStats s = SchedulerAPI.getProfilerStats();
            System.out.println("[TEST] PROFILER stats: tasks=" + s.tasksExecuted +
                    " avg=" + s.getAvgMillisPerTask() + "ms max=" + s.getMaxMillis() + "ms");
        });
    }

    // CONDITION SCHEDULER
    private static void testCondition() {
        SchedulerAPI.when(() -> TickScheduler.getTickStatic() > 120)
                .checkEvery(10)
                .onSuccess(ctx ->
                        System.out.println("[TEST] CONDITION PASS (tick=" + ctx.tick + ")"))
                .timeout(200)
                .onTimeout(ctx ->
                        System.out.println("[TEST] CONDITION FAIL — timeout"))
                .start();
    }

    // PERSISTENT SNAPSHOT + TYPE RESTORE
    private static void testPersistentSnapshot() {

        PersistentStorage.TaskRegistry.register("hello", ctx ->
                System.out.println("[TEST] RESTORE TYPE PASS (tick=" + ctx.tick + ")"));

        SchedulerAPI.builder()
                .delay(50)
                .repeat(0)
                .persistent()
                .type("hello")
                .submit(ctx ->
                        System.out.println("[TEST] ORIGINAL TYPE PASS (tick=" + ctx.tick + ")"));


        Path file = Path.of("scheduler_test_snapshot.json");
        SchedulerAPI.after(5, ctx -> {
            try {
                PersistentStorage.save(file);
                System.out.println("[TEST] SNAPSHOT SAVED");
            } catch (Exception e) { e.printStackTrace(); }
        });

        SchedulerAPI.after(10, ctx -> {
            try {
                System.out.println("[TEST] SNAPSHOT LOADING...");
                PersistentStorage.load(file);
            } catch (Exception e) { e.printStackTrace(); }
        });
    }
}
