package com.avacuoss.TickScheduler.Storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

//nbt that auto-marks SavedData dirty when modified
public class DirtyCompoundTag extends CompoundTag {

    private final Runnable onDirty;

    public DirtyCompoundTag(Runnable onDirty) {
        this.onDirty = onDirty;
    }

    //mark save dirty
    private void dirty() {
        if (onDirty != null) onDirty.run();
    }

    @Override public void putInt(String key, int value) { super.putInt(key, value); dirty(); }
    @Override public void putLong(String key, long value) { super.putLong(key, value); dirty(); }
    @Override public void putString(String key, String value) { super.putString(key, value); dirty(); }
    @Override public void putBoolean(String key, boolean value) { super.putBoolean(key, value); dirty(); }
    @Override public Tag put(String key, Tag tag) { super.put(key, tag); dirty();
        return tag;
    }
    @Override public void remove(String key) { super.remove(key); dirty(); }
}
