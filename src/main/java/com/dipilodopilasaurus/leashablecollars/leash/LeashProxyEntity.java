package com.dipilodopilasaurus.leashablecollars.leash;

import com.dipilodopilasaurus.leashablecollars.LeashableCollars;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
public final class LeashProxyEntity extends Turtle {
    public static final String TEAM_NAME = "leashplayersimpl";
    private static final EntityDimensions DIMENSIONS = EntityDimensions.fixed(1.0E-6F, 1.0E-6F);

    private final LivingEntity target;

    public LeashProxyEntity(LivingEntity target) {
        super(EntityType.TURTLE, target.level());
        this.target = target;

        setHealth(1.0F);
        setInvulnerable(true);
        setBaby(true);
        setInvisible(true);
        noPhysics = true;

        MinecraftServer server = getServer();
        if (server != null) {
            ServerScoreboard scoreboard = server.getScoreboard();
            PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
            if (team == null) {
                team = scoreboard.addPlayerTeam(TEAM_NAME);
            }
            if (team.getCollisionRule() != PlayerTeam.CollisionRule.NEVER) {
                team.setCollisionRule(PlayerTeam.CollisionRule.NEVER);
            }
            scoreboard.addPlayerToTeam(getScoreboardName(), team);
        }

        proxyUpdate();
    }

    private Vec3 getTargetOffset() {
        float bodyYaw = target.yBodyRot;
        return switch (target.getPose()) {
            case CROUCHING -> new Vec3(0.0D, 1.1D, -0.15D);
            case SWIMMING -> Vec3.directionFromRotation(0.0F, bodyYaw).scale(0.35D).add(0.0D, 0.2D, -0.1D);
            case FALL_FLYING -> new Vec3(0.0D, 1.3D, -0.15D)
                    .xRot((float) Math.toRadians(-(90.0D + target.getXRot())))
                .yRot((float) Math.toRadians(-bodyYaw));
            case SLEEPING -> target.getBedOrientation() != null
                    ? new Vec3(target.getBedOrientation().getStepX() * -0.2D, 0.1D, target.getBedOrientation().getStepZ() * -0.2D - 0.15D)
                    : new Vec3(0.0D, 0.1D, -0.15D);
            default -> new Vec3(0.0D, 1.3D, -0.15D);
        };
    }

    private boolean proxyUpdate() {
        if (proxyIsRemoved()) {
            return false;
        }
        if (target == null) {
            return true;
        }
        if (target.level() != level() || !target.isAlive()) {
            return true;
        }

        Vec3 currentPos = this.position();
        Vec3 targetPos = target.position().add(getTargetOffset().scale(target.getScale()));
        if (!Objects.equals(currentPos, targetPos)) {
            setRot(0.0F, 0.0F);
            setPos(targetPos.x(), targetPos.y(), targetPos.z());
            setBoundingBox(DIMENSIONS.makeBoundingBox(target.position()));
        }

        tickLeash();
        return false;
    }

    @Override
    public void tick() {
        if (this.level().isClientSide) {
            return;
        }
        if (proxyUpdate() && !proxyIsRemoved()) {
            proxyRemove();
        }
    }

    public boolean proxyIsRemoved() {
        return this.isRemoved();
    }

    public void proxyRemove() {
        super.remove(RemovalReason.DISCARDED);
    }

    @Override
    public void remove(RemovalReason reason) {
        // Proxy removal is managed explicitly through proxyRemove() so leash cleanup stays consistent.
    }

    @Override
    public float getHealth() {
        return 1.0F;
    }

    @Override
    public void dropLeash(boolean sendPacket, boolean dropItem) {
        // The proxy never drops leash items because the real player interaction handles that logic.
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    public boolean canUnleash(Entity entity) {
        if (entity.equals(target)) {
            if (target instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.playercollars.no_break_fence").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }

        if (!level().getGameRules().getBoolean(LeashableCollars.ALLOW_UNLEASH_OTHER)) {
            ItemStack ownerCollar = LeashableCollars.findOwnedCollar(target, entity.getUUID(), target.getUUID());
            if (ownerCollar == null) {
                if (entity instanceof Player player) {
                    player.displayClientMessage(Component.translatable("message.playercollars.no_break_fence_other", target.getName()).withStyle(ChatFormatting.RED), true);
                }
                return false;
            }
        }

        return true;
    }

    @Override
    protected void registerGoals() {
        // The proxy turtle does not run AI.
    }

    @Override
    protected void doPush(Entity entity) {
        // The proxy should not physically interact with nearby entities.
    }

    @Override
    public void push(Entity entity) {
        // The proxy should not physically interact with nearby entities.
    }

    @Override
    public void playerTouch(Player player) {
        // The proxy should never behave like a normal touchable mob.
    }
}
