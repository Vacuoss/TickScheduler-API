package com.avacuoss.TickScheduler;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.nio.file.Path;

public class SchedulerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("scheduler")
                        .requires(src -> src.hasPermission(0))

                        // /scheduler debug
                        .then(Commands.literal("debug")
                                .executes(ctx -> {
                                    boolean now = !TickScheduler.get().debug;
                                    TickScheduler.get().setDebug(now);
                                    ctx.getSource().sendSuccess(() ->
                                            text("Debug mode: " + now), false);
                                    return Command.SINGLE_SUCCESS;
                                }))

                        // /scheduler profiler
                        .then(Commands.literal("profiler")
                                .then(Commands.literal("on")
                                        .executes(ctx -> {
                                            TickScheduler.get().setProfiling(true);
                                            ctx.getSource().sendSuccess(() ->
                                                    text("Profiler enabled."), false);
                                            return 1;
                                        }))
                                .then(Commands.literal("off")
                                        .executes(ctx -> {
                                            TickScheduler.get().setProfiling(false);
                                            ctx.getSource().sendSuccess(() ->
                                                    text("Profiler disabled."), false);
                                            return 1;
                                        }))
                                .then(Commands.literal("stats")
                                        .executes(ctx -> {
                                            var s = TickScheduler.get().getProfilerStats();
                                            ctx.getSource().sendSuccess(() ->
                                                            text("Profiler stats:\n" +
                                                                    " Tasks: " + s.tasksExecuted + "\n" +
                                                                    " Avg: " + s.getAvgMillisPerTask() + " ms\n" +
                                                                    " Max: " + s.getMaxMillis() + " ms\n" +
                                                                    " Range: " + s.tickFrom + " - " + s.tickTo),
                                                    false);
                                            return 1;
                                        }))
                        )

                        // /scheduler save
                        .then(Commands.literal("save")
                                .executes(ctx -> {
                                    try {
                                        Path file = Path.of("scheduler_snapshot.json");
                                        PersistentStorage.save(file);
                                        ctx.getSource().sendSuccess(
                                                () -> text("Snapshot saved → " + file.toAbsolutePath()),
                                                false
                                        );
                                    } catch (Exception e) {
                                        ctx.getSource().sendFailure(text("ERROR: " + e.getMessage()));
                                    }
                                    return 1;
                                }))

                        // /scheduler load
                        .then(Commands.literal("load")
                                .executes(ctx -> {
                                    try {
                                        Path file = Path.of("scheduler_snapshot.json");
                                        PersistentStorage.load(file);
                                        ctx.getSource().sendSuccess(
                                                () -> text("Snapshot loaded."),
                                                false
                                        );
                                    } catch (Exception e) {
                                        ctx.getSource().sendFailure(text("ERROR: " + e.getMessage()));
                                    }
                                    return 1;
                                }))

                        // /scheduler tasks
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

                        // /scheduler test
                        .then(Commands.literal("test")
                                .executes(ctx -> {
                                    TickSchedulerTest.run();
                                    ctx.getSource().sendSuccess(
                                            () -> text("Tests started."),
                                            false
                                    );
                                    return 1;
                                }))
        );
    }

    private static net.minecraft.network.chat.Component text(String s) {
        return net.minecraft.network.chat.Component.literal(s);
    }
}
