package com.dipilodopilasaurus.leashablecollars;

import com.dipilodopilasaurus.leashablecollars.item.CollarItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class PacketStampDeed {
    public PacketStampDeed() {
    }

    public PacketStampDeed(FriendlyByteBuf buffer) {
        // This packet is a marker packet with no payload.
    }

    public void encode(FriendlyByteBuf buffer) {
        // This packet is a marker packet with no payload.
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = player.getMainHandItem();
            if (!stack.is(LeashableCollars.DEED_OF_OWNERSHIP.get())) {
                return;
            }

            OwnerData ownerData = CollarItem.getOwnerData(stack);
            if (ownerData == null || ownerData.owned().isPresent()) {
                return;
            }
            if (ownerData.uuid().equals(player.getUUID())) {
                player.displayClientMessage(Component.translatable("item.playercollars.deed_of_ownership.no_self_own"), true);
                return;
            }

            ItemStack stamped = new ItemStack(LeashableCollars.STAMPED_DEED_OF_OWNERSHIP.get());
            CollarItem.setOwnerData(stamped, new OwnerData(ownerData.uuid(), ownerData.name(), Optional.of(player.getUUID()), Optional.of(player.getName().getString())));
            player.getInventory().setItem(player.getInventory().selected, stamped);
            player.displayClientMessage(Component.translatable("item.playercollars.deed_of_ownership.stamped", player.getName()), true);
        });
        context.get().setPacketHandled(true);
    }
}