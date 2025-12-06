package com.avacuoss.TickScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

public class Types {

    public enum Priority { LOW, NORMAL, HIGH }

    @FunctionalInterface
    public interface TaskFn { void run(Context ctx); }

    public static class Context {
        public final MinecraftServer server;
        public final long tick;
        public final ServerLevel level;
        public final ServerPlayer player;
        public final BlockPos pos;

        public Context(MinecraftServer s, long t, ServerLevel lvl, ServerPlayer pl, BlockPos pos) {
            server = s; tick = t; level = lvl; player = pl; this.pos = pos;
        }
    }

    public static class Task {
        final long id;
        long next;
        final long repeat;
        final int maxRuns;
        final Priority priority;
        final TaskFn fn;
        final ServerLevel level;
        final ServerPlayer player;
        final BlockPos pos;
        final String groupId;
        final boolean persistent;
        final String typeId;

        boolean cancelled = false;
        int runs = 0;

        public Task(long id, long next, long repeat, int maxRuns, Priority priority,
                    TaskFn fn, ServerLevel level, ServerPlayer player, BlockPos pos,
                    String groupId, boolean persistent, String typeId) {
            this.id = id;
            this.next = next;
            this.repeat = repeat;
            this.maxRuns = maxRuns;
            this.priority = priority;
            this.fn = fn;
            this.level = level;
            this.player = player;
            this.pos = pos;
            this.groupId = groupId;
            this.persistent = persistent;
            this.typeId = typeId;
        }
    }

    public static class Handle {
        public final long id;
        public Handle(long id) { this.id = id; }
        public boolean cancel() { return TickScheduler.get().cancel(id); }
    }

    public static class Builder {
        long delay = 0;
        long repeat = 0;
        int maxRuns = 0;
        Priority priority = Priority.NORMAL;
        ServerLevel level;
        ServerPlayer player;
        BlockPos pos;
        TaskGroup group;
        boolean persistent = false;
        String typeId = null;

        public Builder delay(long d) { delay = d; return this; }
        public Builder repeat(long r) { repeat = r; return this; }
        public Builder maxRuns(int m) { maxRuns = m; return this; }
        public Builder priority(Priority p) { priority = p; return this; }

        public Builder level(ServerLevel lvl) { level = lvl; return this; }
        public Builder player(ServerPlayer p) { player = p; return this; }
        public Builder pos(BlockPos p) { pos = p; return this; }

        public Builder group(TaskGroup g) { group = g; return this; }
        public Builder persistent() { persistent = true; return this; }
        public Builder type(String type) { typeId = type; return this; }

        public Handle submit(TaskFn fn) {
            return TickScheduler.get().schedule(this, fn);
        }
    }

    public static class TaskGroup {
        public final String id;
        private final Set<Long> ids = new HashSet<>();

        public TaskGroup(String id) {
            this.id = id;
        }

        void register(long id) {
            ids.add(id);
        }

        public void cancelAll() {
            TickScheduler.get().cancelGroup(id);
        }
    }

    public static class ProfilerStats {
        public final long tickFrom;
        public final long tickTo;
        public final long tasksExecuted;
        public final long totalNanos;
        public final long maxTaskNanos;

        public ProfilerStats(long tickFrom, long tickTo, long tasksExecuted, long totalNanos, long maxTaskNanos) {
            this.tickFrom = tickFrom;
            this.tickTo = tickTo;
            this.tasksExecuted = tasksExecuted;
            this.totalNanos = totalNanos;
            this.maxTaskNanos = maxTaskNanos;
        }

        public double getAvgMillisPerTask() {
            if (tasksExecuted == 0) return 0.0;
            return (totalNanos / 1_000_000.0) / tasksExecuted;
        }

        public double getMaxMillis() {
            return maxTaskNanos / 1_000_000.0;
        }
    }

    public static class TaskSnapshot {
        public final long delayRemaining;
        public final long repeat;
        public final int maxRuns;
        public final Priority priority;
        public final String groupId;
        public final boolean persistent;
        public final String typeId;

        public TaskSnapshot(long delayRemaining, long repeat, int maxRuns,
                            Priority priority, String groupId, boolean persistent, String typeId) {
            this.delayRemaining = delayRemaining;
            this.repeat = repeat;
            this.maxRuns = maxRuns;
            this.priority = priority;
            this.groupId = groupId;
            this.persistent = persistent;
            this.typeId = typeId;
        }
    }
}
