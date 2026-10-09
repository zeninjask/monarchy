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
    /**
     * Mods installed only for their code: T.O Magic 'n Extras (whose spells the trees grant) and Alex's
     * Caves (which T.O Magic requires), and Black Bird Manipulation (whose techniques Scout gets). Their items, recipes and advancements are removed, their mobs do
     * not spawn on their own, and their structures and cave biomes are turned off by data and config.
     */
    public static final Set<String> CONTENT_NAMESPACES = Set.of("traveloptics", "alexscaves", "cursedfate_roslon_meimei");
    /** Mods whose mobs never spawn on their own (natural, chunk generation, spawners, structures, ...). */
    private static final Set<String> NO_SPAWN_NAMESPACES = Set.of("cursedfate", "wom", "traveloptics", "alexscaves", "cursedfate_roslon_meimei");
    private static final Set<ResourceLocation> FORBIDDEN_ITEMS = Set.of(
        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "scroll_forge"),
        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "inscription_table"),
        ResourceLocation.fromNamespaceAndPath("cursedfate", "cursed_shards"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "elytra")
    );

    /** Kept in the game but never sold by villagers or wandering traders. */
    private static final Set<ResourceLocation> UNTRADEABLE_ITEMS = Set.of(
        ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "hither_thither_wand")
    );

    private DisabledContent() {
    }

    public static boolean isDisabled(ResourceLocation id) {
        return id != null && NAMESPACES.contains(id.getNamespace());
    }

    public static boolean isDisabled(EntityType<?> type) {
        return isDisabled(ForgeRegistries.ENTITY_TYPES.getKey(type));
    }

    /** Mobs that must not spawn on their own; they can still be summoned by abilities and commands. */
    public static boolean isSpawnBlocked(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return id != null && NO_SPAWN_NAMESPACES.contains(id.getNamespace());
    }

    public static boolean isContentDisabled(ResourceLocation id) {
        return id != null && CONTENT_NAMESPACES.contains(id.getNamespace());
    }

    public static boolean isForbidden(Item item) {
        return isContentDisabled(ForgeRegistries.ITEMS.getKey(item))
            || item instanceof ISpellbook
            || item instanceof IScroll
            || item instanceof EldritchManuscript
            || item instanceof SkillBookItem
            || FORBIDDEN_ITEMS.contains(ForgeRegistries.ITEMS.getKey(item));
    }

    public static boolean isUntradeable(ItemStack stack) {
        return isForbidden(stack) || !stack.isEmpty() && UNTRADEABLE_ITEMS.contains(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    public static boolean isForbidden(ItemStack stack) {
        return !stack.isEmpty() && isForbidden(stack.getItem());
    }

    public static boolean producesForbiddenItem(JsonElement recipe) {
        if (recipe.isJsonObject() && recipe.getAsJsonObject().has("result")) {
            JsonElement result = recipe.getAsJsonObject().get("result");
            if (result.isJsonPrimitive()) {
                return isForbiddenId(ResourceLocation.tryParse(result.getAsString()));
            } else if (result.isJsonObject()) {
                JsonObject object = result.getAsJsonObject();
                String key = object.has("item") ? "item" : "id";
                return object.has(key) && isForbiddenId(ResourceLocation.tryParse(object.get(key).getAsString()));
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
