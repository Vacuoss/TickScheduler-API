package com.avacuoss.TickScheduler;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PersistentStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();


    public static void save(Path file) throws IOException {
        List<Types.TaskSnapshot> snaps = SchedulerAPI.getPersistentSnapshot();

        JsonArray arr = new JsonArray();
        for (Types.TaskSnapshot s : snaps) {
            JsonObject o = new JsonObject();
            o.addProperty("delay", s.delayRemaining);
            o.addProperty("repeat", s.repeat);
            o.addProperty("maxRuns", s.maxRuns);
            o.addProperty("priority", s.priority.name());
            o.addProperty("groupId", s.groupId);
            o.addProperty("persistent", s.persistent);
            if (s.typeId != null) o.addProperty("type", s.typeId);
            arr.add(o);
        }

        Files.writeString(file, GSON.toJson(arr));
    }

    public static void load(Path file) throws IOException {
        if (!Files.exists(file)) return;

        JsonArray arr = JsonParser.parseString(Files.readString(file)).getAsJsonArray();

        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();

            long delay = o.get("delay").getAsLong();
            long repeat = o.get("repeat").getAsLong();
            int maxRuns = o.get("maxRuns").getAsInt();
            Types.Priority pr = Types.Priority.valueOf(o.get("priority").getAsString());
            String groupId = o.has("groupId") && !o.get("groupId").isJsonNull() ? o.get("groupId").getAsString() : null;
            boolean persistent = o.get("persistent").getAsBoolean();
            String type = o.has("type") ? o.get("type").getAsString() : null;

            Types.TaskGroup group = groupId != null ? new Types.TaskGroup(groupId) : null;

            if (type != null) {
                // Есть typeId → используем TaskRegistry
                Types.TaskFn fn = TaskRegistry.get(type);
                if (fn == null) {
                    System.err.println("[TickScheduler] WARNING: Task type '" + type + "' not registered!");
                    continue;
                }

                SchedulerAPI.builder()
                        .delay(delay)
                        .repeat(repeat)
                        .maxRuns(maxRuns)
                        .priority(pr)
                        .group(group)
                        .persistent()
                        .type(type)
                        .submit(fn);

            } else {
                SchedulerAPI.builder()
                        .delay(delay)
                        .repeat(repeat)
                        .maxRuns(maxRuns)
                        .priority(pr)
                        .group(group)
                        .persistent()
                        .submit(ctx -> {
                            System.out.println("[TickScheduler] TODO persistent task without typeId");
                        });
            }
        }
    }


    public static class TaskRegistry {
        private static final Map<String, Types.TaskFn> MAP = new HashMap<>();

        public static void register(String typeId, Types.TaskFn fn) {
            MAP.put(typeId, fn);
        }

        public static Types.TaskFn get(String typeId) {
            return MAP.get(typeId);
        }
    }
}
