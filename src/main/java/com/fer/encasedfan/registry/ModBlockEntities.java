package com.fer.encasedfan.registry;

import com.fer.encasedfan.EncasedFan;
import com.fer.encasedfan.block.FanBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, EncasedFan.MODID);

    public static final Supplier<BlockEntityType<FanBlockEntity>> FAN = TYPES.register("fan",
            () -> BlockEntityType.Builder.of(FanBlockEntity::new,
                    ModBlocks.FAN.get(), ModBlocks.GOLDEN_FAN.get()).build(null));
}
