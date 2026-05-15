package com.dipilodopilasaurus.leashablecollars.leash.mixin;

import com.dipilodopilasaurus.leashablecollars.leash.LeashProxyEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class MixinServerWorld {
    @Inject(method = "shouldDiscardEntity", at = @At("HEAD"), cancellable = true, require = 0)
    private void allowLeashProxyPersistence(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LeashProxyEntity) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
