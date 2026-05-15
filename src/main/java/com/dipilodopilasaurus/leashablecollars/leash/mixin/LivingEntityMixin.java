package com.dipilodopilasaurus.leashablecollars.leash.mixin;

import com.dipilodopilasaurus.leashablecollars.block.DogBedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    protected LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "setPosToBed", at = @At("HEAD"), cancellable = true, require = 0)
    private void correctDogBedHeight(BlockPos pos, CallbackInfo ci) {
        BlockState state = level().getBlockState(pos);
        if (!(state.getBlock() instanceof DogBedBlock)) {
            return;
        }

        net.minecraft.core.Direction facing = state.getValue(BedBlock.FACING);
        setPos(pos.getX() + 0.5D + facing.getStepX() * 0.1D, pos.getY() + 0.35D, pos.getZ() + 0.5D + facing.getStepZ() * 0.1D);
        ci.cancel();
    }
}