package com.dipilodopilasaurus.leashablecollars.neoforge.client.screen;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class PawsConfigScreen extends AbstractContainerScreen<PawsConfigMenu> {
    private static final ResourceLocation TEXTURE = LeashableCollarsNeoForge.id("textures/gui/paw_controller.png");
    private static final ResourceLocation WIDGETS_TEXTURE = LeashableCollarsNeoForge.id("textures/gui/paw_controller_widgets.png");

    private PawConfigListWidget listWidget;
    private ItemStack lastGhostStack = ItemStack.EMPTY;

    public PawsConfigScreen(PawsConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        this.imageWidth = 174;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
        super.init();

        this.listWidget = new PawConfigListWidget(160, 106, this.topPos + 18, this.font.lineHeight, this.menu.isHeldItems(), this::handleEntryClick);
        this.listWidget.setLeftPos(this.leftPos + 7);
        this.listWidget.setEntries(this.menu.getListToDisplay());
        addRenderableWidget(this.listWidget);
    }

    private void handleEntryClick(int id) {
        this.menu.applyButton(id);
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
        if (!this.menu.getGhostStack().isEmpty()) {
            this.menu.setGhostStack(ItemStack.EMPTY);
        }
        this.listWidget.setEntries(this.menu.getListToDisplay());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!ItemStack.matches(this.menu.getGhostStack(), this.lastGhostStack)) {
            this.lastGhostStack = this.menu.getGhostStack().copy();
            this.listWidget.setEntries(this.menu.getListToDisplay());
        }
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth - 50) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth + 50, this.imageHeight, 256, 256);
        guiGraphics.blit(WIDGETS_TEXTURE, x + 7, y + 108, this.menu.getGhostStack().isEmpty() ? 16 : 0, 0, 16, 16, 32, 16);
    }
}
