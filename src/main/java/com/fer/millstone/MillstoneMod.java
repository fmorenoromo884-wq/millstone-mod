package com.fer.millstone;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(MillstoneMod.MODID)
public class MillstoneMod {
    public static final String MODID = "millstone";

    public MillstoneMod(IEventBus modBus) {
        ModRegistry.register(modBus);
        modBus.addListener(MillstoneMod::registerCapabilities);
        modBus.addListener(MillstoneMod::addToCreativeTab);
    }

    // Expone el inventario de la Trituradora a tolvas y otros bloques.
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModRegistry.MILLSTONE_BE.get(),
                (be, side) -> be.getAutomationHandler());
    }

    // El bloque aparece en la pestaña "Bloques funcionales" (no es un ítem nuevo, es el item del bloque).
    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModRegistry.MILLSTONE_ITEM.get());
        }
    }
}
