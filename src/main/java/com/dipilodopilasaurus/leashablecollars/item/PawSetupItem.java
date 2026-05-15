package com.dipilodopilasaurus.leashablecollars.item;

import com.dipilodopilasaurus.leashablecollars.LeashableCollars;
import com.dipilodopilasaurus.leashablecollars.client.screen.PawsSelectScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Collections;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class PawSetupItem extends Item {
    public PawSetupItem() {
        super(new Properties().stacksTo(1).craftRemainder(Items.STICK));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, Player user, InteractionHand usedHand) {
        ItemStack stack = user.getItemInHand(usedHand);
        if (!user.isCrouching()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return new InteractionResultHolder<>(openSelectionClient(user, user), stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity interactionTarget, net.minecraft.world.InteractionHand usedHand) {
        if (!(interactionTarget instanceof Player target)) {
            return InteractionResult.PASS;
        }

        if (user.level().isClientSide) {
            return openSelectionClient(user, target);
        }

        return InteractionResult.SUCCESS;
    }

    @OnlyIn(Dist.CLIENT)
    private InteractionResult openSelectionClient(Player user, Player target) {
        for (SlotResult slotResult : CuriosApi.getCuriosInventory(target).map(handler -> handler.findCurios("hands")).orElse(Collections.emptyList())) {
            if (slotResult.stack().getItem() instanceof PawsItem) {
                Minecraft.getInstance().setScreen(new PawsSelectScreen(target.getUUID(), target.getName().getString()));
                return InteractionResult.SUCCESS;
            }
        }

        user.displayClientMessage(Component.translatable("item.playercollars.paw_configurator.no_paws_found").withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
    }
}
