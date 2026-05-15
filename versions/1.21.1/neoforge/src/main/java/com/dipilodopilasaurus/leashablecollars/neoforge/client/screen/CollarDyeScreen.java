package com.dipilodopilasaurus.leashablecollars.neoforge.client.screen;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.OwnerData;
import com.dipilodopilasaurus.leashablecollars.neoforge.network.UpdateCollarPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.MapItemColor;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class CollarDyeScreen extends Screen {
    private final ItemStack stack;
    private final int initialColor;
    private final int initialPawColor;
    private final UUID selfUuid;
    private UUID ownerUuid;
    private final String ownerName;

    public CollarDyeScreen(ItemStack stack, UUID selfUuid) {
        super(stack.getDisplayName());
        this.stack = stack;
        this.selfUuid = selfUuid;
        this.initialColor = getDyedColor(stack, DyeColor.RED.getTextureDiffuseColor()) & 0xFFFFFF;
        this.initialPawColor = getPawColor(stack, DyeColor.BLUE.getTextureDiffuseColor()) & 0xFFFFFF;
        OwnerData ownerData = LeashableCollarsNeoForge.getOwnerData(stack);
        this.ownerUuid = ownerData == null ? null : ownerData.uuid();
        this.ownerName = ownerData == null ? null : ownerData.name();
    }

    @Override
    protected void init() {
        int x = this.width / 2;
        int y = this.height / 2 - 30;

        EditBox dyeField = new EditBox(this.font, x - 30, y, 100, 20, Component.empty());
        dyeField.setMaxLength(6);
        dyeField.setResponder(value -> updateTextField(0, value));
        dyeField.setFilter(CollarDyeScreen::isValidHex);
        dyeField.setValue(Integer.toHexString(initialColor & 0xFFFFFF));

        EditBox pawField = new EditBox(this.font, x - 30, y + 25, 100, 20, Component.empty());
        pawField.setMaxLength(6);
        pawField.setResponder(value -> updateTextField(1, value));
        pawField.setFilter(CollarDyeScreen::isValidHex);
        pawField.setValue(Integer.toHexString(initialPawColor & 0xFFFFFF));

        this.addRenderableWidget(dyeField);
        this.addRenderableWidget(pawField);
        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
            UpdateCollarPayload.OwnerState ownerState;
            if (ownerUuid == null) {
                ownerState = UpdateCollarPayload.OwnerState.DEL;
            } else if (ownerUuid.equals(selfUuid)) {
                ownerState = UpdateCollarPayload.OwnerState.ADD;
            } else {
                ownerState = UpdateCollarPayload.OwnerState.NOP;
            }
            PacketDistributor.sendToServer(new UpdateCollarPayload(getDyedColor(stack, initialColor), getPawColor(stack, initialPawColor), ownerState));
            this.minecraft.setScreen(null);
        }).bounds(x + 5, y + 50, 75, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
            setDyedColor(stack, initialColor);
            setPawColor(stack, initialPawColor);
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
        if (value.isEmpty()) {
            return true;
        }
        try {
            Integer.parseInt(value, 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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
        if (value.isEmpty()) {
            return;
        }

        int color;
        try {
            color = Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return;
        }

        if (field == 0) {
            setDyedColor(stack, color);
        } else {
            setPawColor(stack, color);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawString(font, Component.translatable("item.playercollars.collar"), this.width / 2 - 75, this.height / 2 - 25, -1);
        guiGraphics.drawString(font, Component.translatable("item.playercollars.collar.paw"), this.width / 2 - 75, this.height / 2 + 1, -1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int getDyedColor(ItemStack stack, int fallback) {
        DyedItemColor color = stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
        return (color == null ? fallback : color.rgb()) & 0xFFFFFF;
    }

    private static int getPawColor(ItemStack stack, int fallback) {
        MapItemColor color = stack.get(net.minecraft.core.component.DataComponents.MAP_COLOR);
        return (color == null ? fallback : color.rgb()) & 0xFFFFFF;
    }

    private static void setDyedColor(ItemStack stack, int rgb) {
        stack.set(net.minecraft.core.component.DataComponents.DYED_COLOR, new DyedItemColor(rgb & 0xFFFFFF, true));
    }

    private static void setPawColor(ItemStack stack, int rgb) {
        stack.set(net.minecraft.core.component.DataComponents.MAP_COLOR, new MapItemColor(rgb & 0xFFFFFF));
    }
}
