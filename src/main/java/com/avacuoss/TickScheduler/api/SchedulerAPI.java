package com.avacuoss.TickScheduler.api;

import com.avacuoss.TickScheduler.Ticks.ConditionScheduler;
import com.avacuoss.TickScheduler.Ticks.TickScheduler;
import com.avacuoss.TickScheduler.Types.Types;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SchedulerAPI {

    //basic
    public static Types.Builder builder() {
        return new Types.Builder();
    }

    public static Types.Handle after(long ticks, Types.TaskFn fn) {
        return builder().delay(ticks).submit(fn);
    }

    public static Types.Handle next(Types.TaskFn fn) {
        return after(1, fn);
    }

    public static Types.Handle repeat(long delay, long interval, int maxRuns, Types.TaskFn fn) {
        return builder().delay(delay).repeat(interval).maxRuns(maxRuns).submit(fn);
    }

    public static Types.Builder forLevel(ServerLevel lvl) {
        return builder().level(lvl);
    }

    public static Types.Builder forPlayer(ServerPlayer p) {
        return builder().player(p);
    }

    public static Types.Builder at(ServerLevel lvl, BlockPos pos) {
        return builder().level(lvl).pos(pos);
    }

    public static Types.TaskGroup group(String id) {
        return new Types.TaskGroup(id);
    }

    //threading
    public static void runAsync(Runnable r) {
        TickScheduler.get().runAsync(r);
    }

    public static <T> void supplyAsync(Supplier<T> supplier, Consumer<T> onServer) {
        supplyAsync(supplier, onServer, null);
    }

    public static <T> void supplyAsync(Supplier<T> supplier, Consumer<T> onServer, Consumer<Throwable> onError) {
        TickScheduler.get().runAsync(() -> {
            try {
                T result = supplier.get();
                TickScheduler.get().runOnServerThread(() -> onServer.accept(result));
            } catch (Throwable t) {
                if (onError != null) {
                    TickScheduler.get().runOnServerThread(() -> onError.accept(t));
                } else {
                    t.printStackTrace();
                }
            }
        });
    }

    //debug and profiler
    public static void setDebug(boolean enabled) {
        TickScheduler.get().setDebug(enabled);
    }

    public static void enableProfiler(boolean enabled) {
        TickScheduler.get().setProfiling(enabled);
    }

    public static Types.ProfilerStats getProfilerStats() {
        return TickScheduler.get().getProfilerStats();
    }

    //persistence
    public static List<Types.TaskSnapshot> getPersistentSnapshot() {
        return TickScheduler.get().snapshotPersistentTasks();
    }

    //conditions
    public static ConditionScheduler.Builder when(BooleanSupplier condition) {
        return ConditionScheduler.when(condition);
    }
    //repeat task while condition is true
    public static Types.Handle repeatWhile(java.util.function.BooleanSupplier cond, long every, Types.TaskFn fn) {
        Types.TaskGroup g = group("repeatwhile_" + System.nanoTime());

        Types.Handle h = builder()
                .repeat(every)
                .group(g)
                .submit(ctx -> {
                    if (!cond.getAsBoolean()) {
                        g.cancelAll();
                        return;
                    }
                    fn.run(ctx);
                });

        return h;
    }
    //runs task every second (20 ticks)
    public static Types.Handle everySecond(Types.TaskFn fn) {
        return builder().repeat(20).submit(fn);
    }

    //runs after N seconds
    public static Types.Handle afterSeconds(int seconds, Types.TaskFn fn) {
        return after(seconds * 20L, fn);
    }

}
