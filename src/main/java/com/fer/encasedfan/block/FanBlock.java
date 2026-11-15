package com.fer.encasedfan.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.fer.encasedfan.registry.ModBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Ventilador activado por redstone.
 * RANGE (0-3) -> alcance en bloques {0, 2, 4, 6} segun la fuerza de la senal.
 */
public class FanBlock extends BaseEntityBlock {
    public static final MapCodec<FanBlock> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            propertiesCodec(),
            Codec.BOOL.fieldOf("golden").forGetter(b -> b.golden)
    ).apply(inst, FanBlock::new));

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty RANGE = IntegerProperty.create("range", 0, 3);
    private static final int[] RANGE_BLOCKS = {0, 2, 4, 6};

    private final boolean golden;

    public FanBlock(Properties props, boolean golden) {
        super(props);
        this.golden = golden;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RANGE, 0));
    }

    public boolean isGolden() {
        return golden;
    }

    /** Alcance nominal en bloques segun el estado. */
    public static int rangeBlocks(BlockState state) {
        return RANGE_BLOCKS[state.getValue(RANGE)];
    }

    /** 0 = apagado, 1-5 = 2 bloques, 6-10 = 4 bloques, 11-15 = 6 bloques. */
    public static int rangeLevelForSignal(int signal) {
        if (signal <= 0) return 0;
        if (signal <= 5) return 1;
        if (signal <= 10) return 2;
        return 3;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RANGE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // El viento sale hacia el jugador (asi ves las aspas al colocarlo).
        Direction facing = ctx.getNearestLookingDirection().getOpposite();
        int range = rangeLevelForSignal(ctx.getLevel().getBestNeighborSignal(ctx.getClickedPos()));
        return defaultBlockState().setValue(FACING, facing).setValue(RANGE, range);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   BlockPos fromPos, boolean moving) {
        if (level.isClientSide) return;
        int range = rangeLevelForSignal(level.getBestNeighborSignal(pos));
        if (state.getValue(RANGE) != range) {
            level.setBlock(pos, state.setValue(RANGE, range), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // BaseEntityBlock lo pone INVISIBLE por defecto
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FanBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FAN.get(),
                level.isClientSide ? FanBlockEntity::clientTick : FanBlockEntity::serverTick);
    }
}
