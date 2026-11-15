package com.fer.encasedfan.registry;

import com.fer.encasedfan.EncasedFan;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

public class ModTags {
    /** Entidades que el ventilador nunca empuja (warden, ravager, etc.). Editable por datapack. */
    public static final TagKey<EntityType<?>> FAN_IMMUNE =
            TagKey.create(Registries.ENTITY_TYPE, EncasedFan.id("fan_immune"));

    /** Bloques que dejan pasar la corriente aunque esten justo enfrente. Editable por datapack. */
    public static final TagKey<Block> AIR_PASSABLE =
            TagKey.create(Registries.BLOCK, EncasedFan.id("air_passable"));
}
