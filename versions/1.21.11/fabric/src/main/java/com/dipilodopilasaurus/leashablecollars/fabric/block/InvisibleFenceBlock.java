package com.dipilodopilasaurus.leashablecollars.fabric.block;

import com.dipilodopilasaurus.leashablecollars.fabric.LeashableCollarsFabric;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleUtil;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.Set;

public class InvisibleFenceBlock extends FenceBlock {
    public static final RegistryKey<Block> REGISTRY_KEY = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(LeashableCollarsFabric.MOD_ID, "invisible_fence"));
    public static final RegistryKey<Item> ITEM_REGISTRY_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LeashableCollarsFabric.MOD_ID, "invisible_fence"));
    public static final BooleanProperty POWERED = Properties.POWERED;

    private static final Set<Direction> HORIZONTAL_DIRECTIONS = EnumSet.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    public InvisibleFenceBlock(AbstractBlock.Settings settings) {
        super(settings.registryKey(REGISTRY_KEY));
        setDefaultState(getStateManager().getDefaultState().with(POWERED, false).with(WATERLOGGED, false));
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(POWERED);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        state = super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (neighborState.isOf(this) && neighborState.get(POWERED) != state.get(POWERED)) {
            state = state.with(POWERED, neighborState.get(POWERED));
        }
        return state;
    }

    @Override
    protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) {
            return null;
        }
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            BlockState neighbor = context.getWorld().getBlockState(context.getBlockPos().offset(direction));
            if (neighbor.isOf(this) && neighbor.get(POWERED)) {
                return state.with(POWERED, true);
            }
        }
        return state;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (context instanceof EntityShapeContext entityContext) {
            if (state.get(POWERED) && entityContext.getEntity() instanceof LivingEntity livingEntity) {
                if (!hasCollar(livingEntity)) {
                    return VoxelShapes.empty();
                }
                return super.getCollisionShape(state, world, pos, context);
            }
            if (entityContext.getEntity() == null) {
                return super.getCollisionShape(state, world, pos, context);
            }
        }
        return VoxelShapes.empty();
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        if (state.get(POWERED) && random.nextFloat() < 0.25F) {
            ParticleUtil.spawnParticlesAround(world, pos, 1, 0.5, 0.5, true, DustParticleEffect.DEFAULT);
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.PASS;
        }
        if (hasCollar(player)) {
            player.sendMessage(Text.translatable("block.playercollars.invisible_fence.toggle_fail").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        boolean powered = !state.get(POWERED);
        setConnectedPowered(world, pos, powered);
        player.sendMessage(Text.translatable(powered
                ? "block.playercollars.invisible_fence.toggle_on"
                : "block.playercollars.invisible_fence.toggle_off").formatted(Formatting.GREEN), true);
        return ActionResult.SUCCESS;
    }

    private static void setConnectedPowered(World world, BlockPos startPos, boolean powered) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(startPos);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.removeFirst();
            BlockState currentState = world.getBlockState(currentPos);
            if (!currentState.isOf(LeashableCollarsFabric.INVISIBLE_FENCE_BLOCK)) {
                continue;
            }
            if (currentState.get(POWERED) != powered) {
                world.setBlockState(currentPos, currentState.with(POWERED, powered), 7);
            }

            for (Direction direction : HORIZONTAL_DIRECTIONS) {
                BlockPos neighborPos = currentPos.offset(direction);
                BlockState neighborState = world.getBlockState(neighborPos);
                if (neighborState.isOf(LeashableCollarsFabric.INVISIBLE_FENCE_BLOCK) && neighborState.get(POWERED) != powered) {
                    queue.addLast(neighborPos);
                }
            }
        }
    }

    private static boolean hasCollar(LivingEntity livingEntity) {
        if (!(livingEntity instanceof PlayerEntity player)) {
            return false;
        }

        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (player.getInventory().getStack(slot).isIn(LeashableCollarsFabric.COLLAR_TAG)) {
                return true;
            }
        }

        return false;
    }
}