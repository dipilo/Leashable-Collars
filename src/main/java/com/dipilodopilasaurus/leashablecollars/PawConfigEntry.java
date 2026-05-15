package com.dipilodopilasaurus.leashablecollars;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record PawConfigEntry(boolean tag, ResourceLocation id) {
    private static final String TAG_KEY = "Tag";
    private static final String ID_KEY = "Id";

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(tag);
        buffer.writeResourceLocation(id);
    }

    public static PawConfigEntry decode(FriendlyByteBuf buffer) {
        return new PawConfigEntry(buffer.readBoolean(), buffer.readResourceLocation());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_KEY, this.tag);
        tag.putString(ID_KEY, id.toString());
        return tag;
    }

    public static PawConfigEntry load(CompoundTag tag) {
        return new PawConfigEntry(tag.getBoolean(TAG_KEY), new ResourceLocation(tag.getString(ID_KEY)));
    }

    public boolean matchesItem(ItemStack stack) {
        if (tag) {
            return stack.is(ItemTags.create(id));
        }
        return Objects.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()), id);
    }

    public boolean matchesBlock(BlockState state) {
        if (tag) {
            return state.is(BlockTags.create(id));
        }
        return Objects.equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()), id);
    }

    public Component getDisplayName(boolean heldItems) {
        if (tag) {
            return Component.literal("#" + id);
        }
        if (heldItems) {
            return ForgeRegistries.ITEMS.getValue(id) != null
                    ? ForgeRegistries.ITEMS.getValue(id).getDescription()
                    : Component.literal(id.toString());
        }
        return ForgeRegistries.BLOCKS.getValue(id) != null
                ? ForgeRegistries.BLOCKS.getValue(id).getName()
                : Component.literal(id.toString());
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

    public static List<PawConfigEntry> readListTag(CompoundTag parent, String key) {
        if (!parent.contains(key, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag listTag = parent.getList(key, Tag.TAG_COMPOUND);
        List<PawConfigEntry> entries = new ArrayList<>(listTag.size());
        for (int i = 0; i < listTag.size(); i++) {
            entries.add(load(listTag.getCompound(i)));
        }
        return entries;
    }

    public static void writeListTag(CompoundTag parent, String key, List<PawConfigEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            parent.remove(key);
            return;
        }
        ListTag listTag = new ListTag();
        for (PawConfigEntry entry : entries) {
            listTag.add(entry.save());
        }
        parent.put(key, listTag);
    }
}