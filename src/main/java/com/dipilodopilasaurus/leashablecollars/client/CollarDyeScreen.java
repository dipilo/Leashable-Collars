package com.dipilodopilasaurus.leashablecollars.client;

import com.dipilodopilasaurus.leashablecollars.LeashableCollars;
import com.dipilodopilasaurus.leashablecollars.PacketUpdateCollar;
import com.dipilodopilasaurus.leashablecollars.item.CollarItem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class CollarDyeScreen extends Screen {
    private final ItemStack stack;
    private final CollarItem item;
    private final int initialColor;
    private final int initialPawColor;
    private final UUID selfUuid;
    private UUID ownerUuid;
    private final String ownerName;

    public CollarDyeScreen(ItemStack stack, UUID selfUuid) {
        super(stack.getDisplayName());
        this.stack = stack;
        this.item = LeashableCollars.COLLAR_ITEM.get();
        this.selfUuid = selfUuid;
        this.initialColor = item.getColor(stack);
        this.initialPawColor = item.getPawColor(stack);
        Pair<UUID, String> owner = item.getOwner(stack);
        this.ownerUuid = owner == null ? null : owner.getFirst();
        this.ownerName = owner == null ? null : owner.getSecond();
    }

    @Override
    protected void init() {
        int x = this.width / 2;
        int y = this.height / 2 - 30;

        EditBox dyeField = new EditBox(this.font, x - 30, y, 100, 20, Component.empty());
        dyeField.setMaxLength(6);
        dyeField.setResponder(value -> updateTextField(0, value));
        dyeField.setFilter(value -> isValidHex(value));
        dyeField.setValue(Integer.toHexString(initialColor));

        EditBox pawField = new EditBox(this.font, x - 30, y + 25, 100, 20, Component.empty());
        pawField.setMaxLength(6);
        pawField.setResponder(value -> updateTextField(1, value));
        pawField.setFilter(value -> isValidHex(value));
        pawField.setValue(Integer.toHexString(initialPawColor));

        this.addRenderableWidget(dyeField);
        this.addRenderableWidget(pawField);
        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
            PacketUpdateCollar.OwnerState ownerState;
            if (ownerUuid == null) {
                ownerState = PacketUpdateCollar.OwnerState.DEL;
            } else if (ownerUuid.equals(selfUuid)) {
                ownerState = PacketUpdateCollar.OwnerState.ADD;
            } else {
                ownerState = PacketUpdateCollar.OwnerState.NOP;
            }
            LeashableCollars.NETWORK.sendToServer(new PacketUpdateCollar(stack, ownerState));
            this.minecraft.setScreen(null);
        }).bounds(x + 5, y + 50, 75, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
            item.setColor(stack, initialColor);
            item.setPawColor(stack, initialPawColor);
            this.minecraft.setScreen(null);
        }).bounds(x - 80, y + 50, 75, 20).build());

        Button ownerButton = Button.builder(Component.empty(), this::updateOwner).bounds(x - 80, y + 72, 160, 20).build();
        if (ownerUuid == null) {
            ownerButton.setMessage(Component.translatable("item.playercollars.collar.become_owner"));
        } else if (ownerUuid.equals(selfUuid)) {
            ownerButton.setMessage(Component.translatable("item.playercollars.collar.remove_owner"));
        } else {
            ownerButton.setMessage(Component.translatable("item.playercollars.collar.owner", ownerName));
            ownerButton.active = false;
        }
        this.addRenderableWidget(ownerButton);
    }

    private static boolean isValidHex(String value) {
        try {
            Integer.parseInt(value, 16);
        } catch (NumberFormatException e) {
            return value.isEmpty();
        }
        return true;
    }

    private void updateOwner(Button button) {
        if (ownerUuid == null) {
            ownerUuid = selfUuid;
            button.setMessage(Component.translatable("item.playercollars.collar.remove_owner"));
        } else {
            ownerUuid = null;
            button.setMessage(Component.translatable("item.playercollars.collar.become_owner"));
        }
    }

    private void updateTextField(int field, String value) {
        int color;
        try {
            color = Integer.parseInt(value, 16);
        } catch (NumberFormatException e) {
            return;
        }

        if (field == 0) {
            item.setColor(stack, color);
        } else {
            item.setPawColor(stack, color);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawString(font, Component.translatable("item.playercollars.collar"), this.width / 2 - 75, this.height / 2 - 25, -1);
        guiGraphics.drawString(font, Component.translatable("item.playercollars.collar.paw"), this.width / 2 - 75, this.height / 2 + 1, -1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
