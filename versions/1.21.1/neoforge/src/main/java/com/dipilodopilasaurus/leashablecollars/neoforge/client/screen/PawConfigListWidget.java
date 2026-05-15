package com.dipilodopilasaurus.leashablecollars.neoforge.client.screen;

import com.dipilodopilasaurus.leashablecollars.neoforge.PawConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;

import java.util.List;
import java.util.function.IntConsumer;

public class PawConfigListWidget extends ObjectSelectionList<PawConfigListWidget.Entry> {
    private final boolean heldItems;
    private final IntConsumer clickHandler;
    private int left;

    public PawConfigListWidget(int width, int height, int y, int itemHeight, boolean heldItems, IntConsumer clickHandler) {
        super(Minecraft.getInstance(), width, height, y, itemHeight);
        this.heldItems = heldItems;
        this.clickHandler = clickHandler;
    }

    public void setLeftPos(int left) {
        this.left = left;
        this.setX(left);
    }

    public void setEntries(List<PawConfigEntry> entries) {
        clearEntries();
        for (int i = 0; i < entries.size(); i++) {
            addEntry(new Entry(i, entries.get(i)));
        }
        setScrollAmount(0.0D);
    }

    @Override
    public int getRowWidth() {
        return this.width - 5;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.left + this.getRowWidth();
    }

    @Override
    public int getRowLeft() {
        return this.left + 2;
    }

    public class Entry extends ObjectSelectionList.Entry<Entry> {
        private final int index;
        private final PawConfigEntry configEntry;

        public Entry(int index, PawConfigEntry configEntry) {
            this.index = index;
            this.configEntry = configEntry;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (hovered) {
                guiGraphics.fill(left, top - 2, left + width, top + height, 0x8F999999);
            }
            guiGraphics.drawString(Minecraft.getInstance().font, configEntry.getDisplayName(heldItems), left, top - 1, 0xFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            clickHandler.accept(index);
            return true;
        }

        @Override
        public net.minecraft.network.chat.Component getNarration() {
            return configEntry.getDisplayName(heldItems);
        }
    }
}
