package com.dipilodopilasaurus.leashablecollars.neoforge.block;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.Set;

import top.theillusivec4.curios.api.CuriosApi;

public class InvisibleFenceBlock extends FenceBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final Set<Direction> HORIZONTAL_DIRECTIONS = EnumSet.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    public InvisibleFenceBlock(Properties properties) {
        super(properties.strength(0.5F).noOcclusion().pushReaction(PushReaction.NORMAL));
        registerDefaultState(stateDefinition.any().setValue(POWERED, false).setValue(WATERLOGGED, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            BlockState neighbor = context.getLevel().getBlockState(context.getClickedPos().relative(direction));
            if (neighbor.getBlock() instanceof InvisibleFenceBlock && neighbor.getValue(POWERED)) {
                return state.setValue(POWERED, true);
            }
        }
        return state;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (state.getValue(POWERED) && entity instanceof LivingEntity livingEntity) {
                if (!hasCollar(livingEntity)) {
                    return Shapes.empty();
                }
                return super.getCollisionShape(state, level, pos, context);
            }
            if (entity == null) {
                return super.getCollisionShape(state, level, pos, context);
            }
        }
        return Shapes.empty();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (hasCollar(player)) {
            player.displayClientMessage(Component.translatable("block.playercollars.invisible_fence.toggle_fail"), true);
            return InteractionResult.FAIL;
        }
        boolean powered = !state.getValue(POWERED);
        setConnectedPowered(level, pos, powered);
        player.displayClientMessage(Component.translatable(powered
                ? "block.playercollars.invisible_fence.toggle_on"
                : "block.playercollars.invisible_fence.toggle_off"), true);
        return InteractionResult.SUCCESS;
    }

    private static void setConnectedPowered(Level level, BlockPos startPos, boolean powered) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(startPos);
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.removeFirst();
            BlockState currentState = level.getBlockState(currentPos);
            if (!(currentState.getBlock() instanceof InvisibleFenceBlock)) {
                continue;
            }
            if (currentState.getValue(POWERED) != powered) {
                level.setBlock(currentPos, currentState.setValue(POWERED, powered), 7);
            }
            for (Direction direction : HORIZONTAL_DIRECTIONS) {
                BlockPos neighborPos = currentPos.relative(direction);
                BlockState neighborState = level.getBlockState(neighborPos);
                if (neighborState.getBlock() instanceof InvisibleFenceBlock && neighborState.getValue(POWERED) != powered) {
                    queue.addLast(neighborPos);
                }
            }
        }
    }

    private static boolean hasCollar(LivingEntity livingEntity) {
        return CuriosApi.getCuriosInventory(livingEntity)
                .map(handler -> handler.findFirstCurio(stack -> stack.is(LeashableCollarsNeoForge.COLLAR_TAG)).isPresent())
                .orElse(false);
    }
}