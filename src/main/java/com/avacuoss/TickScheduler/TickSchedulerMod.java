package com.avacuoss.TickScheduler;

import com.avacuoss.TickScheduler.Storage.TickSchedulerSavedData;
import com.avacuoss.TickScheduler.Ticks.TickScheduler;
import com.avacuoss.TickScheduler.commands.SchedulerCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(TickSchedulerMod.MOD_ID)
public class TickSchedulerMod {

    public static final String MOD_ID = "tickscheduler";

    public TickSchedulerMod() {
        MinecraftForge.EVENT_BUS.register(ForgeEvents.class);
        System.out.println("[TickScheduler] Initialized!");
    }

    public static class ForgeEvents {
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            SchedulerCommand.register(event.getDispatcher());
        }
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                TickScheduler.get().tick(event.getServer());
            }
        }
        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            // Restore persistent tasks from world data
            TickSchedulerSavedData.get(event.getServer()).restoreIntoScheduler();

            TickSchedulerTest.run();

            System.out.println("[TickScheduler] Persistent tasks restored.");
        }
        @SubscribeEvent
        public static void onWorldSave(LevelEvent.Save event) {
            if (event.getLevel() instanceof ServerLevel lvl) {
                TickSchedulerSavedData.get(lvl.getServer()).writeFromScheduler();
            }
        }

        @SubscribeEvent
        public static void onServerStopping(ServerStoppingEvent event) {
            TickSchedulerSavedData.get(event.getServer()).writeFromScheduler();
        }
    }
}

