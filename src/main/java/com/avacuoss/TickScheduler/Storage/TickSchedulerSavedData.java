package com.avacuoss.TickScheduler.Storage;

import com.avacuoss.TickScheduler.Ticks.TickScheduler;
import com.avacuoss.TickScheduler.Types.Types;
import com.avacuoss.TickScheduler.api.SchedulerAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class TickSchedulerSavedData extends SavedData {

    public static final String NAME = "tickscheduler";

    private final ListTag tasks = new ListTag();

    public static TickSchedulerSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                TickSchedulerSavedData::load,
                TickSchedulerSavedData::new,
                NAME
        );
    }

    public TickSchedulerSavedData() {}

    //save
    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("tasks", tasks);
        tag.putInt("schema", 2);
        return tag;
    }

    //load
    public static TickSchedulerSavedData load(CompoundTag tag) {
        TickSchedulerSavedData data = new TickSchedulerSavedData();
        if (tag.contains("tasks", Tag.TAG_LIST)) {
            data.tasks.addAll(tag.getList("tasks", Tag.TAG_COMPOUND));
        }
        return data;
    }

    //write from scheduler
    public void writeFromScheduler() {
        tasks.clear();

        for (Types.TaskSnapshot s : SchedulerAPI.getPersistentSnapshot()) {
            CompoundTag t = new CompoundTag();
            t.putLong("delay", s.delayRemaining);
            t.putLong("repeat", s.repeat);
            t.putInt("maxRuns", s.maxRuns);
            t.putString("priority", s.priority.name());

            if (s.groupId != null) t.putString("group", s.groupId);
            if (s.typeId != null) t.putString("type", s.typeId);
            if (s.data != null) t.put("data", s.data.copy());

            tasks.add(t);
        }

        setDirty(); //mark world dirty
    }
    public static void markDirty(MinecraftServer server) {
        get(server).setDirty();
    }


    //restore into scheduler
    public void restoreIntoScheduler() {
        for (Tag tag : tasks) {
            if (!(tag instanceof CompoundTag t)) continue;

            long delay = t.getLong("delay");
            long repeat = t.getLong("repeat");
            int maxRuns = t.getInt("maxRuns");
            Types.Priority pr = Types.Priority.valueOf(t.getString("priority"));

            String groupId = t.contains("group") ? t.getString("group") : null;
            String typeId = t.contains("type") ? t.getString("type") : null;
            CompoundTag data = t.contains("data", Tag.TAG_COMPOUND) ? t.getCompound("data").copy() : null;

            if (typeId == null) continue;

            var handler = PersistentTaskRegistry.get(typeId);
            if (handler == null) {
                System.out.println("[tickscheduler] missing persistent type " + typeId);
                continue;
            }

            Types.TaskGroup group = groupId != null ? new Types.TaskGroup(groupId) : null;

            SchedulerAPI.builder()
                    .delay(delay)
                    .repeat(repeat)
                    .maxRuns(maxRuns)
                    .priority(pr)
                    .group(group)
                    .persistent()
                    .type(typeId)
                    .data(data)
                    .submit(ctx -> handler.run(ctx, data));
        }
    }
}
