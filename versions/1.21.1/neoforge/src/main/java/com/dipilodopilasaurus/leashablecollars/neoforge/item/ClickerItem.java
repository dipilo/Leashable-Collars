package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.List;

public class ClickerItem extends Item {
    private static final int CLICKER_DISTANCE = 4;

    public ClickerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 45;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return isEnchantment(enchantment, "playercollars", "clicker");
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return supportsEnchantment(stack, enchantment);
    }

    private static boolean isEnchantment(Holder<Enchantment> enchantment, String namespace, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        return enchantment.unwrapKey().map(key -> key.location().equals(id)).orElse(false);
    }

    private static boolean shouldForceTurn(ItemStack stack) {
        return !stack.has(DataComponents.INTANGIBLE_PROJECTILE);
    }

    private static int getClickerDistance(Level level, ItemStack stack) {
        return CLICKER_DISTANCE << LeashableCollarsNeoForge.getEnchantmentLevel(level, stack, LeashableCollarsNeoForge.CLICKER_ENCHANTMENT);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                if (shouldForceTurn(stack)) {
                    stack.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
                    player.displayClientMessage(Component.translatable("item.playercollars.clicker.turn_disable"), true);
                } else {
                    stack.remove(DataComponents.INTANGIBLE_PROJECTILE);
                    player.displayClientMessage(Component.translatable("item.playercollars.clicker.turn_enable"), true);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        player.startUsingItem(hand);
        if (!level.isClientSide()) {
            if (shouldForceTurn(stack)) {
                forceTurnNearbyPlayers((ServerLevel) level, player, getClickerDistance(level, stack));
            }
            level.playSound(null, player, LeashableCollarsNeoForge.CLICKER_ON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResultHolder.fail(stack);
    }

    private static void forceTurnNearbyPlayers(ServerLevel level, Player owner, int distance) {
        for (ServerPlayer other : level.players()) {
            if (other == owner || !other.closerThan(owner, distance)) {
                continue;
            }
            if (LeashableCollarsNeoForge.findOwnedCollar(other, owner.getUUID()) != null) {
                other.lookAt(EntityAnchorArgument.Anchor.EYES, owner.getEyePosition());
                other.connection.send(new ClientboundPlayerLookAtPacket(EntityAnchorArgument.Anchor.EYES, owner.getX(), owner.getEyeY(), owner.getZ()));
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return Integer.MAX_VALUE;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!level.isClientSide()) {
            level.playSound(null, entity, LeashableCollarsNeoForge.CLICKER_OFF.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (shouldForceTurn(stack)) {
            tooltip.add(Component.translatable("item.playercollars.clicker.turn"));
        }
    }
}