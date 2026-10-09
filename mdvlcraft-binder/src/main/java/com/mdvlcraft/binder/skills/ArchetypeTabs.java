package com.mdvlcraft.binder.skills;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.Skill.State;

@EventBusSubscriber(
    modid = "mdvlcraft"
)
public final class ArchetypeTabs {
    public static final ResourceLocation CLASSES = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "classes");
    private static final List<String> CLASS_SKILLS = List.of("warrior", "mage", "rogue", "ranger");

    private ArchetypeTabs() {
    }

    public static void register() {
        SkillsAPI.registerSkillUnlockEvent((player, categoryId, skillId) -> archetypeTab(categoryId, skillId).ifPresent(tab -> tab.unlock(player)));
        SkillsAPI.registerSkillLockEvent((player, categoryId, skillId) -> archetypeTab(categoryId, skillId).ifPresent(tab -> tab.erase(player)));
    }

    private static Optional<Category> archetypeTab(ResourceLocation categoryId, String skillId) {
        return categoryId.equals(CLASSES) && !CLASS_SKILLS.contains(skillId)
            ? Optional.of(
                (Category)SkillsAPI.getCategory(ResourceLocation.fromNamespaceAndPath("mdvlcraft", skillId))
                    .orElseThrow(() -> new IllegalStateException("Archetype " + skillId + " has no skill tab"))
            )
            : Optional.empty();
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer)event.getEntity();
        Category classes = (Category)SkillsAPI.getCategory(CLASSES).orElseThrow(() -> new IllegalStateException("Skill tab " + CLASSES + " is missing"));

        for (String classSkill : CLASS_SKILLS) {
            Skill skill = (Skill)classes.getSkill(classSkill).orElseThrow(() -> new IllegalStateException("Class skill " + classSkill + " is missing"));
            if (skill.getState(player) != State.UNLOCKED) {
                skill.unlock(player);
            }
        }
    }
}
