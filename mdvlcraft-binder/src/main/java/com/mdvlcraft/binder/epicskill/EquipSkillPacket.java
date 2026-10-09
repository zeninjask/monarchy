package com.mdvlcraft.binder.epicskill;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent.Context;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillSlot;

public record EquipSkillPacket(int slot, Optional<ResourceLocation> skill) {
    public static void encode(EquipSkillPacket packet, FriendlyByteBuf buf) {
        buf.m_130130_(packet.slot);
        buf.m_236835_(packet.skill, FriendlyByteBuf::m_130085_);
    }

    public static EquipSkillPacket decode(FriendlyByteBuf buf) {
        return new EquipSkillPacket(buf.m_130242_(), buf.m_236860_(FriendlyByteBuf::m_130281_));
    }

    public static void handle(EquipSkillPacket packet, Supplier<Context> context) {
        context.get().enqueueWork(() -> {
            SkillSlot slot = (SkillSlot)SkillSlot.ENUM_MANAGER.get(packet.slot);
            Skill skill = packet.skill.<Skill>map(id -> SkillManager.getSkill(id.toString())).orElse(null);
            if (slot != null && packet.skill.isPresent() == (skill != null)) {
                EpicSkillEquip.request(context.get().getSender(), slot, skill);
            }
        });
        context.get().setPacketHandled(true);
    }
}
