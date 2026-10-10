package com.mdvlcraft.binder.skills;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Experience;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;

/**
 * The menu's Respec page, server side. Resetting an archetype's tree locks all its skills, which hands the points
 * back to spend again. Changing class swaps one picked archetype for another: the old one's node on the Classes tab
 * is locked (which wipes its tab, see {@link ArchetypeTabs}), the new one's is unlocked, and the new tab starts at
 * the old one's level less a quarter (rounded down), its points free to spend.
 */
public final class Respec {
    /** The archetypes in skill-tree order (also the order the class names are keyed by). */
    public static final List<String> ARCHETYPES = List.of("knight", "samurai", "berzerker", "monk", "holy", "flame", "lightning",
        "necromancy", "ice", "water", "assassin", "phantom", "scout", "thief", "wanderer", "stalker", "druid", "cursed");

    private Respec() {
    }

    public static int keptLevel(int level) {
        return level * 3 / 4;
    }

    public static void reset(ServerPlayer player, String archetype) {
        if (!isPicked(player, archetype)) {
            return;
        }
        tab(archetype).ifPresent(tab -> {
            tab.resetSkills(player);
            player.displayClientMessage(Component.translatable("message.mdvlcraft.respec.reset", title(archetype)), false);
            tab.openScreen(player);
        });
    }

    public static void change(ServerPlayer player, String from, String to) {
        if (!isPicked(player, from) || !ARCHETYPES.contains(to) || isPicked(player, to)) {
            return;
        }
        Optional<Category> classes = SkillsAPI.getCategory(ArchetypeTabs.CLASSES);
        Optional<Category> oldTab = tab(from);
        Optional<Category> newTab = tab(to);
        Optional<Skill> oldNode = classes.flatMap(c -> c.getSkill(from));
        Optional<Skill> newNode = classes.flatMap(c -> c.getSkill(to));
        if (oldTab.isEmpty() || newTab.isEmpty() || oldNode.isEmpty() || newNode.isEmpty()) {
            return;
        }
        int level = keptLevel(oldTab.get().getExperience().map(e -> e.getLevel(player)).orElse(0));
        oldNode.get().lock(player);   // wipes the old tab
        newTab.get().erase(player);   // a clean new tab, whatever was left from before
        newNode.get().unlock(player); // shows the new tab
        newTab.get().getExperience().ifPresent(e -> e.setLevel(player, level));
        player.displayClientMessage(Component.translatable("message.mdvlcraft.respec.changed", title(from), title(to), level), false);
        newTab.get().openScreen(player);
    }

    private static boolean isPicked(ServerPlayer player, String archetype) {
        return ARCHETYPES.contains(archetype) && SkillsAPI.getCategory(ArchetypeTabs.CLASSES)
            .flatMap(c -> c.getSkill(archetype))
            .map(s -> s.getState(player) == Skill.State.UNLOCKED)
            .orElse(false);
    }

    private static Optional<Category> tab(String archetype) {
        return SkillsAPI.getCategory(ResourceLocation.fromNamespaceAndPath("mdvlcraft", archetype));
    }

    private static String title(String archetype) {
        return Character.toUpperCase(archetype.charAt(0)) + archetype.substring(1);
    }
}
