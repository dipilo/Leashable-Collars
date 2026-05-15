package com.dipilodopilasaurus.leashablecollars.neoforge.client;

import com.dipilodopilasaurus.leashablecollars.neoforge.client.screen.CollarDyeScreen;
import com.dipilodopilasaurus.leashablecollars.neoforge.client.screen.DeedItemScreen;
import com.dipilodopilasaurus.leashablecollars.neoforge.client.screen.PawsSelectScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class ClientScreenHooks {
    private ClientScreenHooks() {
    }

    public static void openCollarDyeScreen(ItemStack stack, UUID playerId) {
        Minecraft.getInstance().setScreen(new CollarDyeScreen(stack, playerId));
    }

    public static void openDeedItemScreen(ItemStack stack, Player player) {
        Minecraft.getInstance().setScreen(new DeedItemScreen(stack, player));
    }

    public static void openPawsSelectScreen(UUID targetId, String targetName) {
        Minecraft.getInstance().setScreen(new PawsSelectScreen(targetId, targetName));
    }
}