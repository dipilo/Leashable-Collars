package com.dipilodopilasaurus.leashablecollars.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RotationLerpHandler {
    private static final float TIME_TO_TURN = 0.25f;
    private static float turnTimer = TIME_TO_TURN + 1;
    private static float rotX;
    private static float rotY;
    private static long millis;

    private RotationLerpHandler() {
    }

    @SubscribeEvent
    public static void turnTowardsClick(ViewportEvent.ComputeCameraAngles event) {
        if (turnTimer < TIME_TO_TURN) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                turnTimer = TIME_TO_TURN + 1;
                return;
            }

            long currentMillis = Util.getMillis();
            float delta = (currentMillis - millis) / 1000f;
            player.turn(rotY * delta, rotX * delta);
            turnTimer += delta;
            millis = currentMillis;
        }
    }

    public static void beginClickTurn(Vec3 towards) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        turnTimer = 0;
        Vec3 offset = EntityAnchorArgument.Anchor.EYES.apply(player).subtract(towards);
        double horizontalDistance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        rotX = Mth.wrapDegrees((float) ((Mth.atan2(offset.y, horizontalDistance) * Mth.RAD_TO_DEG) - player.getXRot())) / TIME_TO_TURN / 0.15f;
        rotY = Mth.wrapDegrees((float) (-Mth.atan2(-offset.z, offset.x) * Mth.RAD_TO_DEG) + 90.0F - player.getYRot()) / TIME_TO_TURN / 0.15f;
        millis = Util.getMillis();
    }
}
