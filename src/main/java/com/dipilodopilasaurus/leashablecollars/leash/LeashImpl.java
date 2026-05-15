package com.dipilodopilasaurus.leashablecollars.leash;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public interface LeashImpl {
    InteractionResult leashPlayersInteract(Player player, InteractionHand hand);

    Entity leashPlayersGetProxyLeashHolder();
}
