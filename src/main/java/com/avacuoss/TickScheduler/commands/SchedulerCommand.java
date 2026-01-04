package com.avacuoss.TickScheduler.commands;

import com.avacuoss.TickScheduler.TickSchedulerTest;
import com.avacuoss.TickScheduler.Ticks.TickScheduler;
import com.avacuoss.TickScheduler.api.SchedulerAPI;
import com.avacuoss.TickScheduler.Storage.PersistentStorage;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.nio.file.Path;

public class SchedulerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("scheduler")
                        .requires(src -> src.hasPermission(0))

                        //debug
                        .then(Commands.literal("debug")
                                .executes(ctx -> {
                                    boolean now = !TickScheduler.get().isDebug();
                                    TickScheduler.get().setDebug(now);
                                    ctx.getSource().sendSuccess(() -> text("Debug mode: " + now), false);
                                    return Command.SINGLE_SUCCESS;
                                }))

                        //profile
                        .then(Commands.literal("profiler")
                                .then(Commands.literal("on")
                                        .executes(ctx -> {
                                            TickScheduler.get().setProfiling(true);
                                            ctx.getSource().sendSuccess(() -> text("Profiler enabled"), false);
                                            return 1;
                                        }))
                                .then(Commands.literal("off")
                                        .executes(ctx -> {
                                            TickScheduler.get().setProfiling(false);
                                            ctx.getSource().sendSuccess(() -> text("Profiler disabled"), false);
                                            return 1;
                                        }))
                                .then(Commands.literal("stats")
                                        .executes(ctx -> {
                                            var s = TickScheduler.get().getProfilerStats();

                                            double totalMs = s.totalNanos / 1_000_000.0;
                                            long ticks = Math.max(1, s.tickTo - s.tickFrom);
                                            double perTick = totalMs / ticks;

                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "§6TickScheduler Profiler\n" +
                                                            "§7---------------------\n" +
                                                            "§fTicks: §e" + s.tickFrom + " → " + s.tickTo + "\n" +
                                                            "§fTasks: §e" + s.tasksExecuted + "\n" +
                                                            "§fAvg: §e" + String.format("%.3f", s.getAvgMillisPerTask()) + " ms\n" +
                                                            "§fMax: §e" + String.format("%.3f", s.getMaxMillis()) + " ms\n" +
                                                            "§fTotal: §e" + String.format("%.3f", totalMs) + " ms\n" +
                                                            "§fTPS cost: §e" + String.format("%.3f", perTick) + " ms/tick"
                                            ), false);
                                            return 1;
                                        }))
                        )

                        //snapshot
                        .then(Commands.literal("save")
                                .executes(ctx -> {
                                    try {
                                        Path file = Path.of("scheduler_snapshot.json");
                                        PersistentStorage.save(file);
                                        ctx.getSource().sendSuccess(() ->
                                                text("Snapshot saved → " + file.toAbsolutePath()), false);
                                    } catch (Exception e) {
                                        ctx.getSource().sendFailure(text("Save failed: " + e.getMessage()));
                                    }
                                    return 1;
                                }))

                        .then(Commands.literal("load")
                                .executes(ctx -> {
                                    try {
                                        Path file = Path.of("scheduler_snapshot.json");
                                        PersistentStorage.load(file);
                                        ctx.getSource().sendSuccess(() -> text("Snapshot loaded"), false);
                                    } catch (Exception e) {
                                        ctx.getSource().sendFailure(text("Load failed: " + e.getMessage()));
                                    }
                                    return 1;
                                }))

                        .then(Commands.literal("tasks")
                                .executes(ctx -> {
                                    var snaps = SchedulerAPI.getPersistentSnapshot();
                                    ctx.getSource().sendSuccess(() ->
                                            text("Persistent tasks: " + snaps.size()), false);
                                    for (var s : snaps) {
                                        ctx.getSource().sendSuccess(() ->
                                                text("- type=" + s.typeId +
                                                        " delay=" + s.delayRemaining +
                                                        " repeat=" + s.repeat), false);
                                    }
                                    return 1;
                                }))

                        //test
                        .then(Commands.literal("test")

                                // core engine tests
                                .then(Commands.literal("core")
                                        .executes(ctx -> {
                                            TickSchedulerTest.run();
                                            ctx.getSource().sendSuccess(() -> text("Core tests started"), false);
                                            return 1;
                                        }))

                                //speed potion test
                                .then(Commands.literal("speed")
                                        .executes(ctx -> {
                                            var p = ctx.getSource().getPlayerOrException();

                                            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 5, 1));

                                            SchedulerAPI.after(20 * 5, c -> {
                                                if (c.player != null) {
                                                    c.player.removeEffect(MobEffects.MOVEMENT_SPEED);
                                                    c.player.sendSystemMessage(text("Speed expired"));
                                                }
                                            });

                                            p.sendSystemMessage(text("Speed applied for 5s"));
                                            return 1;
                                        }))

                                //persistent cooldown
                                .then(Commands.literal("cooldown")
                                        .executes(ctx -> {
                                            var p = ctx.getSource().getPlayerOrException();

                                            CompoundTag data = new CompoundTag();
                                            data.putUUID("player", p.getUUID());

                                            SchedulerAPI.builder()
                                                    .delay(20 * 10)
                                                    .persistent()
                                                    .type("cooldown_test")
                                                    .data(data)
                                                    .submit(c -> {
                                                        if (c.player != null) {
                                                            c.player.sendSystemMessage(text("Cooldown finished"));
                                                        } else if (c.data() != null) {
                                                            var pl = c.server.getPlayerList()
                                                                    .getPlayer(c.data().getUUID("player"));
                                                            if (pl != null)
                                                                pl.sendSystemMessage(text("Cooldown finished"));
                                                        }
                                                    });

                                            p.sendSystemMessage(text("Cooldown started (persistent, 10s)"));
                                            return 1;
                                        }))

                                //async
                                .then(Commands.literal("async")
                                        .executes(ctx -> {
                                            SchedulerAPI.runAsync(() -> {
                                                String thread = Thread.currentThread().getName();
                                                TickScheduler.get().runOnServerThread(() ->
                                                        ctx.getSource().sendSuccess(() ->
                                                                text("Async thread=" + thread), false)
                                                );
                                            });
                                            return 1;
                                        }))

                                //condition scheduler
                                .then(Commands.literal("condition")
                                        .executes(ctx -> {
                                            long start = TickScheduler.getTickStatic();
                                            SchedulerAPI.when(() -> TickScheduler.getTickStatic() > start + 40)
                                                    .checkEvery(5)
                                                    .onSuccess(c ->
                                                            ctx.getSource().sendSuccess(() ->
                                                                    text("Condition success"), false))
                                                    .timeout(100)
                                                    .onTimeout(c ->
                                                            ctx.getSource().sendSuccess(() ->
                                                                    text("Condition timeout"), false))
                                                    .start();
                                            return 1;
                                        }))

                                //run all tests
                                .then(Commands.literal("all")
                                        .executes(ctx -> {
                                            TickSchedulerTest.run();
                                            ctx.getSource().sendSuccess(() ->
                                                    text("Core tests started"), false);

                                            var server = ctx.getSource().getServer();
                                            server.execute(() -> server.getCommands().performPrefixedCommand(ctx.getSource(), "scheduler test speed"));
                                            server.execute(() -> server.getCommands().performPrefixedCommand(ctx.getSource(), "scheduler test async"));
                                            server.execute(() -> server.getCommands().performPrefixedCommand(ctx.getSource(), "scheduler test condition"));
                                            server.execute(() -> server.getCommands().performPrefixedCommand(ctx.getSource(), "scheduler test cooldown"));

                                            return 1;
                                        }))
                        )
        );
    }

    private static Component text(String s) {
        return Component.literal(s);
    }
}
