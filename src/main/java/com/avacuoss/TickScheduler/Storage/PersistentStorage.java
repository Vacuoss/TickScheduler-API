package com.avacuoss.TickScheduler.Storage;

import com.google.gson.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.*;
import java.util.List;
import com.avacuoss.TickScheduler.api.SchedulerAPI;
import com.avacuoss.TickScheduler.Types.Types;

/**
 * Minimal persistent storage for scheduled tasks.
 *
 * Format is JSON with a schema version so we can evolve without breaking old saves.
 * Only persistent tasks are stored, and only tasks with a registered typeId can be fully restored.
 */
public class PersistentStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SCHEMA_VERSION = 1;

    public static void save(Path file) throws IOException {
        List<Types.TaskSnapshot> snaps = SchedulerAPI.getPersistentSnapshot();

        JsonArray tasks = new JsonArray();
        for (Types.TaskSnapshot s : snaps) {
            JsonObject o = new JsonObject();
            o.addProperty("delay", s.delayRemaining);
            o.addProperty("repeat", s.repeat);
            o.addProperty("maxRuns", s.maxRuns);
            o.addProperty("priority", s.priority.name());
            if (s.groupId != null) o.addProperty("groupId", s.groupId);
            o.addProperty("persistent", s.persistent);
            if (s.typeId != null) o.addProperty("type", s.typeId);
            tasks.add(o);
        }

        JsonObject root = new JsonObject();
        root.addProperty("schema", SCHEMA_VERSION);
        root.add("tasks", tasks);

        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);

        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp)) {
            GSON.toJson(root, w);
        }
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void load(Path file) throws IOException {
        if (file == null || !Files.exists(file)) return;

        JsonElement parsed;
        try (Reader r = Files.newBufferedReader(file)) {
            parsed = JsonParser.parseReader(r);
        }
        JsonArray arr;
        if (parsed != null && parsed.isJsonObject()) {
            JsonObject root = parsed.getAsJsonObject();
            JsonElement tasks = root.get("tasks");
            arr = tasks != null && tasks.isJsonArray() ? tasks.getAsJsonArray() : new JsonArray();
        } else if (parsed != null && parsed.isJsonArray()) {
            arr = parsed.getAsJsonArray();
        } else {
            return;
        }

        for (JsonElement e : arr) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();

            long delay = getLong(o, "delay", 0);
            long repeat = getLong(o, "repeat", 0);
            int maxRuns = (int) getLong(o, "maxRuns", 0);
            Types.Priority pr = Types.Priority.valueOf(getString(o, "priority", Types.Priority.NORMAL.name()));
            String groupId = getString(o, "groupId", null);
            String typeId = getString(o, "type", null);

            Types.TaskGroup group = groupId != null ? new Types.TaskGroup(groupId) : null;

            if (typeId != null) {
                Types.TaskFn fn = TaskRegistry.get(typeId);
                if (fn == null) {
                    System.out.println("[TickScheduler] Unknown persistent task typeId='" + typeId + "'. Skipping.");
                    continue;
                }
                SchedulerAPI.builder()
                        .delay(delay)
                        .repeat(repeat)
                        .maxRuns(maxRuns)
                        .priority(pr)
                        .group(group)
                        .persistent()
                        .type(typeId)
                        .submit(fn);
            } else {

                SchedulerAPI.builder()
                        .delay(delay)
                        .repeat(repeat)
                        .maxRuns(maxRuns)
                        .priority(pr)
                        .group(group)
                        .persistent()
                        .submit(ctx -> System.out.println("[TickScheduler] Skipped persistent task without typeId (cannot restore code)."));
            }
        }
    }

    private static long getLong(JsonObject o, String key, long def) {
        JsonElement e = o.get(key);
        return (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) ? e.getAsLong() : def;
    }

    private static String getString(JsonObject o, String key, String def) {
        JsonElement e = o.get(key);
        return (e != null && e.isJsonPrimitive()) ? e.getAsString() : def;
    }

    /**
     * Registry for restoring persistent tasks.
     * The recommended pattern is:
     *   TaskRegistry.register("my_mod:task_type", ctx -> {...});
     *
     * If you want truly persistent tasks with custom data, consider extending the storage
     * format to include a "data" JSON object and registering a deserializer.
     */
    public static class TaskRegistry {
        private static final java.util.Map<String, Types.TaskFn> MAP = new java.util.concurrent.ConcurrentHashMap<>();

        public static void register(String typeId, Types.TaskFn fn) {
            if (typeId == null || typeId.isEmpty()) throw new IllegalArgumentException("typeId");
            if (fn == null) throw new IllegalArgumentException("fn");
            MAP.put(typeId, fn);
        }

        public static Types.TaskFn get(String typeId) {
            return typeId == null ? null : MAP.get(typeId);
        }
    }
}
