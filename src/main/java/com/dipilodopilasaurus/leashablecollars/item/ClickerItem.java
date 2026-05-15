package com.dipilodopilasaurus.leashablecollars.item;

import com.dipilodopilasaurus.leashablecollars.LeashableCollars;
import com.dipilodopilasaurus.leashablecollars.PacketLookAtLerped;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ClickerItem extends Item implements DyeableLeatherItem {
    private static final String CLICKER_TAG = "playercollars_clicker";
    private static final String TURN_TAG = "force_turn";

    public ClickerItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static boolean shouldForceTurn(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(CLICKER_TAG);
        return tag == null || !tag.contains(TURN_TAG) || tag.getBoolean(TURN_TAG);
    }

    private static void setForceTurn(ItemStack stack, boolean enabled) {
        stack.getOrCreateTagElement(CLICKER_TAG).putBoolean(TURN_TAG, enabled);
    }

    private static void toggleForceTurn(Player player, ItemStack stack) {
        boolean enabled = !shouldForceTurn(stack);
        setForceTurn(stack, enabled);
        player.displayClientMessage(Component.translatable(enabled
                ? "item.playercollars.clicker.turn_enable"
                : "item.playercollars.clicker.turn_disable"), true);
    }

    private static int getClickerDistance(ItemStack stack) {
        return 4 << stack.getEnchantmentLevel(LeashableCollars.CLICKER_ENCHANTMENT.get());
    }

    private static void forceTurnNearbyPlayers(ServerLevel level, Player player, int distance) {
        List<ServerPlayer> nearbyPlayers = level.getPlayers(other -> !other.is(player) && other.closerThan(player, distance));
        for (ServerPlayer other : nearbyPlayers) {
            sendTurnPacketIfOwned(player, other);
        }
    }

    private static void sendTurnPacketIfOwned(Player owner, ServerPlayer target) {
        CuriosApi.getCuriosInventory(target).ifPresent(handler -> handler.getStacksHandler("necklace").ifPresent(slot -> {
            ItemStack collarStack = LeashableCollars.filterStacksByOwner(slot.getStacks(), owner.getUUID());
            if (collarStack == null) {
                collarStack = LeashableCollars.filterStacksByOwner(slot.getCosmeticStacks(), owner.getUUID());
            }
            if (collarStack != null) {
                LeashableCollars.NETWORK.send(PacketDistributor.PLAYER.with(() -> target), new PacketLookAtLerped(owner));
            }
        }));
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment == LeashableCollars.CLICKER_ENCHANTMENT.get();
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 40;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isCrouching()) {
            if (!level.isClientSide) {
                toggleForceTurn(player, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        player.startUsingItem(hand);
        if (!level.isClientSide) {
            int distance = getClickerDistance(stack);
            if (shouldForceTurn(stack)) {
                forceTurnNearbyPlayers((ServerLevel) level, player, distance);
            }
            level.playSound(null, player, LeashableCollars.CLICKER_ON.get(), SoundSource.PLAYERS, 1, 1);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!level.isClientSide) {
            level.playSound(null, entity, LeashableCollars.CLICKER_OFF.get(), SoundSource.PLAYERS, 1, 1);
        }
    }

    @Override
    public int getColor(ItemStack stack) {
        CompoundTag displayTag = stack.getTagElement("display");
        return displayTag != null && displayTag.contains("color", 99) ? displayTag.getInt("color") : 0xFFFFFF;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (shouldForceTurn(stack)) {
            tooltip.add(Component.translatable("item.playercollars.clicker.turn"));
        }
    }
}
