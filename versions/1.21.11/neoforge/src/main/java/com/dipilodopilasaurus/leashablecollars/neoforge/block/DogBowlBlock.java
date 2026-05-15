package com.dipilodopilasaurus.leashablecollars.neoforge.block;

import com.mojang.serialization.MapCodec;
import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DogBowlBlock extends BaseEntityBlock {
    public static final IntegerProperty LEVEL = BlockStateProperties.AGE_3;
    public static final BooleanProperty MILK = BlockStateProperties.SNOWY;

    private static final VoxelShape SHAPE_BASE = Shapes.or(
            Block.box(2.0, 0.0, 1.0, 14.0, 5.0, 2.0),
            Block.box(2.0, 0.0, 14.0, 14.0, 5.0, 15.0),
            Block.box(1.0, 0.0, 1.0, 2.0, 5.0, 15.0),
            Block.box(14.0, 0.0, 1.0, 15.0, 5.0, 15.0)
    );
    private static final VoxelShape[] SHAPES = new VoxelShape[] {
            Shapes.or(SHAPE_BASE, Block.box(2.0, 0.0, 2.0, 14.0, 1.0, 14.0)),
            Shapes.or(SHAPE_BASE, Block.box(2.0, 0.0, 2.0, 14.0, 2.0, 14.0)),
            Shapes.or(SHAPE_BASE, Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0)),
            Shapes.or(SHAPE_BASE, Block.box(2.0, 0.0, 2.0, 14.0, 6.0, 14.0))
    };

    private final DyeColor color;

    public DogBowlBlock(DyeColor color, Properties properties) {
        super(properties.strength(0.6F).noOcclusion().pushReaction(PushReaction.DESTROY));
        this.color = color;
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0).setValue(MILK, false));
    }

    public DyeColor getColor() {
        return color;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new DogBowlBlock(this.color, properties));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LEVEL)];
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return Block.canSupportCenter(level, below, Direction.UP) || level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DogBowlBlockEntity bowl)) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return predictsInsert(state, stack) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (tryInsertMilk(state, level, pos, player, hand, stack, bowl) || tryInsertFood(state, level, pos, stack, bowl)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DogBowlBlockEntity bowl)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return state.getValue(MILK) || state.getValue(LEVEL) > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        ItemStack taken = bowl.takeOne();
        if (taken.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (taken.is(Items.MILK_BUCKET)) {
            level.setBlock(pos, state.setValue(MILK, false), 3);
            player.removeAllEffects();
            player.playSound(net.minecraft.sounds.SoundEvents.GENERIC_DRINK.value(), 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }

        updateLevelState(level, pos, state, bowl);
        FoodProperties food = taken.get(DataComponents.FOOD);
        if (food != null && player.canEat(food.canAlwaysEat())) {
            ItemStack remainder = taken.finishUsingItem(level, player);
            if (!remainder.isEmpty() && !player.addItem(remainder)) {
                player.drop(remainder, true);
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.addItem(taken)) {
            player.drop(taken, true);
        }
        return InteractionResult.SUCCESS;
    }

    private boolean predictsInsert(BlockState state, ItemStack stack) {
        if (stack.is(Items.MILK_BUCKET) && !state.getValue(MILK) && state.getValue(LEVEL) == 0) {
            return true;
        }
        return stack.get(DataComponents.FOOD) != null;
    }

    private boolean tryInsertMilk(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, DogBowlBlockEntity bowl) {
        if (!stack.is(Items.MILK_BUCKET) || bowl.getCount() != 0) {
            return false;
        }
        bowl.setStack(new ItemStack(Items.MILK_BUCKET));
        level.setBlock(pos, state.setValue(MILK, true), 3);
        if (!player.getAbilities().instabuild) {
            player.setItemInHand(hand, new ItemStack(Items.BUCKET));
        }
        player.playSound(net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, 1.0F, 1.0F);
        return true;
    }

    private boolean tryInsertFood(BlockState state, Level level, BlockPos pos, ItemStack stack, DogBowlBlockEntity bowl) {
        if (stack.get(DataComponents.FOOD) == null) {
            return false;
        }
        int inserted = bowl.insert(stack);
        if (inserted <= 0) {
            return false;
        }
        stack.shrink(inserted);
        updateLevelState(level, pos, state, bowl);
        return true;
    }

    private void updateLevelState(Level level, BlockPos pos, BlockState state, DogBowlBlockEntity bowl) {
        level.setBlock(pos, state.setValue(LEVEL, Math.min((bowl.getCount() + 20) / 21, 3)), 3);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, MILK);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DogBowlBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    public static class DogBowlBlockEntity extends BlockEntity {
        private ItemStack inBowl = ItemStack.EMPTY;

        public DogBowlBlockEntity(BlockPos pos, BlockState state) {
            super(LeashableCollarsNeoForge.DOG_BOWL_BLOCK_ENTITY.get(), pos, state);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            if (!inBowl.isEmpty()) {
                output.store("item", ItemStack.CODEC, inBowl);
            }
        }

        @Override
        public void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            inBowl = input.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            super.preRemoveSideEffects(pos, state);
            drop();
        }

        public int getCount() {
            return inBowl.getCount();
        }

        public void setStack(ItemStack stack) {
            inBowl = stack.copy();
            setChanged();
        }

        public int insert(ItemStack stack) {
            if (inBowl.isEmpty()) {
                inBowl = stack.copy();
                setChanged();
                return stack.getCount();
            }
            if (ItemStack.isSameItemSameComponents(inBowl, stack)) {
                int amount = Math.min(stack.getCount(), inBowl.getMaxStackSize() - inBowl.getCount());
                inBowl.grow(amount);
                setChanged();
                return amount;
            }
            return 0;
        }

        public ItemStack takeOne() {
            if (inBowl.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack single = inBowl.copy();
            single.setCount(1);
            inBowl.shrink(1);
            if (inBowl.isEmpty()) {
                inBowl = ItemStack.EMPTY;
            }
            setChanged();
            return single;
        }

        public void drop() {
            if (level != null && !inBowl.isEmpty() && !inBowl.is(Items.MILK_BUCKET)) {
                level.addFreshEntity(new ItemEntity(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inBowl.copy()));
            }
            inBowl = ItemStack.EMPTY;
        }
    }
}