package com.avacuoss.TickScheduler.Types;

import com.avacuoss.TickScheduler.Ticks.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
        private final CompoundTag data;

        public Context(MinecraftServer s, long t, ServerLevel lvl, ServerPlayer pl, BlockPos pos, CompoundTag data) {
            this.server = s;
            this.tick = t;
            this.level = lvl;
            this.player = pl;
            this.pos = pos;
            this.data = data != null ? data : new CompoundTag();
        }

        //never returns null
        public CompoundTag data() {
            return data;
        }

        //throws if task has no player
        public ServerPlayer requirePlayer() {
            if (player == null)
                throw new IllegalStateException("task has no player context");
            return player;
        }
    }

    public static class Task {
        public final long id;
        public long next;

        public final long repeat;
        public final int maxRuns;
        public final Priority priority;

        public final TaskFn fn;

        public final ServerLevel level;
        public final ServerPlayer player;
        public final BlockPos pos;

        public final String groupId;
        public final boolean persistent;
        public final String typeId;
        public final CompoundTag data;

        public boolean cancelled = false;
        public int runs = 0;

        public Task(long id, long next, long repeat, int maxRuns, Priority priority,
                    TaskFn fn, ServerLevel level, ServerPlayer player, BlockPos pos,
                    String groupId, boolean persistent, String typeId, CompoundTag data) {
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
            this.data = data != null ? data : new CompoundTag();
        }
    }

    public static class Handle {
        public final long id;
        public Handle(long id) { this.id = id; }
        public boolean cancel() { return TickScheduler.get().cancel(id); }
    }

    public static class Builder {
        public long delay = 0;
        public long repeat = 0;
        public int maxRuns = 0;
        public Priority priority = Priority.NORMAL;

        public ServerLevel level;
        public ServerPlayer player;
        public BlockPos pos;

        public TaskGroup group;
        public boolean persistent = false;
        public String typeId = null;
        public CompoundTag data = null;

        public Builder delay(long d) { this.delay = d; return this; }
        public Builder repeat(long r) { this.repeat = r; return this; }
        public Builder maxRuns(int m) { this.maxRuns = m; return this; }
        public Builder priority(Priority p) { this.priority = p; return this; }

        public Builder level(ServerLevel lvl) { this.level = lvl; return this; }
        public Builder player(ServerPlayer p) { this.player = p; return this; }
        public Builder pos(BlockPos p) { this.pos = p; return this; }

        public Builder group(TaskGroup g) { this.group = g; return this; }
        public Builder persistent() { this.persistent = true; return this; }
        public Builder type(String type) { this.typeId = type; return this; }

        public Builder data(CompoundTag tag) {
            this.data = tag == null ? null : tag.copy();
            return this;
        }

        public Handle submit(TaskFn fn) {
            return TickScheduler.get().schedule(this, fn);
        }
    }

    public static class TaskGroup {
        public final String id;
        private final Set<Long> ids = ConcurrentHashMap.newKeySet();

        public TaskGroup(String id) { this.id = id; }
        public void register(long taskId) { ids.add(taskId); }
        public void cancelAll() { TickScheduler.get().cancelGroup(id); }
    }

    public static class ProfilerStats {
        public final long tickFrom, tickTo, tasksExecuted, totalNanos, maxTaskNanos;

        public ProfilerStats(long a,long b,long c,long d,long e){
            tickFrom=a;tickTo=b;tasksExecuted=c;totalNanos=d;maxTaskNanos=e;
        }

        public double getAvgMillisPerTask() {
            return tasksExecuted==0?0:(totalNanos/1_000_000.0)/tasksExecuted;
        }
        public double getMaxMillis() {
            return maxTaskNanos/1_000_000.0;
        }
    }

    public static class TaskSnapshot {
        public final long delayRemaining, repeat;
        public final int maxRuns;
        public final Priority priority;
        public final String groupId, typeId;
        public final boolean persistent;
        public final CompoundTag data;

        public TaskSnapshot(long d,long r,int m,Priority p,String g,boolean per,String t,CompoundTag data){
            delayRemaining=d;repeat=r;maxRuns=m;priority=p;groupId=g;persistent=per;typeId=t;
            this.data=data;
        }
    }
}
