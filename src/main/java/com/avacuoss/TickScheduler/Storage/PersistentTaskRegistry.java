package com.avacuoss.TickScheduler.Storage;

import com.avacuoss.TickScheduler.Types.Types;
import net.minecraft.nbt.CompoundTag;
import java.util.concurrent.ConcurrentHashMap;

public class PersistentTaskRegistry {

    @FunctionalInterface
    public interface PersistentHandler {
        void run(Types.Context ctx, CompoundTag data);
    }

    private static final ConcurrentHashMap<String, PersistentHandler> MAP = new ConcurrentHashMap<>();

    public static void register(String typeId, PersistentHandler handler) {
        if (typeId == null || typeId.isEmpty()) throw new IllegalArgumentException("typeId");
        if (handler == null) throw new IllegalArgumentException("handler");
        MAP.put(typeId, handler);
    }

    public static PersistentHandler get(String typeId) {
        return typeId == null ? null : MAP.get(typeId);
    }

    public static boolean has(String typeId) {
        return get(typeId) != null;
    }
}
