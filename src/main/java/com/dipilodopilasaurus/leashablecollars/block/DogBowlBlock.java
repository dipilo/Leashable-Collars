package com.dipilodopilasaurus.leashablecollars.block;

import com.dipilodopilasaurus.leashablecollars.LeashableCollars;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.food.FoodProperties;

public class DogBowlBlock extends BaseEntityBlock {
    public static final IntegerProperty LEVEL = BlockStateProperties.AGE_3;
    public static final BooleanProperty MILK = BlockStateProperties.SNOWY;

    private static final VoxelShape SHAPE_BASE = Shapes.or(
            Block.box(2.0, 0.0, 1.0, 14.0, 5.0, 2.0),
            Block.box(2.0, 0.0, 14.0, 14.0, 5.0, 15.0),
            Block.box(1.0, 0.0, 1.0, 2.0, 5.0, 15.0),
            Block.box(14.0, 0.0, 1.0, 15.0, 5.0, 15.0)
    );
    private static final VoxelShape[] SHAPES = new VoxelShape[]{
            Shapes.or(SHAPE_BASE, Block.box(2.0, 0.0, 2.0, 14.0, 1.0, 14.0)),
            Shapes.or(SHAPE_BASE, Block.box(2.0, 0.0, 2.0, 14.0, 2.0, 14.0)),
            Shapes.or(SHAPE_BASE, Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0)),
            Shapes.or(SHAPE_BASE, Block.box(2.0, 0.0, 2.0, 14.0, 6.0, 14.0))
    };

    private final DyeColor color;

    public DogBowlBlock(DyeColor color) {
        super(Properties.of().strength(0.6F).noOcclusion().pushReaction(PushReaction.DESTROY));
        this.color = color;
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0).setValue(MILK, false));
    }

    public DyeColor getColor() {
        return color;
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
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return predictClientInteraction(state, player.getItemInHand(hand), player);
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!(level.getBlockEntity(pos) instanceof DogBowlBlockEntity bowl)) {
            return InteractionResult.PASS;
        }
        InteractionResult insertResult = tryInsertContents(state, level, pos, player, hand, stack, bowl);
        if (insertResult.consumesAction()) {
            return insertResult;
        }

        return tryTakeContents(state, level, pos, player, bowl);
    }

    private InteractionResult predictClientInteraction(BlockState state, ItemStack stack, Player player) {
        if (!stack.isEmpty()) {
            if (stack.is(Items.MILK_BUCKET) && !state.getValue(MILK) && state.getValue(LEVEL) == 0) {
                return InteractionResult.SUCCESS;
            }
            if (stack.getFoodProperties(player) != null) {
                return InteractionResult.SUCCESS;
            }
        }
        return state.getValue(MILK) || state.getValue(LEVEL) > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private InteractionResult tryInsertContents(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, DogBowlBlockEntity bowl) {
        if (stack.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (tryInsertMilk(state, level, pos, player, hand, stack, bowl)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return tryInsertFood(state, level, pos, player, stack, bowl)
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
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
        player.playSound(SoundEvents.BUCKET_EMPTY, 1.0F, 1.0F);
        return true;
    }

    private boolean tryInsertFood(BlockState state, Level level, BlockPos pos, Player player, ItemStack stack, DogBowlBlockEntity bowl) {
        FoodProperties food = stack.getFoodProperties(player);
        if (food == null) {
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

    private InteractionResult tryTakeContents(BlockState state, Level level, BlockPos pos, Player player, DogBowlBlockEntity bowl) {
        ItemStack taken = bowl.takeOne();
        if (taken.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (taken.is(Items.MILK_BUCKET)) {
            return consumeMilk(level, pos, state, player);
        }

        updateLevelState(level, pos, state, bowl);
        FoodProperties food = taken.getFoodProperties(player);
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

    private InteractionResult consumeMilk(Level level, BlockPos pos, BlockState state, Player player) {
        level.setBlock(pos, state.setValue(MILK, false), 3);
        if (!level.isClientSide) {
            player.removeAllEffects();
        }
        player.playSound(SoundEvents.GENERIC_DRINK, 1.0F, 1.0F);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void updateLevelState(Level level, BlockPos pos, BlockState state, DogBowlBlockEntity bowl) {
        level.setBlock(pos, state.setValue(LEVEL, Math.min((bowl.getCount() + 20) / 21, 3)), 3);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DogBowlBlockEntity bowl) {
            bowl.drop();
        }
        super.onRemove(state, level, pos, newState, isMoving);
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
            super(LeashableCollars.DOG_BOWL_BLOCK_ENTITY.get(), pos, state);
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (!inBowl.isEmpty()) {
                tag.put("item", inBowl.save(new CompoundTag()));
            }
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            if (tag.contains("item")) {
                inBowl = ItemStack.of(tag.getCompound("item"));
            } else {
                inBowl = ItemStack.EMPTY;
            }
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
            if (ItemStack.isSameItemSameTags(inBowl, stack)) {
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
