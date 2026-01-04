package com.avacuoss.TickScheduler.Ticks;

import com.avacuoss.TickScheduler.Storage.TickSchedulerSavedData;
import com.avacuoss.TickScheduler.Types.Types;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class TickScheduler {

    private static final TickScheduler I = new TickScheduler();
    public static TickScheduler get() { return I; }

    private long tick = 0;
    public long getTick() { return tick; }
    public static long getTickStatic() { return get().getTick(); }

    private volatile Thread serverThread = null;

    private final ExecutorService asyncPool = Executors.newFixedThreadPool(2);
    private final ConcurrentLinkedQueue<Runnable> mainThreadQueue = new ConcurrentLinkedQueue<>();

    private final AtomicLong ids = new AtomicLong(0);
    private final Map<Long, Types.Task> all = new HashMap<>();

    private final PriorityQueue<Types.Task> pq = new PriorityQueue<>((a, b) -> {
        int c = Long.compare(a.next, b.next);
        if (c != 0) return c;
        c = Integer.compare(b.priority.ordinal(), a.priority.ordinal());
        if (c != 0) return c;
        return Long.compare(a.id, b.id);
    });

    private final List<ConditionWatcher> watchers = new ArrayList<>();

    private boolean debug = false;
    public void setDebug(boolean v) { debug = v; }
    public boolean isDebug() { return debug; }

    //profiler
    private boolean profiling = false;
    private long profStartTick = 0;
    private long profTasks = 0;
    private long profTotalNanos = 0;
    private long profMaxTaskNanos = 0;
    private Types.ProfilerStats lastStats = new Types.ProfilerStats(0,0,0,0,0);

    public void setProfiling(boolean enabled) {
        if (enabled) {
            profiling = true;
            profStartTick = tick;
            profTasks = 0;
            profTotalNanos = 0;
            profMaxTaskNanos = 0;
        } else {
            profiling = false;
            lastStats = new Types.ProfilerStats(profStartTick, tick, profTasks, profTotalNanos, profMaxTaskNanos);
        }
    }

    public Types.ProfilerStats getProfilerStats() {
        if (profiling)
            return new Types.ProfilerStats(profStartTick, tick, profTasks, profTotalNanos, profMaxTaskNanos);
        return lastStats;
    }

    private TickScheduler() {}

    //tick
    public void tick(MinecraftServer server) {
        if (serverThread == null) serverThread = Thread.currentThread();

        for (Runnable r; (r = mainThreadQueue.poll()) != null; ) {
            try { r.run(); } catch (Throwable t) { t.printStackTrace(); }
        }

        tick++;

        processWatchers(server);
        runDueTasks(server);
    }

    //watchers
    private void processWatchers(MinecraftServer server) {
        for (int i = watchers.size() - 1; i >= 0; i--) {
            ConditionWatcher w = watchers.get(i);
            if (w.cancelled) { watchers.remove(i); continue; }

            Types.Context ctx = new Types.Context(server, tick, w.level, w.player, w.pos, null);

            if (w.timeoutTick >= 0 && tick >= w.timeoutTick) {
                if (w.onTimeout != null)
                    safeRunFake(ctx, () -> w.onTimeout.run(ctx));
                if (w.groupId != null) cancelGroup(w.groupId);
                watchers.remove(i);
                continue;
            }

            if (tick < w.nextCheckTick) continue;

            boolean ok = false;
            try { ok = w.condition.getAsBoolean(); } catch (Throwable t) { t.printStackTrace(); }

            if (ok) {
                if (w.onSuccess != null)
                    safeRunFake(ctx, () -> w.onSuccess.run(ctx));
                if (w.groupId != null) cancelGroup(w.groupId);
                watchers.remove(i);
            } else {
                w.nextCheckTick = tick + w.checkEvery;
            }
        }
    }

    void addWatcher(ConditionWatcher w) {
        if (w == null) return;
        if (isServerThread()) watchers.add(w);
        else mainThreadQueue.add(() -> watchers.add(w));
    }

    //tesk
    private void runDueTasks(MinecraftServer server) {
        while (!pq.isEmpty()) {
            Types.Task t = pq.peek();
            if (t.next > tick) break;
            pq.poll();

            if (t.cancelled) { all.remove(t.id); continue; }

            Types.Context ctx = new Types.Context(server, tick, t.level, t.player, t.pos, t.data);

            boolean cont;
            try {
                safeRun(t, ctx, () -> t.fn.run(ctx));
                t.runs++;
                cont = t.repeat > 0 && (t.maxRuns <= 0 || t.runs < t.maxRuns);
            } catch (Throwable ex) {
                ex.printStackTrace();
                cont = false;
            }

            if (cont && !t.cancelled) {
                t.next = tick + t.repeat;
                pq.add(t);
            } else {
                all.remove(t.id);
            }
        }
    }

    private void safeRun(Types.Task t, Types.Context ctx, Runnable r) {
        if (!profiling) r.run();
        else {
            long start = System.nanoTime();
            try { r.run(); }
            finally {
                long dt = System.nanoTime() - start;
                profTasks++;
                profTotalNanos += dt;
                if (dt > profMaxTaskNanos) profMaxTaskNanos = dt;
            }
        }

        if (t.persistent && t.data != null) {
            TickSchedulerSavedData.markDirty(ctx.server);
        }
    }

    private void safeRunFake(Types.Context ctx, Runnable r) {
        if (!profiling) r.run();
        else {
            long start = System.nanoTime();
            try { r.run(); }
            finally {
                long dt = System.nanoTime() - start;
                profTasks++;
                profTotalNanos += dt;
                if (dt > profMaxTaskNanos) profMaxTaskNanos = dt;
            }
        }
    }

    //scheduling
    public Types.Handle schedule(Types.Builder b, Types.TaskFn fn) {
        long id = ids.incrementAndGet();
        long execTick = tick + Math.max(0, b.delay);

        CompoundTag dataCopy = b.data == null ? null : b.data.copy();

        Types.Task task = new Types.Task(
                id, execTick,
                Math.max(0, b.repeat),
                b.maxRuns,
                b.priority,
                fn,
                b.level, b.player, b.pos,
                b.group == null ? null : b.group.id,
                b.persistent,
                b.typeId,
                dataCopy
        );

        all.put(id, task);

        Runnable add = () -> {
            pq.add(task);
            if (b.group != null) b.group.register(id);
        };

        if (isServerThread()) add.run();
        else mainThreadQueue.add(add);

        return new Types.Handle(id);
    }

    public boolean cancel(long id) {
        Types.Task t = all.get(id);
        if (t == null) return false;
        t.cancelled = true;
        return true;
    }

    public void cancelGroup(String groupId) {
        for (Types.Task t : all.values())
            if (groupId != null && groupId.equals(t.groupId)) t.cancelled = true;
        for (ConditionWatcher w : watchers)
            if (groupId != null && groupId.equals(w.groupId)) w.cancelled = true;
    }

    public List<Types.TaskSnapshot> snapshotPersistentTasks() {
        List<Types.TaskSnapshot> out = new ArrayList<>();
        for (Types.Task t : all.values()) {
            if (!t.persistent || t.cancelled) continue;
            long delayRemaining = Math.max(0, t.next - tick);
            CompoundTag dataCopy = t.data == null ? null : t.data.copy();
            out.add(new Types.TaskSnapshot(delayRemaining, t.repeat, t.maxRuns, t.priority, t.groupId, true, t.typeId, dataCopy));
        }
        return out;
    }
    //returns true if current thread is minecraft server tick thread
    private boolean isServerThread() {
        return serverThread != null && Thread.currentThread() == serverThread;
    }

    //thereding
    public void runOnServerThread(Runnable r) {
        if (r == null) return;
        if (isServerThread()) r.run();
        else mainThreadQueue.add(r);
    }

    public void runAsync(Runnable r) {
        if (r == null) return;
        asyncPool.submit(r);
    }

    //WATCHER
    static final class ConditionWatcher {
        final java.util.function.BooleanSupplier condition;
        final long checkEvery;
        long nextCheckTick;
        final long timeoutTick;
        final Types.TaskFn onSuccess;
        final Types.TaskFn onTimeout;
        final String groupId;
        final net.minecraft.server.level.ServerLevel level;
        final net.minecraft.server.level.ServerPlayer player;
        final net.minecraft.core.BlockPos pos;
        volatile boolean cancelled = false;

        ConditionWatcher(java.util.function.BooleanSupplier condition, long checkEvery, long firstCheckTick,
                         long timeoutTick, Types.TaskFn onSuccess, Types.TaskFn onTimeout, String groupId,
                         net.minecraft.server.level.ServerLevel level, net.minecraft.server.level.ServerPlayer player,
                         net.minecraft.core.BlockPos pos) {
            this.condition = condition;
            this.checkEvery = Math.max(1, checkEvery);
            this.nextCheckTick = firstCheckTick;
            this.timeoutTick = timeoutTick;
            this.onSuccess = onSuccess;
            this.onTimeout = onTimeout;
            this.groupId = groupId;
            this.level = level;
            this.player = player;
            this.pos = pos;
        }
    }
}
