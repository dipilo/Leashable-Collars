package com.dipilodopilasaurus.leashablecollars.neoforge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DogBedBlock extends BedBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    public DogBedBlock(DyeColor color, BlockBehaviour.Properties properties) {
        super(color, properties
                .mapColor(MapColor.WOOL)
                .strength(0.2F)
                .noOcclusion()
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }
}