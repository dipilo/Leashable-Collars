package com.dipilodopilasaurus.leashablecollars.item;

import com.dipilodopilasaurus.leashablecollars.LeashableCollars;
import com.dipilodopilasaurus.leashablecollars.OwnerData;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;

public class OwnershipCraftingRecipe extends CustomRecipe {
    private final Ingredient base;

    public OwnershipCraftingRecipe(ResourceLocation id, CraftingBookCategory category, Ingredient base) {
        super(id, category);
        this.base = base;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        ItemStack deed = ItemStack.EMPTY;
        ItemStack baseStack = ItemStack.EMPTY;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(LeashableCollars.STAMPED_DEED_OF_OWNERSHIP.get())) {
                if (!deed.isEmpty()) {
                    return false;
                }
                deed = stack;
            } else if (base.test(stack)) {
                if (!baseStack.isEmpty() || CollarItem.getOwnerData(stack) != null) {
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
    public ItemStack assemble(CraftingContainer container, net.minecraft.core.RegistryAccess registryAccess) {
        ItemStack deed = ItemStack.EMPTY;
        ItemStack output = ItemStack.EMPTY;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(LeashableCollars.STAMPED_DEED_OF_OWNERSHIP.get())) {
                deed = stack;
            } else if (base.test(stack)) {
                output = stack.copy();
                output.setCount(1);
            }
        }

        OwnerData ownerData = CollarItem.getOwnerData(deed);
        if (ownerData == null || output.isEmpty()) {
            return ItemStack.EMPTY;
        }
        CollarItem.setOwnerData(output, ownerData);
        return output;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.hasCraftingRemainingItem()) {
                remaining.set(i, stack.getCraftingRemainingItem());
            }
        }
        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return LeashableCollars.OWNERSHIP_RECIPE_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<OwnershipCraftingRecipe> {
        @Override
        public OwnershipCraftingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            CraftingBookCategory category = CraftingBookCategory.CODEC.byName(json.has("category") ? json.get("category").getAsString() : "misc", CraftingBookCategory.MISC);
            Ingredient ingredient = CraftingHelper.getIngredient(json.get("base"), false);
            return new OwnershipCraftingRecipe(recipeId, category, ingredient);
        }

        @Override
        public OwnershipCraftingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            return new OwnershipCraftingRecipe(recipeId, category, ingredient);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, OwnershipCraftingRecipe recipe) {
            buffer.writeEnum(recipe.category());
            recipe.base.toNetwork(buffer);
        }
    }
}
