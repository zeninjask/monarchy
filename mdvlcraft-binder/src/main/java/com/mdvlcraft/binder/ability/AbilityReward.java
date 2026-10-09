package com.mdvlcraft.binder.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.json.JsonObject;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

public final class AbilityReward implements Reward {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "ability");
    final Ability ability;
    final int level;

    private AbilityReward(Ability ability, int level) {
        this.ability = ability;
        this.level = level;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, AbilityReward::create);
    }

    private static Result<AbilityReward, Problem> create(RewardConfigContext context) {
        return context.getData().andThen(JsonElement::getAsObject).andThen(AbilityReward::parse);
    }

    private static Result<AbilityReward, Problem> parse(JsonObject data) {
        List<Problem> problems = new ArrayList<>();
        Optional<String> id = data.getString("ability").ifFailure(problems::add).getSuccess();
        Optional<Integer> level = data.getInt("level").ifFailure(problems::add).getSuccess();
        if (!problems.isEmpty()) {
            return Result.failure(Problem.combine(problems));
        } else {
            ResourceLocation abilityId = ResourceLocation.m_135820_(id.orElseThrow());
            Optional<Ability> ability = abilityId == null ? Optional.empty() : Ability.byId(abilityId);
            if (ability.isEmpty()) {
                return Result.failure(Problem.message("Unknown ability `" + id.orElseThrow() + "`"));
            } else {
                int abilityLevel = level.orElseThrow();
                int maxLevel = ability.get() instanceof SpellAbility ? Integer.MAX_VALUE : 1;
                return abilityLevel >= 1 && abilityLevel <= maxLevel
                    ? Result.success(new AbilityReward(ability.get(), abilityLevel))
                    : Result.failure(Problem.message("Level " + abilityLevel + " of `" + abilityId + "` is not allowed"));
            }
        }
    }

    public void update(RewardUpdateContext context) {
        AbilityGrants.set(context.getPlayer(), this, context.getCount() > 0);
    }

    public void dispose(RewardDisposeContext context) {
        AbilityGrants.dispose(this);
    }
}
