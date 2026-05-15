package com.dipilodopilasaurus.leashablecollars.neoforge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record PawConfigEntry(boolean tag, ResourceLocation id) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(tag);
        buffer.writeResourceLocation(id);
    }

    public static PawConfigEntry decode(FriendlyByteBuf buffer) {
        return new PawConfigEntry(buffer.readBoolean(), buffer.readResourceLocation());
    }

    public boolean matchesItem(ItemStack stack) {
        if (tag) {
            return stack.is(ItemTags.create(id));
        }
        return Objects.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()), id);
    }

    public boolean matchesBlock(BlockState state) {
        if (tag) {
            return state.is(BlockTags.create(id));
        }
        return Objects.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()), id);
    }

    public Component getDisplayName(boolean heldItems) {
        if (tag) {
            return Component.literal("#" + id);
        }
        if (heldItems) {
            return BuiltInRegistries.ITEM.getOptional(id)
                    .map(item -> item.getDefaultInstance().getHoverName())
                    .orElse(Component.literal(id.toString()));
        }
        return BuiltInRegistries.BLOCK.getOptional(id)
                .map(block -> block.getName())
                .orElse(Component.literal(id.toString()));
    }

    public String asStoredString() {
        return (tag ? "tag:" : "id:") + id;
    }

    public static PawConfigEntry fromStoredString(String stored) {
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        if (stored.startsWith("tag:")) {
            return new PawConfigEntry(true, ResourceLocation.parse(stored.substring(4)));
        }
        if (stored.startsWith("id:")) {
            return new PawConfigEntry(false, ResourceLocation.parse(stored.substring(3)));
        }
        return new PawConfigEntry(false, ResourceLocation.parse(stored));
    }

    public static List<PawConfigEntry> fromStored(List<String> stored) {
        if (stored == null || stored.isEmpty()) {
            return List.of();
        }
        List<PawConfigEntry> entries = new ArrayList<>(stored.size());
        for (String value : stored) {
            PawConfigEntry entry = fromStoredString(value);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public static List<String> toStored(List<PawConfigEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<String> stored = new ArrayList<>(entries.size());
        for (PawConfigEntry entry : entries) {
            stored.add(entry.asStoredString());
        }
        return List.copyOf(stored);
    }

    public static List<PawConfigEntry> readList(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<PawConfigEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(decode(buffer));
        }
        return entries;
    }

    public static void writeList(FriendlyByteBuf buffer, List<PawConfigEntry> entries) {
        buffer.writeVarInt(entries.size());
        for (PawConfigEntry entry : entries) {
            entry.encode(buffer);
        }
    }
}
