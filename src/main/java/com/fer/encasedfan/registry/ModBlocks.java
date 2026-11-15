package com.fer.encasedfan.registry;

import com.fer.encasedfan.EncasedFan;
import com.fer.encasedfan.block.FanBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EncasedFan.MODID);

    public static final DeferredBlock<FanBlock> FAN = BLOCKS.registerBlock("encased_fan",
            props -> new FanBlock(props, false),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                    .sound(SoundType.METAL).noOcclusion());

    public static final DeferredBlock<FanBlock> GOLDEN_FAN = BLOCKS.registerBlock("encased_golden_fan",
            props -> new FanBlock(props, true),
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0F, 6.0F)
                    .sound(SoundType.METAL).noOcclusion());
}
