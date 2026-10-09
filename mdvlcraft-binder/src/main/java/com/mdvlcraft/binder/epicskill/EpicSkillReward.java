package com.mdvlcraft.binder.epicskill;

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
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.skill.Skill;

public final class EpicSkillReward implements Reward {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "epicfight_skill");
    final Skill skill;

    private EpicSkillReward(Skill skill) {
        this.skill = skill;
    }

    public static void register() {
        SkillsAPI.registerReward(ID, EpicSkillReward::create);
    }

    private static Result<EpicSkillReward, Problem> create(RewardConfigContext context) {
        return context.getData().andThen(JsonElement::getAsObject).andThen(EpicSkillReward::parse);
    }

    private static Result<EpicSkillReward, Problem> parse(JsonObject data) {
        Result<String, Problem> id = data.getString("skill");
        if (id.getFailure().isPresent()) {
            return Result.failure((Problem)id.getFailure().orElseThrow());
        } else {
            String name = (String)id.getSuccess().orElseThrow();
            Skill skill = SkillManager.getSkill(name);
            if (skill == null) {
                return Result.failure(Problem.message("Unknown Epic Fight skill `" + name + "`"));
            } else if (!skill.getCategory().learnable()) {
                return Result.failure(Problem.message("Epic Fight skill `" + name + "` cannot be learned"));
            } else {
                EpicSkillGrants.register(skill);
                return Result.success(new EpicSkillReward(skill));
            }
        }
    }

    public void update(RewardUpdateContext context) {
        EpicSkillGrants.set(context.getPlayer(), this, context.getCount() > 0);
    }

    public void dispose(RewardDisposeContext context) {
        EpicSkillGrants.dispose(this);
    }
}
