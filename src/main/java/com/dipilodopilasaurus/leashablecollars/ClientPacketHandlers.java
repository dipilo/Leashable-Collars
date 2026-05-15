package com.dipilodopilasaurus.leashablecollars;

import com.dipilodopilasaurus.leashablecollars.client.RotationLerpHandler;
import com.dipilodopilasaurus.leashablecollars.client.screen.PawsSelectScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void openPawsSelect(UUID targetUuid, String targetName) {
        Minecraft.getInstance().setScreen(new PawsSelectScreen(targetUuid, targetName));
    }

    public static void beginClickTurn(Vec3 towards) {
        RotationLerpHandler.beginClickTurn(towards);
    }
}