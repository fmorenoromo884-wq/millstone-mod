package com.fer.millstone;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MillstoneMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MillstoneMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MillstoneMod.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MillstoneMod.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, MillstoneMod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MillstoneMod.MODID);

    // ---- Bloque + su item ----
    public static final DeferredBlock<MillstoneBlock> MILLSTONE_BLOCK = BLOCKS.register("millstone",
            () -> new MillstoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(3.5F)
                    .sound(SoundType.WOOD)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    // No transmite señal de redstone a su alrededor (así no bloquea las tolvas vecinas)
                    .isRedstoneConductor((state, level, pos) -> false)));

    public static final DeferredItem<BlockItem> MILLSTONE_ITEM = ITEMS.registerSimpleBlockItem("millstone", MILLSTONE_BLOCK);

    // ---- Block entity ----
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MillstoneBlockEntity>> MILLSTONE_BE =
            BLOCK_ENTITIES.register("millstone",
                    () -> BlockEntityType.Builder.of(MillstoneBlockEntity::new, MILLSTONE_BLOCK.get()).build(null));

    // ---- Menu (GUI) ----
    public static final DeferredHolder<MenuType<?>, MenuType<MillstoneMenu>> MILLSTONE_MENU =
            MENUS.register("millstone", () -> IMenuTypeExtension.create(MillstoneMenu::new));

    // ---- Recetas de trituración ----
    public static final DeferredHolder<RecipeType<?>, RecipeType<MillstoneRecipe>> CRUSHING_TYPE =
            RECIPE_TYPES.register("crushing", () -> RecipeType.<MillstoneRecipe>simple(
                    ResourceLocation.fromNamespaceAndPath(MillstoneMod.MODID, "crushing")));

    public static final DeferredHolder<RecipeSerializer<?>, MillstoneRecipe.Serializer> CRUSHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("crushing", MillstoneRecipe.Serializer::new);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
    }
}
