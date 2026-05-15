package com.dipilodopilasaurus.leashablecollars.leash.mixin;

import com.dipilodopilasaurus.leashablecollars.PawEffectsHandler;
import com.dipilodopilasaurus.leashablecollars.leash.LeashImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayerEntity {
    @Redirect(method = "updatePlayerPose", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setPose(Lnet/minecraft/world/entity/Pose;)V"), require = 0)
    private void forceFootPawsPose(Player player, Pose pose) {
        if (PawEffectsHandler.shouldForceFootPawsCrawl(player)
                && (pose == Pose.STANDING || pose == Pose.CROUCHING)) {
            pose = Pose.SWIMMING;
        }
        player.setPose(pose);
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void applyFootPawsCrawlAfterPlayerTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        boolean forceCrawl = PawEffectsHandler.shouldForceFootPawsCrawl(player);
        player.setForcedPose(forceCrawl ? Pose.SWIMMING : null);
    }

    @Inject(method = "interactOn", at = @At("RETURN"), cancellable = true)
    private void onLeashPlayersInteract(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() != InteractionResult.PASS) {
            return;
        }
        Object self = this;
        if (self instanceof ServerPlayer player && entity instanceof LeashImpl leashImpl) {
            cir.setReturnValue(leashImpl.leashPlayersInteract(player, hand));
            cir.cancel();
        }
    }
}
