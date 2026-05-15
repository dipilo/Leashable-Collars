package com.dipilodopilasaurus.leashablecollars.enchantment;

import com.dipilodopilasaurus.leashablecollars.item.CollarItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class CollarEnchantment extends Enchantment {
    private final int maxLevel;

    public CollarEnchantment(Rarity rarity, int maxLevel) {
        super(rarity, EnchantmentCategory.WEARABLE, new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
        this.maxLevel = maxLevel;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof CollarItem;
    }

    @Override
    public boolean isTradeable() {
        return true;
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }
}