package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.epicskill.EpicSkillEquip;
import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public enum SlotSkillAbility implements Ability {
    MYRIAD_BLADES("myriad_blades", "sword_soaring:wan_jian_gui_zong", 35, "sword_soaring:textures/gui/skills/sword_controller/wan_jian_gui_zong.png"),
    BABYLONIAN_ARMORY("babylonian_armory", "sword_soaring:babylon", 35, "sword_soaring:textures/gui/skills/sword_controller/babylon.png"),
    CELESTIAL_ARRAY("celestial_array", "sword_soaring:rain_sword", 30, "sword_soaring:textures/gui/skills/sword_controller/rain_sword.png"),
    SOUL_HUNT("soul_hunt", "efn:execution", 20, "efn:textures/gui/skills/efn_arts/execution.png");

    private final ResourceLocation id;
    private final String skillId;
    private final int manaCost;
    private final ResourceLocation icon;

    private SlotSkillAbility(String name, String skillId, int manaCost, String icon) {
        this.id = ResourceLocation.fromNamespaceAndPath("mdvlcraft", name);
        this.skillId = skillId;
        this.manaCost = manaCost;
        this.icon = ResourceLocation.parse(icon);
    }

    public static Optional<SlotSkillAbility> byId(ResourceLocation id) {
        return Arrays.stream(values()).filter(a -> a.id.equals(id)).findFirst();
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public Component displayName() {
        return Component.translatable("ability.mdvlcraft." + this.id.getPath());
    }

    @Override
    public ResourceLocation icon() {
        return this.icon;
    }

    @Override
    public int manaCost(int level) {
        return this.manaCost;
    }

    void cast(ServerPlayer player) {
        Skill skill = SkillManager.getSkill(this.skillId);
        if (skill == null) {
            throw new IllegalStateException("Epic Fight skill " + this.skillId + " is not registered");
        } else {
            ServerPlayerPatch patch = (ServerPlayerPatch)EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            SkillSlot slot = SkillSlot.ENUM_MANAGER
                .universalValues()
                .stream()
                .filter(s -> s.category() == skill.getCategory())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No skill slot for " + this.skillId));
            SkillContainer container = patch.getSkill(slot);
            if (container.getSkill() != skill) {
                EpicSkillEquip.apply(patch, container, skill);
            }

            MagicData magic = MagicData.getPlayerMagicData(player);
            if (!player.isCreative() && magic.getMana() < this.manaCost) {
                actionBar(player, Component.translatable("ui.irons_spellbooks.cast_error_mana", new Object[]{this.displayName()}));
            } else if (!skill.canExecute(container)) {
                actionBar(player, Component.translatable("ability.mdvlcraft.cannot_cast", new Object[]{this.displayName()}));
            } else {
                if (!player.isCreative()) {
                    magic.setMana(magic.getMana() - this.manaCost);
                    PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
                }

                skill.executeOnServer(container, new FriendlyByteBuf(Unpooled.buffer()));
            }
        }
    }

    private static void actionBar(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message.copy().withStyle(ChatFormatting.RED)));
    }
}
