package com.dipilodopilasaurus.leashablecollars.client.screen;

import com.dipilodopilasaurus.leashablecollars.LeashableCollars;
import com.dipilodopilasaurus.leashablecollars.PawConfigEntry;
import com.dipilodopilasaurus.leashablecollars.item.PawsItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraft.world.MenuProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PawsConfigMenu extends AbstractContainerMenu {
    private final Container ghostInventory;
    private final boolean heldItems;
    private final List<PawConfigEntry> data;
    private List<PawConfigEntry> listToDisplay;
    private final List<ItemStack> pawStacks;

    public PawsConfigMenu(int containerId, Inventory playerInventory, boolean heldItems, List<PawConfigEntry> data, List<ItemStack> pawStacks) {
        super(LeashableCollars.PAWS_CONFIG_MENU.get(), containerId);
        this.heldItems = heldItems;
        this.data = new ArrayList<>(data);
        this.listToDisplay = new ArrayList<>(data);
        this.pawStacks = pawStacks;
        this.ghostInventory = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                PawsConfigMenu.this.slotsChanged(this);
            }
        };

        addSlot(new Slot(this.ghostInventory, 0, 175, 108));

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 7 + col * 18, 140 + row * 18));
            }
        }

        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            addSlot(new Slot(playerInventory, hotbar, 7 + hotbar * 18, 198));
        }
    }

    public static PawsConfigMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        return new PawsConfigMenu(containerId, playerInventory, buffer.readBoolean(), PawConfigEntry.readList(buffer), List.of());
    }

    public boolean isHeldItems() {
        return heldItems;
    }

    public List<PawConfigEntry> getListToDisplay() {
        return listToDisplay;
    }

    public void setGhostStack(ItemStack stack) {
        this.ghostInventory.setItem(0, stack);
        refreshDisplayedList();
    }

    public ItemStack getGhostStack() {
        return this.ghostInventory.getItem(0);
    }

    public void refreshDisplayedList() {
        ItemStack ghostStack = getGhostStack();
        this.listToDisplay = ghostStack.isEmpty() ? new ArrayList<>(data) : createEntriesForStack(ghostStack);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == this.ghostInventory) {
            refreshDisplayedList();
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == 0) {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                setGhostStack(ItemStack.EMPTY);
            } else {
                ItemStack copy = carried.copy();
                copy.setCount(1);
                setGhostStack(copy);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return applyButton(id);
    }

    public boolean applyButton(int id) {
        if (id < 0) {
            return false;
        }
        if (getGhostStack().isEmpty()) {
            if (id >= data.size()) {
                return false;
            }
            data.remove(id);
        } else {
            if (id >= listToDisplay.size()) {
                return false;
            }
            PawConfigEntry entry = listToDisplay.get(id);
            if (!data.contains(entry)) {
                data.add(entry);
            }
        }
        refreshDisplayedList();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == 0) {
            setGhostStack(ItemStack.EMPTY);
            return ItemStack.EMPTY;
        }
        ItemStack stack = getSlot(index).getItem();
        if (!stack.isEmpty()) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            setGhostStack(copy);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide || pawStacks.isEmpty()) {
            return;
        }
        for (ItemStack pawStack : pawStacks) {
            if (!(pawStack.getItem() instanceof PawsItem pawsItem)) {
                continue;
            }
            if (heldItems) {
                pawsItem.setHeldItemsConfig(pawStack, data.isEmpty() ? null : data);
            } else {
                pawsItem.setCanInteractConfig(pawStack, data.isEmpty() ? null : data);
            }
        }
    }

    private List<PawConfigEntry> createEntriesForStack(ItemStack stack) {
        List<PawConfigEntry> entries = new ArrayList<>();
        if (heldItems) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId != null) {
                entries.add(new PawConfigEntry(false, itemId));
            }
            stack.getItem().builtInRegistryHolder().tags()
                    .map(TagKey::location)
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .map(id -> new PawConfigEntry(true, id))
                    .forEach(entry -> {
                        if (!entries.contains(entry)) {
                            entries.add(entry);
                        }
                    });
            return entries;
        }

        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return List.of();
        }

        Block block = blockItem.getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        if (blockId != null) {
            entries.add(new PawConfigEntry(false, blockId));
        }
        block.builtInRegistryHolder().tags()
                .map(TagKey::location)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .map(id -> new PawConfigEntry(true, id))
                .forEach(entry -> {
                    if (!entries.contains(entry)) {
                        entries.add(entry);
                    }
                });
        return entries;
    }

    public static class Provider implements MenuProvider {
        private final Component title;
        private final boolean heldItems;
        private final List<PawConfigEntry> data;
        private final List<ItemStack> pawStacks;

        public Provider(Component title, boolean heldItems, List<PawConfigEntry> data, List<ItemStack> pawStacks) {
            this.title = title;
            this.heldItems = heldItems;
            this.data = data;
            this.pawStacks = pawStacks;
        }

        @Override
        public Component getDisplayName() {
            return title;
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
            return new PawsConfigMenu(containerId, inventory, heldItems, data, pawStacks);
        }
    }
}