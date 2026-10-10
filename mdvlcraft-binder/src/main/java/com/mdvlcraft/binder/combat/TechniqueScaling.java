package com.mdvlcraft.binder.combat;

import com.mdvlcraft.binder.ability.MonkArts;
import com.mdvlcraft.binder.config.BinderConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Cursed Fate techniques (Blitz, Surprise Attack, Dismantle, ...) deal fixed damage of their own.
 * Scale it by the caster's melee damage (held weapon, Strength, Sharpness and so on) relative to a
 * reference weapon, an iron sword by default.
 */
@EventBusSubscriber(
    modid = "mdvlcraft"
)
public final class TechniqueScaling {
    private TechniqueScaling() {
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer caster
            && event.getSource().typeHolder().unwrapKey().map(key -> key.location().getNamespace().equals("cursedfate")).orElse(false)
            && !MonkArts.artActive(caster)) {  // the Monk's arts scale with the Monk's punch instead
            event.setAmount(event.getAmount() * factor(caster, event));
        }
    }

    private static float factor(ServerPlayer caster, LivingHurtEvent event) {
        double reference = BinderConfig.techniqueReferenceDamage();
        if (reference <= 0.0) {
            return 1.0F;
        }
        double damage = caster.getAttributeValue(Attributes.ATTACK_DAMAGE)
            + EnchantmentHelper.getDamageBonus(caster.getMainHandItem(), event.getEntity().getMobType());
        return (float)Math.max(BinderConfig.techniqueMinimumFactor(), damage / reference);
    }
}
