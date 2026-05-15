package com.dipilodopilasaurus.leashablecollars.leash.mixin;

import com.dipilodopilasaurus.leashablecollars.LeashableCollars;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeashFenceKnotEntity.class)
public abstract class MixinLeashKnotEntity {
    @Inject(method = "interact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/LeashFenceKnotEntity;discard()V"), cancellable = true)
    private void preventLeashKnotBreak(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        LeashFenceKnotEntity leashFenceKnot = (LeashFenceKnotEntity) (Object) this;
        if (!leashFenceKnot.level().isClientSide && LeashableCollars.blockLeashKnotBreak(player, leashFenceKnot)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}