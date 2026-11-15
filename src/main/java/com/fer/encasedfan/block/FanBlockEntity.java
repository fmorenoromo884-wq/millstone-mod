package com.fer.encasedfan.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.fer.encasedfan.registry.ModBlockEntities;

public class FanBlockEntity extends BlockEntity {
    /** Alcance real (bloques libres de obstaculos, <= alcance nominal). */
    private int reach = 0;

    // --- animacion (solo cliente) ---
    public float angle, prevAngle, speed;

    public FanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FAN.get(), pos, state);
    }

    public int getReach() {
        return reach;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FanBlockEntity be) {
        int nominal = FanBlock.rangeBlocks(state);
        if (nominal == 0) {
            be.reach = 0;
            return;
        }
        Direction dir = state.getValue(FanBlock.FACING);
        be.reach = FanLogic.computeReach(level, pos, dir, nominal);
        if (be.reach > 0) {
            FanLogic.pushEntities(level, pos, dir, be.reach, ((FanBlock) state.getBlock()).isGolden());
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, FanBlockEntity be) {
        int nominal = FanBlock.rangeBlocks(state);
        boolean golden = ((FanBlock) state.getBlock()).isGolden();
        Direction dir = state.getValue(FanBlock.FACING);

        if (nominal == 0) {
            be.reach = 0;
        } else if (level.getGameTime() % 10 == 0) {
            be.reach = FanLogic.computeReach(level, pos, dir, nominal);
        }

        // Las aspas aceleran y frenan suavemente. Dorado gira el doble de rapido.
        float target = nominal == 0 ? 0f : (golden ? 48f : 24f);
        be.speed += (target - be.speed) * 0.08f;
        be.prevAngle = be.angle;
        be.angle += be.speed;
        if (be.angle >= 360f) {
            be.angle -= 360f;
            be.prevAngle -= 360f;
        }

        // Particulas de viento a lo largo de la corriente
        if (be.reach > 0 && level.random.nextInt(golden ? 2 : 4) == 0) {
            double dist = 0.6 + level.random.nextDouble() * (be.reach - 0.6);
            double x = pos.getX() + 0.5 + dir.getStepX() * (0.5 + dist);
            double y = pos.getY() + 0.5 + dir.getStepY() * (0.5 + dist);
            double z = pos.getZ() + 0.5 + dir.getStepZ() * (0.5 + dist);
            if (dir.getAxis() != Direction.Axis.X) x += (level.random.nextDouble() - 0.5) * 0.7;
            if (dir.getAxis() != Direction.Axis.Y) y += (level.random.nextDouble() - 0.5) * 0.7;
            if (dir.getAxis() != Direction.Axis.Z) z += (level.random.nextDouble() - 0.5) * 0.7;
            double v = golden ? 0.12 : 0.07;
            level.addParticle(ParticleTypes.CLOUD, x, y, z,
                    dir.getStepX() * v, dir.getStepY() * v, dir.getStepZ() * v);
        }
    }
}
