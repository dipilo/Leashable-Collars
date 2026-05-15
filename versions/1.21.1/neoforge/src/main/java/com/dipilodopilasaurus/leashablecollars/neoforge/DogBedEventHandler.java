package com.dipilodopilasaurus.leashablecollars.neoforge;

import com.dipilodopilasaurus.leashablecollars.neoforge.block.DogBedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class DogBedEventHandler {
    private DogBedEventHandler() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.isSleeping()) {
            return;
        }

        BlockPos sleepingPos = player.getSleepingPos().orElse(null);
        if (sleepingPos == null) {
            return;
        }

        BlockState state = player.level().getBlockState(sleepingPos);
        if (!(state.getBlock() instanceof DogBedBlock)) {
            return;
        }

        Vec3 base = Vec3.atBottomCenterOf(sleepingPos);
        Vec3 offset = new Vec3(state.getValue(BedBlock.FACING).getStepX() * 0.1D, 0.35D, state.getValue(BedBlock.FACING).getStepZ() * 0.1D);
        Vec3 targetPos = base.add(offset);
        if (player.position().distanceToSqr(targetPos) > 1.0E-4D) {
            player.setPos(targetPos.x, targetPos.y, targetPos.z);
        }
    }
}