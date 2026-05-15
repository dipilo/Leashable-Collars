package com.dipilodopilasaurus.leashablecollars.item;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class FootPawsItem extends Item implements DyeableLeatherItem, ICurio, ICapabilityProvider {
    private static final String DISPLAY_TAG = "display";
    private static final String BEANS_TAG = "beans";

    private final int defaultColor;
    private final int defaultBeansColor;

    public FootPawsItem(int defaultColor, int defaultBeansColor) {
        super(new Properties().stacksTo(1));
        this.defaultColor = defaultColor;
        this.defaultBeansColor = defaultBeansColor;
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return this;
    }

    @Override
    public ItemStack getStack() {
        return new ItemStack(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction direction) {
        if (capability == CuriosCapability.ITEM) {
            return LazyOptional.of(() -> (T) this);
        }
        return LazyOptional.empty();
    }

    @Override
    public int getColor(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(DISPLAY_TAG);
        return tag != null && tag.contains("color", 99) ? tag.getInt("color") : defaultColor;
    }

    public int getBeansColor(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(DISPLAY_TAG);
        return tag != null && tag.contains(BEANS_TAG, 99) ? tag.getInt(BEANS_TAG) : defaultBeansColor;
    }

    public void setBeansColor(ItemStack stack, int color) {
        stack.getOrCreateTagElement(DISPLAY_TAG).putInt(BEANS_TAG, color);
    }

    @Override
    public void curioTick(SlotContext slotContext) {
        LivingEntity entity = slotContext.entity();
        if (entity.isSprinting()) {
            entity.fallDistance = 0.0F;
        }
    }
}
