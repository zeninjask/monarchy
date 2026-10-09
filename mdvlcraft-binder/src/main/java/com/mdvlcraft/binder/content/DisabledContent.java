package com.mdvlcraft.binder.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.redspace.ironsspellbooks.api.item.IScroll;
import io.redspace.ironsspellbooks.api.item.ISpellbook;
import io.redspace.ironsspellbooks.item.EldritchManuscript;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.world.item.SkillBookItem;

public final class DisabledContent {
    public static final Set<String> NAMESPACES = Set.of("irons_spellbooks", "cursedfate");
    private static final Set<ResourceLocation> FORBIDDEN_ITEMS = Set.of(
        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "scroll_forge"),
        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "inscription_table"),
        ResourceLocation.fromNamespaceAndPath("cursedfate", "cursed_shards"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "elytra")
    );

    private DisabledContent() {
    }

    public static boolean isDisabled(ResourceLocation id) {
        return id != null && NAMESPACES.contains(id.m_135827_());
    }

    public static boolean isDisabled(EntityType<?> type) {
        return isDisabled(ForgeRegistries.ENTITY_TYPES.getKey(type));
    }

    public static boolean isForbidden(Item item) {
        return item instanceof ISpellbook
            || item instanceof IScroll
            || item instanceof EldritchManuscript
            || item instanceof SkillBookItem
            || FORBIDDEN_ITEMS.contains(ForgeRegistries.ITEMS.getKey(item));
    }

    public static boolean isForbidden(ItemStack stack) {
        return !stack.m_41619_() && isForbidden(stack.m_41720_());
    }

    public static boolean producesForbiddenItem(JsonElement recipe) {
        if (recipe.isJsonObject() && recipe.getAsJsonObject().has("result")) {
            JsonElement result = recipe.getAsJsonObject().get("result");
            if (result.isJsonPrimitive()) {
                return isForbiddenId(ResourceLocation.m_135820_(result.getAsString()));
            } else if (result.isJsonObject()) {
                JsonObject object = result.getAsJsonObject();
                String key = object.has("item") ? "item" : "id";
                return object.has(key) && isForbiddenId(ResourceLocation.m_135820_(object.get(key).getAsString()));
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static boolean isForbiddenId(ResourceLocation id) {
        return id != null && ForgeRegistries.ITEMS.containsKey(id) && isForbidden((Item)ForgeRegistries.ITEMS.getValue(id));
    }
}
