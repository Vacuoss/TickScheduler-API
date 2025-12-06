package com.avacuoss.TickScheduler;

import net.minecraft.server.MinecraftServer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class TickScheduler {

    private static final TickScheduler I = new TickScheduler();
    public static TickScheduler get() { return I; }

    private long tick = 0;
    private final Map<Long, List<Types.Task>> tasks = new HashMap<>();
    private final Map<Long, Types.Task> all = new HashMap<>();
    private final AtomicLong ids = new AtomicLong(0);

    private final ExecutorService asyncPool = Executors.newFixedThreadPool(2);
    private boolean debug = false;

    // Profiler
    private boolean profiling = false;
    private long profStartTick = 0;
    private long profTasks = 0;
    private long profTotalNanos = 0;
    private long profMaxTaskNanos = 0;

    private TickScheduler() {}

    public long getTick() { return tick; }
    public static long getTickStatic() { return get().getTick(); }

    public void setDebug(boolean v) { debug = v; }

    public void setProfiling(boolean enabled) {
        profiling = enabled;
        profStartTick = tick;
        profTasks = 0;
        profTotalNanos = 0;
        profMaxTaskNanos = 0;
    }

    public Types.ProfilerStats getProfilerStats() {
        return new Types.ProfilerStats(
                profStartTick, tick, profTasks, profTotalNanos, profMaxTaskNanos
        );
    }

    private void log(String msg) {
        if (debug) System.out.println("[TickScheduler] " + msg);
    }

    public void tick(MinecraftServer server) {
        tick++;

        List<Types.Task> due = tasks.remove(tick);
        if (due == null || due.isEmpty()) return;

        due.sort((a, b) -> {
            int pr = b.priority.ordinal() - a.priority.ordinal();
            if (pr != 0) return pr;
            return Long.compare(a.id, b.id);
        });

        for (Types.Task t : due) {

            if (t.cancelled) {
                all.remove(t.id);
                continue;
            }

            long startNanos = profiling ? System.nanoTime() : 0;
            boolean cont;

            try {
                Types.Context ctx = new Types.Context(server, tick, t.level, t.player, t.pos);
                if (debug)
                    log("run id=" + t.id + " pr=" + t.priority + " group=" + t.groupId + " type=" + t.typeId);
                t.fn.run(ctx);
                t.runs++;
                cont = t.repeat > 0 && (t.maxRuns <= 0 || t.runs < t.maxRuns);
            } catch (Throwable ex) {
                ex.printStackTrace();
                cont = false;
            }

            if (profiling) {
                long dt = System.nanoTime() - startNanos;
                profTasks++;
                profTotalNanos += dt;
                if (dt > profMaxTaskNanos) profMaxTaskNanos = dt;
            }

            if (cont) {
                t.next = tick + t.repeat;
                tasks.computeIfAbsent(t.next, k -> new ArrayList<>()).add(t);
            } else {
                all.remove(t.id);
            }
        }
    }

    public Types.Handle schedule(Types.Builder b, Types.TaskFn fn) {

        long id = ids.incrementAndGet();
        long exec = tick + b.delay;

        Types.Task t = new Types.Task(
                id, exec, b.repeat, b.maxRuns, b.priority,
                fn, b.level, b.player, b.pos,
                b.group == null ? null : b.group.id,
                b.persistent,
                b.typeId
        );

        all.put(id, t);
        tasks.computeIfAbsent(exec, k -> new ArrayList<>()).add(t);

        if (debug)
            log("schedule id=" + id + " at=" + exec + " type=" + b.typeId + " persistent=" + b.persistent);

        if (b.group != null) b.group.register(id);
        return new Types.Handle(id);
    }

    public boolean cancel(long id) {
        Types.Task t = all.get(id);
        if (t == null) return false;
        t.cancelled = true;
        return true;
    }

    public void cancelGroup(String groupId) {
        if (groupId == null) return;
        for (Types.Task t : all.values()) {
            if (groupId.equals(t.groupId)) t.cancelled = true;
        }
    }

    public boolean isCancelled(long id) {
        Types.Task t = all.get(id);
        return t == null || t.cancelled;
    }

    public void runAsync(Runnable r) {
        asyncPool.submit(r);
    }

    // Hybrid Persistent Snapshot API
    public List<Types.TaskSnapshot> snapshotPersistentTasks() {
        List<Types.TaskSnapshot> out = new ArrayList<>();
        for (Types.Task t : all.values()) {
            if (!t.persistent) continue;
            long delayRemaining = Math.max(0, t.next - tick);
            out.add(new Types.TaskSnapshot(
                    delayRemaining,
                    t.repeat,
                    t.maxRuns,
                    t.priority,
                    t.groupId,
                    t.persistent,
                    t.typeId
            ));
        }
        return out;
    }
}
