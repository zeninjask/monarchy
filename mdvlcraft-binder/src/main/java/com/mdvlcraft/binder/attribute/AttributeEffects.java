package com.mdvlcraft.binder.attribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.RegistryObject;
import yesman.epicfight.skill.dodge.DodgeSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

@EventBusSubscriber(
    modid = "mdvlcraft"
)
public final class AttributeEffects {
    private static final UUID EPIC_FIGHT_LISTENER = UUID.fromString("6f1c3e0a-4d2b-4a8e-9a51-2b7d8f0c9e11");
    private static final int RIPOSTE_WINDOW_TICKS = 60;
    private static final int NATURE_REGEN_INTERVAL_TICKS = 100;
    private static final Map<UUID, Long> RIPOSTE_UNTIL = new HashMap<>();

    private AttributeEffects() {
    }

    private static double value(Player player, RegistryObject<Attribute> attribute) {
        return player.m_21133_((Attribute)attribute.get());
    }

    private static boolean isMelee(DamageSource source, Player attacker) {
        return source.m_276093_(DamageTypes.f_268464_) && source.m_7640_() == attacker;
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (source.m_7639_() instanceof ServerPlayer attacker && isMelee(source, attacker)) {
            event.setAmount(event.getAmount() * (float)(1.0 + meleeBonus(attacker, target)));
        }
    }

    private static double meleeBonus(ServerPlayer attacker, LivingEntity target) {
        CapabilityItem weapon = EpicFightCapabilities.getItemStackCapability(attacker.m_21205_());
        WeaponCategory category = weapon.getWeaponCategory();
        double bonus = 0.0;
        if (category == WeaponCategories.GREATSWORD) {
            bonus += value(attacker, BinderAttributes.GREATSWORD_DAMAGE);
        } else if (category == WeaponCategories.UCHIGATANA || category == WeaponCategories.TACHI) {
            bonus += value(attacker, BinderAttributes.KATANA_DAMAGE);
        } else if (category == WeaponCategories.DAGGER) {
            bonus += value(attacker, BinderAttributes.DAGGER_DAMAGE);
        } else if (category == WeaponCategories.SWORD && attacker.m_21206_().m_41619_()) {
            bonus += value(attacker, BinderAttributes.RAPIER_DAMAGE);
        }

        Vec3 facing = Vec3.m_82498_(0.0F, target.m_146908_());
        Vec3 toAttacker = attacker.m_20182_().m_82546_(target.m_20182_()).m_82542_(1.0, 0.0, 1.0).m_82541_();
        if (facing.m_82526_(toAttacker) < -0.5) {
            bonus += value(attacker, BinderAttributes.BACKSTAB_DAMAGE);
        }

        Long riposteUntil = RIPOSTE_UNTIL.remove(attacker.m_20148_());
        if (riposteUntil != null && riposteUntil >= attacker.m_9236_().m_46467_()) {
            bonus += value(attacker, BinderAttributes.RIPOSTE_DAMAGE);
        }

        return bonus;
    }

    @SubscribeEvent
    public static void onFinishEating(Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FoodProperties var7 = event.getItem().getFoodProperties(player);
            double bonus = value(player, BinderAttributes.SATURATION_BONUS);
            if (var7 != null && bonus > 0.0) {
                FoodData foodData = player.m_36324_();
                float extra = (float)(var7.m_38744_() * var7.m_38745_() * 2.0F * bonus);
                foodData.m_38717_(Math.min(foodData.m_38722_() + extra, (float)foodData.m_38702_()));
            }
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getSource().m_7639_() instanceof ServerPlayer player && !(event.getEntity() instanceof Player) && !event.getDrops().isEmpty()) {
            if (player.m_217043_().m_188500_() < value(player, BinderAttributes.PLUNDER)) {
                List<ItemEntity> drops = List.copyOf(event.getDrops());
                ItemEntity original = drops.get(player.m_217043_().m_188503_(drops.size()));
                event.getDrops()
                    .add(new ItemEntity(original.m_9236_(), original.m_20185_(), original.m_20186_(), original.m_20189_(), original.m_32055_().m_41777_()));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.phase == Phase.END && event.player instanceof ServerPlayer player && player.f_19797_ % 100 == 0) {
            double regen = value(player, BinderAttributes.NATURE_REGEN);
            if (regen > 0.0
                && player.m_20096_()
                && player.m_9236_().m_8055_(player.m_20097_()).m_204336_(BlockTags.f_144274_)
                && player.m_9236_().m_45527_(player.m_20183_())) {
                player.m_5634_((float)regen);
            }
        }
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerPlayerPatch patch = (ServerPlayerPatch)EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
            if (patch == null) {
                throw new IllegalStateException("Epic Fight patch missing on " + player.m_36316_().getName());
            } else {
                patch.getEventListener().addEventListener(EventType.TAKE_DAMAGE_EVENT_ATTACK, EPIC_FIGHT_LISTENER, attack -> {
                    double restore = value(player, BinderAttributes.PARRY_STAMINA);
                    if (attack.isParried() && restore > 0.0) {
                        patch.setStamina((float)Math.min((double)patch.getMaxStamina(), patch.getStamina() + patch.getMaxStamina() * restore));
                    }
                });
                patch.getEventListener().addEventListener(EventType.SKILL_CONSUME_EVENT, EPIC_FIGHT_LISTENER, consume -> {
                    if (consume.getSkill() instanceof DodgeSkill) {
                        consume.setAmount(consume.getAmount() * (float)(1.0 - value(player, BinderAttributes.DODGE_EFFICIENCY)));
                    }
                });
                patch.getEventListener().addEventListener(EventType.DODGE_SUCCESS_EVENT, EPIC_FIGHT_LISTENER, dodge -> {
                    if (value(player, BinderAttributes.RIPOSTE_DAMAGE) > 0.0) {
                        RIPOSTE_UNTIL.put(player.m_20148_(), player.m_9236_().m_46467_() + 60L);
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        RIPOSTE_UNTIL.remove(event.getEntity().m_20148_());
    }
}
