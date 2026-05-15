package com.dipilodopilasaurus.leashablecollars.item;

import com.dipilodopilasaurus.leashablecollars.OwnerData;
import com.dipilodopilasaurus.leashablecollars.client.CollarDyeScreen;
import com.dipilodopilasaurus.leashablecollars.LeashableCollars;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CollarItem extends Item implements DyeableLeatherItem, ICurio, ICapabilityProvider {
    private static final String DISPLAY_TAG = "display";
    private static final String OWNER_TAG = "owner";
    public final boolean tagless;

    public CollarItem() {
        this(false);
    }

    public CollarItem(boolean tagless) {
        super(new Item.Properties().stacksTo(1));
        this.tagless = tagless;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 40;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment == Enchantments.BINDING_CURSE
            || enchantment == LeashableCollars.SHORT_LEASH_ENCHANTMENT.get()
            || enchantment == LeashableCollars.REGENERATION_ENCHANTMENT.get()
            || enchantment == LeashableCollars.THORNS_ENCHANTMENT.get();
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
    public void curioTick(SlotContext slotContext) {
        LivingEntity entity = slotContext.entity();
        if (entity.level().isClientSide) {
            return;
        }

        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> handler.findCurio(slotContext.identifier(), slotContext.index()).ifPresent(slotResult -> {
            int regenerationLevel = this.getEnchantmentLevel(slotResult.stack(), LeashableCollars.REGENERATION_ENCHANTMENT.get());
            if (regenerationLevel == 0) {
                return;
            }
            Pair<UUID, String> owner = this.getOwner(slotResult.stack());
            if (owner == null || owner.getFirst().equals(entity.getUUID())) {
                return;
            }
            Player ownerPlayer = entity.level().getPlayerByUUID(owner.getFirst());
            if (ownerPlayer != null && ownerPlayer.distanceTo(entity) < 16) {
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, regenerationLevel - 1, false, false, false));
            }
        }));
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
        CompoundTag displayTag = stack.getTagElement(DISPLAY_TAG);
        return displayTag != null && displayTag.contains("color", 99) ? displayTag.getInt("color") : MapColor.COLOR_RED.col;
    }

    public int getPawColor(ItemStack stack) {
        CompoundTag displayTag = stack.getTagElement(DISPLAY_TAG);
        return displayTag != null && displayTag.contains("paw", 99) ? displayTag.getInt("paw") : MapColor.COLOR_BLUE.col;
    }

    public void setPawColor(ItemStack stack, int color) {
        CompoundTag displayTag = stack.getOrCreateTagElement(DISPLAY_TAG);
        displayTag.putInt("paw", color);
    }

    public @Nullable Pair<UUID, String> getOwner(ItemStack stack) {
        OwnerData ownerData = getOwnerData(stack);
        if (ownerData == null) {
            return null;
        }
        return new Pair<>(ownerData.uuid(), ownerData.name());
    }

    public void setOwner(ItemStack stack, @Nullable UUID uuid, @Nullable String name) {
        if (uuid == null || name == null) {
            setOwnerData(stack, null);
            return;
        }
        setOwnerData(stack, new OwnerData(uuid, name));
    }

    public static @Nullable OwnerData getOwnerData(ItemStack stack) {
        CompoundTag ownerTag = stack.getTagElement(OWNER_TAG);
        return OwnerData.fromTag(ownerTag);
    }

    public static void setOwnerData(ItemStack stack, @Nullable OwnerData ownerData) {
        if (ownerData == null) {
            stack.removeTagKey(OWNER_TAG);
            return;
        }
        stack.addTagElement(OWNER_TAG, ownerData.toTag());
    }

    public void setOwnedTarget(ItemStack stack, @Nullable UUID ownedUuid, @Nullable String ownedName) {
        OwnerData ownerData = getOwnerData(stack);
        if (ownerData == null) {
            return;
        }
        if (ownedUuid == null || ownedName == null) {
            setOwnerData(stack, new OwnerData(ownerData.uuid(), ownerData.name()));
            return;
        }
        setOwnerData(stack, new OwnerData(ownerData.uuid(), ownerData.name(), java.util.Optional.of(ownedUuid), java.util.Optional.of(ownedName)));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
        if (result.getResult() == InteractionResult.PASS && player.isCrouching() && level.isClientSide) {
            Minecraft.getInstance().setScreen(new CollarDyeScreen(result.getObject(), player.getUUID()));
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, result.getObject());
        }
        return result;
    }

    @Override
    public Component getName(ItemStack stack) {
        OwnerData ownerData = getOwnerData(stack);
        if (ownerData != null && ownerData.ownedName().isPresent()) {
            return Component.translatable("item.playercollars.collar.named", ownerData.ownedName().get());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, tooltip, tooltipFlag);
        if (tooltipFlag.isAdvanced() && !tagless) {
            tooltip.add(Component.translatable("item.playercollars.collar.paw_color", Integer.toHexString(getPawColor(stack))).withStyle(ChatFormatting.GRAY));
        }
        OwnerData ownerData = getOwnerData(stack);
        if (ownerData != null) {
            tooltip.add(Component.translatable("item.playercollars.collar.owner", ownerData.name()).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
}
