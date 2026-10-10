package com.mdvlcraft.binder.content;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Shows the Astrologer star icon of a race or class in the Origins pickers: its model picks one of the icons
 * by CustomModelData (models/item/origin_icon.json, made by tools/astro_origins.py). Not in any creative tab.
 */
public final class OriginIcon {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "mdvlcraft");
    public static final RegistryObject<Item> ORIGIN_ICON = ITEMS.register("origin_icon", () -> new Item(new Item.Properties().stacksTo(1)));

    private OriginIcon() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
