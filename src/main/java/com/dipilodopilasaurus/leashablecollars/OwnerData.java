package com.dipilodopilasaurus.leashablecollars;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public record OwnerData(UUID uuid, String name, Optional<UUID> owned, Optional<String> ownedName) {
    public static final String OWNER_UUID = "uuid";
    public static final String OWNER_NAME = "name";
    public static final String OWNED_UUID = "owned";
    public static final String OWNED_NAME = "owned_name";

    public OwnerData(UUID uuid, String name) {
        this(uuid, name, Optional.empty(), Optional.empty());
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(OWNER_UUID, uuid);
        tag.putString(OWNER_NAME, name);
        owned.ifPresent(value -> tag.putUUID(OWNED_UUID, value));
        ownedName.ifPresent(value -> tag.putString(OWNED_NAME, value));
        return tag;
    }

    public static @Nullable OwnerData fromTag(@Nullable CompoundTag tag) {
        if (tag == null || !tag.hasUUID(OWNER_UUID) || !tag.contains(OWNER_NAME)) {
            return null;
        }
        Optional<UUID> owned = tag.hasUUID(OWNED_UUID) ? Optional.of(tag.getUUID(OWNED_UUID)) : Optional.empty();
        Optional<String> ownedName = tag.contains(OWNED_NAME) ? Optional.of(tag.getString(OWNED_NAME)) : Optional.empty();
        return new OwnerData(tag.getUUID(OWNER_UUID), tag.getString(OWNER_NAME), owned, ownedName);
    }
}
