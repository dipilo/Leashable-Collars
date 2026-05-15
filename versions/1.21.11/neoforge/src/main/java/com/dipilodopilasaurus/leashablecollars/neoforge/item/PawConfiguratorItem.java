package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.ArrayList;
import java.util.List;

public class PawConfiguratorItem extends Item {
    public PawConfiguratorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isCrouching()) {
            return super.use(level, player, hand);
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            boolean strict = !isStrictMode(stack);
            setStrictMode(stack, strict);
            player.displayClientMessage(Component.translatable(strict
                    ? "item.playercollars.paw_configurator.mode_strict"
                    : "item.playercollars.paw_configurator.mode_relaxed"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState blockState = context.getLevel().getBlockState(context.getClickedPos());
        if (blockState.is(LeashableCollarsNeoForge.PAWS_ALLOW_INTERACT)) {
            if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.translatable(
                        "item.playercollars.paw_configurator.no_remove",
                        blockState.getBlock().getName()), true);
            }
            return InteractionResult.PASS;
        }

        String blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
        ItemStack stack = context.getItemInHand();
        List<String> allowedBlocks = stack.get(LeashableCollarsNeoForge.PAWS_ALLOWED_BLOCKS.get());
        List<String> updated = allowedBlocks == null ? new ArrayList<>() : new ArrayList<>(allowedBlocks);
        boolean removed = updated.remove(blockId);
        if (!removed) {
            updated.add(blockId);
        }
        stack.set(LeashableCollarsNeoForge.PAWS_ALLOWED_BLOCKS.get(), List.copyOf(updated));

        if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(Component.translatable(
                    removed ? "item.playercollars.paw_configurator.removed" : "item.playercollars.paw_configurator.added",
                    blockState.getBlock().getName()), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!user.isCrouching() || !(interactionTarget instanceof Player target) || user.level().isClientSide()) {
            return InteractionResult.PASS;
        }

        if (LeashableCollarsNeoForge.findOwnedCollar(target, user.getUUID()) == null) {
            user.displayClientMessage(Component.translatable("item.playercollars.paw_configurator.no_set_non_owner"), true);
            return InteractionResult.FAIL;
        }

        List<SlotResult> handPaws = CuriosApi.getCuriosInventory(target)
                .map(handler -> handler.findCurios("hands"))
                .orElse(List.of());

        int updated = 0;
        for (SlotResult slotResult : handPaws) {
            if (!(slotResult.stack().getItem() instanceof PawsItem)) {
                continue;
            }
            PawsItem.copyConfiguration(stack, slotResult.stack());
            updated++;
        }

        if (updated == 0) {
            user.displayClientMessage(Component.translatable("item.playercollars.paw_configurator.no_paws_found"), true);
            return InteractionResult.FAIL;
        }

        target.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
        user.displayClientMessage(Component.translatable("item.playercollars.paw_configurator.success"), true);
        return InteractionResult.SUCCESS;
    }

    private static boolean isStrictMode(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(LeashableCollarsNeoForge.PAWS_STRICT_MODE.get()));
    }

    private static void setStrictMode(ItemStack stack, boolean strict) {
        if (strict) {
            stack.set(LeashableCollarsNeoForge.PAWS_STRICT_MODE.get(), true);
        } else {
            stack.remove(LeashableCollarsNeoForge.PAWS_STRICT_MODE.get());
        }
    }
}