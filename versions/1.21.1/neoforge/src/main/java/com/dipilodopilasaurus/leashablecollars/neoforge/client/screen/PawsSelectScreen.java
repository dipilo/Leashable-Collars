package com.dipilodopilasaurus.leashablecollars.neoforge.client.screen;

import com.dipilodopilasaurus.leashablecollars.neoforge.network.OpenPawsConfigPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

public class PawsSelectScreen extends Screen {
    private final UUID targetUuid;

    public PawsSelectScreen(UUID targetUuid, String targetName) {
        super(Component.translatable("gui.playercollars.paw_configurator.title", targetName));
        this.targetUuid = targetUuid;
    }

    @Override
    protected void init() {
        int x = this.width / 2;
        int y = this.height / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.playercollars.paw_configurator.block.open"), button -> {
            PacketDistributor.sendToServer(new OpenPawsConfigPayload(targetUuid, false));
            onClose();
        }).bounds(x - 80, y, 160, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.playercollars.paw_configurator.item.open"), button -> {
            PacketDistributor.sendToServer(new OpenPawsConfigPayload(targetUuid, true));
            onClose();
        }).bounds(x - 80, y + 22, 160, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 20, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
