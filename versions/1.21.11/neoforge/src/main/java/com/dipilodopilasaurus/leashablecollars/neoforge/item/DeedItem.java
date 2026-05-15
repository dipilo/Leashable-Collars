package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.OwnerData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DeedItem extends Item {
    public DeedItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && LeashableCollarsNeoForge.getOwnerData(stack) == null) {
            LeashableCollarsNeoForge.setOwnerData(stack, new OwnerData(player.getUUID(), player.getName().getString()));
            player.displayClientMessage(Component.translatable("item.playercollars.deed_of_ownership.filled_out"), true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!(interactionTarget instanceof Player target) || user.level().isClientSide()) {
            return InteractionResult.PASS;
        }

        OwnerData ownerData = LeashableCollarsNeoForge.getOwnerData(stack);
        if (ownerData == null || ownerData.owned().isPresent()) {
            return InteractionResult.PASS;
        }
        if (ownerData.uuid().equals(target.getUUID())) {
            user.displayClientMessage(Component.translatable("item.playercollars.deed_of_ownership.no_self_own"), true);
            return InteractionResult.FAIL;
        }

        ItemStack stamped = new ItemStack(LeashableCollarsNeoForge.STAMPED_DEED_OF_OWNERSHIP.get());
        LeashableCollarsNeoForge.setOwnerData(stamped, ownerData.withOwned(target.getUUID(), target.getName().getString()));
        user.setItemInHand(usedHand, stamped);
        user.displayClientMessage(Component.translatable("item.playercollars.deed_of_ownership.stamped", target.getName()), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        return LeashableCollarsNeoForge.getOwnerData(stack) != null
                ? Component.translatable("item.playercollars.deed_of_ownership.filled")
                : super.getName(stack);
    }
}