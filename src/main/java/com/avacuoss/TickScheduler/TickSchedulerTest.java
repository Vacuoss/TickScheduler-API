package com.avacuoss.TickScheduler;

import com.avacuoss.TickScheduler.api.SchedulerAPI;
import com.avacuoss.TickScheduler.Ticks.TickScheduler;
import com.avacuoss.TickScheduler.Types.Types;
import com.avacuoss.TickScheduler.Storage.PersistentTaskRegistry;

public class TickSchedulerTest {

    private static boolean started = false;

    public static void run() {
        if (started) return;
        started = true;

        System.out.println("TickScheduler TESTS START");

        testDelay();
        testRepeat();
        testCancel();
        testPriority();
        testAsync();
        testProfiler();
        testCondition();
        testPersistent();

        System.out.println("TickScheduler TESTS RUNNING");
    }

    //delay test
    private static void testDelay() {
        long start = TickScheduler.getTickStatic();
        SchedulerAPI.after(40, ctx ->
                System.out.println("[TEST] DELAY " + (ctx.tick - start == 40)));
    }

    //repeat test
    private static void testRepeat() {
        SchedulerAPI.repeat(20, 20, 3, ctx ->
                System.out.println("[TEST] REPEAT tick=" + ctx.tick));
    }

    //cancel test
    private static void testCancel() {
        Types.Handle h = SchedulerAPI.after(200, ctx ->
                System.err.println("[TEST] CANCEL FAILED")
        );

        SchedulerAPI.after(5, ctx ->
                System.out.println("[TEST] CANCEL " + h.cancel()));
    }

    //priority test
    private static void testPriority() {
        SchedulerAPI.builder().delay(40).priority(Types.Priority.LOW)
                .submit(ctx -> System.out.println("[TEST] PRIORITY LOW"));

        SchedulerAPI.builder().delay(40).priority(Types.Priority.HIGH)
                .submit(ctx -> System.out.println("[TEST] PRIORITY HIGH"));
    }

    //async test
    private static void testAsync() {
        SchedulerAPI.runAsync(() ->
                System.out.println("[TEST] ASYNC thread=" + Thread.currentThread().getName()));
    }

    //profiler test
    private static void testProfiler() {
        SchedulerAPI.enableProfiler(true);

        SchedulerAPI.repeat(1, 1, 10, ctx -> Math.sqrt(Math.random() * 1000));

        SchedulerAPI.after(15, ctx -> {
            SchedulerAPI.enableProfiler(false);
            Types.ProfilerStats s = SchedulerAPI.getProfilerStats();
            System.out.println("[TEST] PROFILER tasks=" + s.tasksExecuted +
                    " avg=" + s.getAvgMillisPerTask() +
                    " max=" + s.getMaxMillis());
        });
    }

    //condition test
    private static void testCondition() {
        SchedulerAPI.when(() -> TickScheduler.getTickStatic() > 120)
                .checkEvery(10)
                .onSuccess(ctx ->
                        System.out.println("[TEST] CONDITION PASS tick=" + ctx.tick))
                .timeout(200)
                .onTimeout(ctx ->
                        System.err.println("[TEST] CONDITION TIMEOUT"))
                .start();
    }

    //persistent saveddata test
    private static void testPersistent() {
        PersistentTaskRegistry.register("hello", (ctx, data) ->
                System.out.println("[TEST] RESTORED tick=" + ctx.tick));

        SchedulerAPI.builder()
                .delay(50)
                .persistent()
                .type("hello")
                .submit(ctx ->
                        System.out.println("[TEST] ORIGINAL tick=" + ctx.tick));
    }
}
