package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.OwnerData;
import com.dipilodopilasaurus.leashablecollars.neoforge.client.ClientScreenHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class CollarItem extends Item implements ICurioItem {
    private static final String MOD_NAMESPACE = LeashableCollarsNeoForge.MOD_ID;
    private final boolean tagless;

    public CollarItem(Properties properties, boolean tagless) {
        super(properties.stacksTo(1)
            .component(DataComponents.DYED_COLOR, new DyedItemColor(MapColor.COLOR_RED.col, true))
                .component(DataComponents.MAP_COLOR, new MapItemColor(MapColor.COLOR_BLUE.col)));
        this.tagless = tagless;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 100;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return isEnchantment(enchantment, "minecraft", "binding_curse")
                || isEnchantment(enchantment, MOD_NAMESPACE, "short_leash")
                || isEnchantment(enchantment, MOD_NAMESPACE, "regeneration")
                || isEnchantment(enchantment, MOD_NAMESPACE, "thorns");
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    private static boolean isEnchantment(Holder<Enchantment> enchantment, String namespace, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        return enchantment.unwrapKey().map(key -> key.location().equals(id)).orElse(false);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            return !player.isSecondaryUseActive();
        }
        return !slotContext.entity().isCrouching();
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
    }

    @Override
    public ICurio.DropRule getDropRule(SlotContext slotContext, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return !LeashableCollarsNeoForge.isLocked(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        OwnerData ownerData = LeashableCollarsNeoForge.getOwnerData(stack);
        if (ownerData != null && ownerData.ownedName().isPresent()) {
            return Component.translatable("item.playercollars.collar.named", ownerData.ownedName().get());
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (flag.isAdvanced() && !tagless) {
            MapItemColor pawColor = stack.get(DataComponents.MAP_COLOR);
            int pawRgb = pawColor != null ? pawColor.rgb() : DyeColor.BLUE.getMapColor().col;
            tooltip.add(Component.translatable("item.playercollars.collar.paw_color", Integer.toHexString(pawRgb)).withStyle(ChatFormatting.GRAY));
        }

        OwnerData ownerData = LeashableCollarsNeoForge.getOwnerData(stack);
        if (ownerData != null) {
            tooltip.add(Component.translatable("item.playercollars.collar.owner", ownerData.name()).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (level.isClientSide()) {
                ClientScreenHooks.openCollarDyeScreen(stack, player.getUUID());
            }
            return InteractionResultHolder.success(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!(interactionTarget instanceof Player target) || user.level().isClientSide()) {
            return InteractionResult.PASS;
        }

        return CuriosApi.getCuriosInventory(target).flatMap(handler -> handler.getStacksHandler("necklace")).map(slotHandler -> {
            for (int slot = 0; slot < slotHandler.getStacks().getSlots(); slot++) {
                if (!slotHandler.getStacks().getStackInSlot(slot).isEmpty()) {
                    continue;
                }
                ItemStack equipped = stack.copyWithCount(1);
                if (!this.tagless) {
                    LeashableCollarsNeoForge.setOwnerData(equipped, new OwnerData(user.getUUID(), user.getName().getString()).withOwned(target.getUUID(), target.getName().getString()));
                }
                slotHandler.getStacks().setStackInSlot(slot, equipped);
                if (!user.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }).orElse(InteractionResult.FAIL);
    }
}