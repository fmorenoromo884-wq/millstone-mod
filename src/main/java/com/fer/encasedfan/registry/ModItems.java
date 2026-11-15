package com.fer.encasedfan.registry;

import com.fer.encasedfan.EncasedFan;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EncasedFan.MODID);

    public static final DeferredHolder<Item, BlockItem> FAN = ITEMS.register("encased_fan",
            () -> new BlockItem(ModBlocks.FAN.get(), new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> GOLDEN_FAN = ITEMS.register("encased_golden_fan",
            () -> new BlockItem(ModBlocks.GOLDEN_FAN.get(), new Item.Properties()));
}
