package com.mdvlcraft.binder.ability;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodData;

/** Arcane Sustenance (Scout, Wanderer): pay a lot of mana to fill a little of the hunger bar and its saturation. */
public enum SustenanceAbility implements Ability {
    INSTANCE;

    public static final int MANA_COST = 50;
    public static final int FOOD = 2;
    public static final float SATURATION_MODIFIER = 1.0F;
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "arcane_sustenance");
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/mob_effect/saturation.png");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Component displayName() {
        return Component.translatable("ability.mdvlcraft.arcane_sustenance");
    }

    @Override
    public ResourceLocation icon() {
        return ICON;
    }

    @Override
    public int manaCost(int level) {
        return MANA_COST;
    }

    void cast(ServerPlayer player) {
        FoodData food = player.getFoodData();
        if (food.getFoodLevel() >= 20 && food.getSaturationLevel() >= 20.0F) {
            actionBar(player, Component.translatable("ability.mdvlcraft.arcane_sustenance.full"));
            return;
        }
        MagicData magic = MagicData.getPlayerMagicData(player);
        if (!player.isCreative()) {
            if (magic.getMana() < MANA_COST) {
                actionBar(player, Component.translatable("ui.irons_spellbooks.cast_error_mana", this.displayName()));
                return;
            }
            magic.setMana(magic.getMana() - MANA_COST);
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magic));
        }
        food.eat(FOOD, SATURATION_MODIFIER);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, 1.2F);
    }

    private static void actionBar(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message.copy().withStyle(ChatFormatting.RED)));
    }
}
