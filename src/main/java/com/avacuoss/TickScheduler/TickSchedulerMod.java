package com.avacuoss.TickScheduler;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

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
            TickSchedulerTest.run();
            System.out.println("[TickScheduler] Tests started after server init.");
        }
    }
}
