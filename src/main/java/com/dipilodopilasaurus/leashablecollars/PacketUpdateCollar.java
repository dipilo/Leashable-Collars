package com.dipilodopilasaurus.leashablecollars;

import com.dipilodopilasaurus.leashablecollars.item.CollarItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketUpdateCollar {
    private final int pawColor;
    private final int color;
    private final OwnerState ownerState;

    public PacketUpdateCollar(ItemStack stack, OwnerState ownerState) {
        CollarItem item = LeashableCollars.COLLAR_ITEM.get();
        this.pawColor = item.getPawColor(stack);
        this.color = item.getColor(stack);
        this.ownerState = ownerState;
    }

    public PacketUpdateCollar(FriendlyByteBuf buffer) {
        this.color = buffer.readInt();
        this.pawColor = buffer.readInt();
        this.ownerState = buffer.readEnum(OwnerState.class);
    }

    public enum OwnerState {
        NOP,
        DEL,
        ADD
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.color);
        buffer.writeInt(this.pawColor);
        buffer.writeEnum(this.ownerState);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Player player = context.get().getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty() && stack.getItem() instanceof CollarItem item) {
                item.setColor(stack, color);
                item.setPawColor(stack, pawColor);
                if (ownerState == OwnerState.DEL) {
                    item.setOwner(stack, null, null);
                } else if (ownerState == OwnerState.ADD) {
                    item.setOwner(stack, player.getUUID(), player.getName().getString());
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
