package com.avacuoss.TickScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.BooleanSupplier;

public class SchedulerAPI {

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

    public static void runAsync(Runnable r) {
        TickScheduler.get().runAsync(r);
    }

    // Debug & Profiler
    public static void setDebug(boolean enabled) {
        TickScheduler.get().setDebug(enabled);
    }

    public static void enableProfiler(boolean enabled) {
        TickScheduler.get().setProfiling(enabled);
    }

    public static Types.ProfilerStats getProfilerStats() {
        return TickScheduler.get().getProfilerStats();
    }

    // Persistent Snapshot API
    public static List<Types.TaskSnapshot> getPersistentSnapshot() {
        return TickScheduler.get().snapshotPersistentTasks();
    }

    // Condition Scheduler entry point
    public static ConditionScheduler.Builder when(BooleanSupplier condition) {
        return ConditionScheduler.when(condition);
    }
}
