package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.client.ClientScreenHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Collections;

public class PawConfiguratorItem extends Item {
    public PawConfiguratorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isCrouching()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide()) {
            return new InteractionResultHolder<>(openSelectionClient(player, player), stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!(interactionTarget instanceof Player target)) {
            return InteractionResult.PASS;
        }

        if (user.level().isClientSide()) {
            return openSelectionClient(user, target);
        }

        return InteractionResult.SUCCESS;
    }

    private InteractionResult openSelectionClient(Player user, Player target) {
        for (SlotResult slotResult : CuriosApi.getCuriosInventory(target).map(handler -> handler.findCurios("hands")).orElse(Collections.emptyList())) {
            if (slotResult.stack().getItem() instanceof PawsItem) {
                ClientScreenHooks.openPawsSelectScreen(target.getUUID(), target.getName().getString());
                return InteractionResult.SUCCESS;
            }
        }
        user.displayClientMessage(Component.translatable("item.playercollars.paw_configurator.no_paws_found"), true);
        return InteractionResult.FAIL;
    }
}