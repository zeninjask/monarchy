package com.mdvlcraft.binder.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

final class WeaponReport {
    private WeaponReport() {
    }

    static List<WeaponReport.Row> build() {
        Map<Item, String> dataTypes = new HashMap<>();
        ItemCapabilityReloadListener.getWeaponDataStream().forEach(tag -> {
            if (tag.m_128441_("type")) {
                dataTypes.put(Item.m_41445_(tag.m_128451_("id")), tag.m_128461_("type"));
            }
        });
        List<WeaponReport.Row> rows = new ArrayList<>();
        ForgeRegistries.ITEMS
            .getEntries()
            .forEach(
                entry -> {
                    Item item = (Item)entry.getValue();
                    if (isWeaponLike(item)) {
                        CapabilityItem capability = EpicFightCapabilities.getItemStackCapability(new ItemStack(item));
                        boolean supported = capability != null && !capability.isEmpty();
                        rows.add(
                            new WeaponReport.Row(
                                ((ResourceKey)entry.getKey()).m_135782_(),
                                item.getClass().getName(),
                                supported ? String.valueOf(capability.getWeaponCategory()) : "-",
                                dataTypes.getOrDefault(item, "-"),
                                supported
                            )
                        );
                    }
                }
            );
        return rows;
    }

    private static boolean isWeaponLike(Item item) {
        return item instanceof BlockItem
            ? false
            : item instanceof TieredItem
                || item instanceof TridentItem
                || item instanceof ProjectileWeaponItem
                || item instanceof ShieldItem
                || item.m_7167_(EquipmentSlot.MAINHAND).containsKey(Attributes.f_22281_);
    }

    record Row(ResourceLocation id, String itemClass, String category, String weaponType, boolean supported) {
    }
}
