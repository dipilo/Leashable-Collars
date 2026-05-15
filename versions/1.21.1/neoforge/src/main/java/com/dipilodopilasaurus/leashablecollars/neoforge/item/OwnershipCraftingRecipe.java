package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.OwnerData;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class OwnershipCraftingRecipe extends CustomRecipe {
    private final Ingredient base;

    public OwnershipCraftingRecipe(CraftingBookCategory category, Ingredient base) {
        super(category);
        this.base = base;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack deed = ItemStack.EMPTY;
        ItemStack baseStack = ItemStack.EMPTY;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(LeashableCollarsNeoForge.STAMPED_DEED_OF_OWNERSHIP.get())) {
                if (!deed.isEmpty()) {
                    return false;
                }
                deed = stack;
            } else if (base.test(stack)) {
                if (!baseStack.isEmpty() || LeashableCollarsNeoForge.getOwnerData(stack) != null) {
                    return false;
                }
                baseStack = stack;
            } else {
                return false;
            }
        }

        return !deed.isEmpty() && !baseStack.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack deed = ItemStack.EMPTY;
        ItemStack output = ItemStack.EMPTY;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(LeashableCollarsNeoForge.STAMPED_DEED_OF_OWNERSHIP.get())) {
                deed = stack;
            } else if (base.test(stack)) {
                output = stack.copyWithCount(1);
            }
        }

        OwnerData ownerData = LeashableCollarsNeoForge.getOwnerData(deed);
        if (ownerData == null || output.isEmpty()) {
            return ItemStack.EMPTY;
        }

        LeashableCollarsNeoForge.setOwnerData(output, ownerData);
        return output;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return LeashableCollarsNeoForge.OWNERSHIP_RECIPE_SERIALIZER.get();
    }

    private Ingredient getBase() {
        return base;
    }

    public static class Serializer implements RecipeSerializer<OwnershipCraftingRecipe> {
        private static final MapCodec<OwnershipCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter(OwnershipCraftingRecipe::category),
                Ingredient.CODEC.fieldOf("base").forGetter(OwnershipCraftingRecipe::getBase)
        ).apply(instance, OwnershipCraftingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, OwnershipCraftingRecipe> STREAM_CODEC = StreamCodec.composite(
                CraftingBookCategory.STREAM_CODEC,
                OwnershipCraftingRecipe::category,
                Ingredient.CONTENTS_STREAM_CODEC,
                OwnershipCraftingRecipe::getBase,
                OwnershipCraftingRecipe::new);

        @Override
        public MapCodec<OwnershipCraftingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, OwnershipCraftingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}