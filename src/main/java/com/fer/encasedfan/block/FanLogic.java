package com.fer.encasedfan.block;

import com.fer.encasedfan.registry.ModTags;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Toda la fisica del ventilador. Velocidades en bloques/segundo, se convierten a bloques/tick. */
public final class FanLogic {
    private FanLogic() {}

    // ---- Velocidades base (ventilador normal, suelo normal) ----
    /** Entidad: 6 bloques en 6 s. */
    public static final double ENTITY_BPS = 1.0;
    /** Item suelto: 6 bloques en 4 s (x1.5). */
    public static final double ITEM_BPS = 1.5;
    /** El dorado va al doble. */
    public static final double GOLDEN_MULT = 2.0;
    /** Sobre miel todo va a la mitad (solo flujo horizontal). */
    public static final double HONEY_MULT = 0.5;
    /** Hacia arriba: items x1.5, entidades a la mitad (y solo el dorado las levanta). */
    public static final double UP_ITEM_MULT = 1.5;
    public static final double UP_ENTITY_MULT = 0.5;

    /** ItemEntity aplica -0.04 de gravedad ANTES de moverse; lo compensamos al subir/flotar. */
    private static final double ITEM_GRAVITY = 0.04;

    // ------------------------------------------------------------------ alcance
    /** Cuantos bloques libres hay desde el ventilador (hasta el alcance nominal). */
    public static int computeReach(Level level, BlockPos fanPos, Direction dir, int nominal) {
        int reach = 0;
        BlockPos.MutableBlockPos p = fanPos.mutable();
        for (int i = 1; i <= nominal; i++) {
            p.move(dir);
            if (!level.isLoaded(p)) break;
            BlockState s = level.getBlockState(p);
            if (!letsAirThrough(level, p, s, dir)) break;
            reach = i;
        }
        return reach;
    }

    /** Decide si un bloque deja pasar la corriente. Por defecto bloquea. */
    public static boolean letsAirThrough(Level level, BlockPos pos, BlockState s, Direction flow) {
        if (s.isAir()) return true;
        Block b = s.getBlock();
        if (b instanceof LiquidBlock) return true;               // agua y lava (cambian la corriente)
        if (s.is(ModTags.AIR_PASSABLE)) return true;             // vallas, hojas, fogatas, antorchas...
        if (s.getCollisionShape(level, pos).isEmpty()) return true; // cualquier bloque sin colision

        // Trampillas, puertas y puertas de valla: solo si estan abiertas
        if (b instanceof TrapDoorBlock || b instanceof DoorBlock || b instanceof FenceGateBlock) {
            return s.getValue(BlockStateProperties.OPEN);
        }

        // Paneles de cristal (los barrotes de hierro ya pasan por el tag)
        if (b instanceof IronBarsBlock) {
            if (flow.getAxis().isVertical()) return true;
            Direction left = flow.getCounterClockWise();
            Direction right = flow.getClockWise();
            boolean wall = connected(s, left) && connected(s, right)
                    && !connected(s, flow) && !connected(s, flow.getOpposite());
            return !wall; // pared plana frente al ventilador = bloquea; cruz/individual/T = pasa
        }

        // Losas y escaleras: pasan si su cara hacia el ventilador no tapa el frente completo
        if (b instanceof SlabBlock || b instanceof StairBlock) {
            return !Block.isFaceFull(s.getCollisionShape(level, pos), flow.getOpposite());
        }
        return false;
    }

    private static boolean connected(BlockState s, Direction d) {
        return switch (d) {
            case NORTH -> s.getValue(BlockStateProperties.NORTH);
            case SOUTH -> s.getValue(BlockStateProperties.SOUTH);
            case EAST -> s.getValue(BlockStateProperties.EAST);
            case WEST -> s.getValue(BlockStateProperties.WEST);
            default -> false;
        };
    }

    // ------------------------------------------------------------------ empuje
    public static void pushEntities(Level level, BlockPos fanPos, Direction dir, int reach, boolean golden) {
        AABB box = new AABB(fanPos.relative(dir)).expandTowards(
                dir.getStepX() * (reach - 1), dir.getStepY() * (reach - 1), dir.getStepZ() * (reach - 1));
        List<Entity> list = level.getEntities((Entity) null, box, FanLogic::canPush);
        if (list.isEmpty()) return;

        Vec3 flow = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
        boolean up = dir == Direction.UP;
        double topY = fanPos.getY() + 1 + reach; // borde superior de la corriente (flujo hacia arriba)

        for (Entity e : list) {
            boolean itemLike = e instanceof ItemEntity || e instanceof ExperienceOrb;

            // 1) velocidad objetivo en bloques/s
            double bps;
            if (itemLike) {
                bps = ITEM_BPS * (golden ? GOLDEN_MULT : 1.0) * (up ? UP_ITEM_MULT : 1.0);
            } else if (up) {
                if (!golden) continue; // el ventilador normal no levanta entidades
                bps = ENTITY_BPS * GOLDEN_MULT * UP_ENTITY_MULT;
            } else {
                bps = ENTITY_BPS * (golden ? GOLDEN_MULT : 1.0);
            }
            if (dir.getAxis().isHorizontal() && onHoney(e)) bps *= HONEY_MULT;
            double target = bps / 20.0;

            Vec3 v = e.getDeltaMovement();

            // 2) casos especiales
            if (e instanceof Projectile) {
                // Solo un modificador: la flecha sigue su trayectoria pero se desvia un poco
                if (v.dot(flow) < 0.4) e.setDeltaMovement(v.add(flow.scale(0.015)));
                e.hurtMarked = true;
                continue;
            }
            if (e instanceof AbstractMinecart cart) {
                if (!cartAligned(cart, dir)) continue; // nunca sacar la vagoneta de los rieles
                double along = v.dot(flow);
                if (along < target) cart.setDeltaMovement(v.add(flow.scale(target - along)));
                cart.hurtMarked = true;
                continue;
            }

            // 3) flujo hacia arriba: subir y quedarse flotando/rebotando en el borde
            if (up) {
                double hoverFeetY = topY - 1.2;
                double bob = Math.sin((level.getGameTime() + e.getId() * 7L) * 0.15) * 0.12;
                double vy;
                if (e.getY() < hoverFeetY - 0.6) {
                    vy = target;
                } else {
                    vy = Mth.clamp((hoverFeetY + bob - e.getY()) * 0.3, -target, target);
                }
                if (e instanceof ItemEntity) vy += ITEM_GRAVITY;
                e.setDeltaMovement(v.x, vy, v.z);
                e.fallDistance = 0;
                e.hurtMarked = true;
                continue;
            }

            // 4) flujo horizontal / hacia abajo: subir la componente a lo largo del viento hasta la objetivo
            double along = v.dot(flow);
            if (along < target) {
                e.setDeltaMovement(v.add(flow.scale(target - along)));
                e.hurtMarked = true;
            }
        }
    }

    private static boolean canPush(Entity e) {
        return e.isAlive()
                && !e.isSpectator()
                && !e.isPassenger()
                && !(e instanceof HangingEntity)
                && !e.getType().is(ModTags.FAN_IMMUNE);
    }

    private static boolean onHoney(Entity e) {
        return e.onGround() && e.level().getBlockState(e.getOnPos()).is(Blocks.HONEY_BLOCK);
    }

    private static boolean cartAligned(AbstractMinecart cart, Direction dir) {
        Level level = cart.level();
        BlockPos p = cart.blockPosition();
        BlockState s = level.getBlockState(p);
        if (!(s.getBlock() instanceof BaseRailBlock)) {
            p = p.below();
            s = level.getBlockState(p);
            if (!(s.getBlock() instanceof BaseRailBlock)) return false;
        }
        BaseRailBlock rail = (BaseRailBlock) s.getBlock();
        RailShape shape = s.getValue(rail.getShapeProperty());
        return switch (shape) {
            case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> dir.getAxis() == Direction.Axis.Z;
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> dir.getAxis() == Direction.Axis.X;
            default -> false;
        };
    }
}
