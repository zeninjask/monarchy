package com.mdvlcraft.binder.client;

import com.mdvlcraft.binder.mixin.puffish.SkillsClientModAccessor;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.puffish.skillsmod.client.SkillsClientMod;
import net.puffish.skillsmod.client.data.ClientCategoryData;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes;

/**
 * The character status screen, laid out like Elden Ring's Status page and drawn in the skill trees' Astrologer
 * look: archetype level and experience, the important attributes, base stats, the damage per second of every
 * weapon on the hotbar, and how much each kind of hit the player's armour takes off.
 */
public class CharacterScreen extends Screen {
    private static final int W = 440;
    private static final int H = 254;
    private static final ResourceLocation SKY = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/skills/astro_sky.png");
    private static final ResourceLocation ASTROLABE = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "textures/gui/character/astrolabe.png");
    private static final ResourceLocation CLASSES = ResourceLocation.fromNamespaceAndPath("mdvlcraft", "classes");
    private static final int NAVY = 0xF0121734;
    private static final int GOLD = 0xFFAA8C50;
    private static final int GOLD_FAINT = 0x60AA8C50;
    private static final int HEADING = 0xFFE0C27A;
    private static final int LABEL = 0xFFD6E4FF;
    private static final int VALUE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8C9AC4;
    private static final float REFERENCE_HIT = 10.0F;  // defence is shown as the share of a 10-damage hit it stops

    private final Screen parent;

    public CharacterScreen(Screen parent) {
        super(Component.translatable("screen.mdvlcraft.character"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        float scale = this.scale();
        int right = Math.round((this.width + W * scale) / 2.0F);
        int bottom = Math.round((this.height + H * scale) / 2.0F);
        this.addRenderableWidget(new AstroGui.Button(right - 66, bottom - 18, 56, 14, Component.translatable("screen.mdvlcraft.menu.back"),
            () -> Minecraft.getInstance().setScreen(this.parent)));
    }

    private float scale() {
        return Math.min(1.0F, Math.min((this.width - 16) / (float) W, (this.height - 16) / (float) H));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (AbilityKeys.MENU.matches(key, scan)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float scale = this.scale();
        g.pose().pushPose();
        g.pose().translate((this.width - W * scale) / 2.0F, (this.height - H * scale) / 2.0F, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        this.panel(g);
        Font font = this.font;
        g.drawString(font, "✦ " + Component.translatable("screen.mdvlcraft.character").getString(), 10, 8, HEADING, false);
        g.drawString(font, player.getName(), 22, 26, VALUE, false);
        g.fill(10, 37, W - 10, 38, GOLD_FAINT);

        int left = 18;
        int mid = 160;
        int right = 302;
        int colW = 124;
        // level and experience
        Optional<ClientCategoryData> archetype = highestArchetype();
        int y = 46;
        y = row(g, left, colW, y, Component.translatable("screen.mdvlcraft.character.level"),
            archetype.map(a -> String.valueOf(a.getCurrentLevel())).orElse("n/a"));
        y = row(g, left, colW, y, Component.translatable("screen.mdvlcraft.character.archetype"),
            archetype.map(a -> a.getConfig().title().getString()).orElse("n/a"));
        y += 6;
        y = row(g, left, colW, y, Component.translatable("screen.mdvlcraft.character.experience"),
            archetype.map(a -> String.valueOf(a.getCurrentExperience())).orElse("n/a"));
        y = row(g, left, colW, y, Component.translatable("screen.mdvlcraft.character.next_level"),
            archetype.map(a -> String.valueOf(a.getExperienceToNextLevel())).orElse("n/a"));
        // attributes
        y += 14;
        y = heading(g, left, y, "screen.mdvlcraft.character.attributes");
        y = attr(g, player, left, colW, y, "minecraft:generic.attack_damage", v -> fixed(v, 1));
        y = attr(g, player, left, colW, y, "minecraft:generic.attack_speed", v -> fixed(v, 2));
        y = attr(g, player, left, colW, y, "attributeslib:crit_chance", CharacterScreen::percent);
        y = attr(g, player, left, colW, y, "attributeslib:crit_damage", CharacterScreen::percent);
        y = attr(g, player, left, colW, y, "attributeslib:life_steal", CharacterScreen::percent);
        y = attr(g, player, left, colW, y, "irons_spellbooks:spell_power", CharacterScreen::percent);
        y = attr(g, player, left, colW, y, "irons_spellbooks:cooldown_reduction", v -> signedPercent(v - 1.0));
        y = attr(g, player, left, colW, y, "irons_spellbooks:mana_regen", CharacterScreen::percent);
        attr(g, player, left, colW, y, "minecraft:generic.movement_speed", v -> percent(v / 0.1));

        // base stats
        y = 46;
        y = heading(g, mid, y, "screen.mdvlcraft.character.base_stats");
        y = row(g, mid, colW, y, Component.translatable("screen.mdvlcraft.character.hp"),
            fixed(player.getHealth(), 0) + " / " + fixed(player.getMaxHealth(), 0));
        y = row(g, mid, colW, y, Component.translatable("screen.mdvlcraft.character.mana"),
            ClientMagicData.getPlayerMana() + " / " + fixed(player.getAttributeValue(AttributeRegistry.MAX_MANA.get()), 0));
        LocalPlayerPatch patch = EpicFightCapabilities.getLocalPlayerPatch(player);
        y = row(g, mid, colW, y, Component.translatable("screen.mdvlcraft.character.stamina"),
            patch == null ? "–" : fixed(patch.getStamina(), 0) + " / " + fixed(patch.getMaxStamina(), 0));
        y += 6;
        y = row(g, mid, colW, y, Component.translatable("screen.mdvlcraft.character.poise"),
            fixed(player.getAttributeValue(EpicFightAttributes.STUN_ARMOR.get()), 1));
        row(g, mid, colW, y, Component.translatable("screen.mdvlcraft.character.luck"), fixed(player.getAttributeValue(Attributes.LUCK), 1));
        g.blit(ASTROLABE, mid - 6, 118, 0, 0, 132, 132, 132, 132);

        // attack power: damage per second of each weapon on the hotbar
        y = 46;
        y = heading(g, right, y, "screen.mdvlcraft.character.attack_power");
        int weapons = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).isEmpty()) {
                continue;
            }
            double damage = valueWith(player, Attributes.ATTACK_DAMAGE, stack);
            double speed = valueWith(player, Attributes.ATTACK_SPEED, stack);
            String name = (slot + 1) + "  " + stack.getHoverName().getString();
            y = row(g, right, colW, y, Component.literal(this.font.plainSubstrByWidth(name, colW - 34)), fixed(damage * speed, 1));
            weapons++;
        }
        if (weapons == 0) {
            g.drawString(font, Component.translatable("screen.mdvlcraft.character.no_weapons"), right + 4, y, MUTED, false);
            y += 11;
        }
        // defence and damage negation against a reference hit
        y += 8;
        y = heading(g, right, y, "screen.mdvlcraft.character.defence");
        double armor = player.getAttributeValue(Attributes.ARMOR);
        double toughness = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        y = row(g, right, colW, y, Component.translatable("screen.mdvlcraft.character.physical"),
            fixed(armor, 1) + "  /  " + negation(player, player.damageSources().mobAttack(player)));
        y = row(g, right, colW, y, Component.translatable("screen.mdvlcraft.character.toughness"), fixed(toughness, 1));
        y = row(g, right, colW, y, Component.translatable("screen.mdvlcraft.character.projectile"),
            negation(player, player.damageSources().arrow(null, null)));
        y = row(g, right, colW, y, Component.translatable("screen.mdvlcraft.character.fire"), negation(player, player.damageSources().onFire()));
        y = row(g, right, colW, y, Component.translatable("screen.mdvlcraft.character.blast"),
            negation(player, player.damageSources().explosion(null, null)));
        y = row(g, right, colW, y, Component.translatable("screen.mdvlcraft.character.fall"), negation(player, player.damageSources().fall()));
        y = attr(g, player, right, colW, y, "minecraft:generic.knockback_resistance", CharacterScreen::percent);
        attr(g, player, right, colW, y, "attributeslib:dodge_chance", CharacterScreen::percent);

        g.fill(10, H - 22, W - 10, H - 21, GOLD_FAINT);
        g.drawString(font, Component.translatable("screen.mdvlcraft.character.footer", AbilityKeys.MENU.getTranslatedKeyMessage()),
            14, H - 15, MUTED, false);
        g.pose().popPose();
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** The night-sky panel with a gold frame, as on the skill tree screen. */
    private void panel(GuiGraphics g) {
        g.fill(0, 0, W, H, NAVY);
        for (int x = 0; x < W; x += 128) {
            for (int y = 0; y < H; y += 128) {
                g.blit(SKY, x, y, 0, 0, Math.min(128, W - x), Math.min(128, H - y), 128, 128);
            }
        }
        g.fill(0, 0, W, 1, GOLD);
        g.fill(0, H - 1, W, H, GOLD);
        g.fill(0, 0, 1, H, GOLD);
        g.fill(W - 1, 0, W, H, GOLD);
        g.fill(3, 3, W - 3, 4, GOLD_FAINT);
        g.fill(3, H - 4, W - 3, H - 3, GOLD_FAINT);
        g.fill(3, 3, 4, H - 3, GOLD_FAINT);
        g.fill(W - 4, 3, W - 3, H - 3, GOLD_FAINT);
        g.fill(150, 46, 151, H - 30, GOLD_FAINT);
        g.fill(292, 46, 293, H - 30, GOLD_FAINT);
    }

    private int heading(GuiGraphics g, int x, int y, String key) {
        g.drawString(this.font, "✧ " + Component.translatable(key).getString(), x - 4, y, HEADING, false);
        return y + 13;
    }

    private int row(GuiGraphics g, int x, int width, int y, Component label, String value) {
        int room = width - 4 - this.font.width(value) - 4;
        String text = label.getString();
        if (this.font.width(text) > room) {
            text = this.font.plainSubstrByWidth(text, room - this.font.width(".")) + ".";
        }
        g.drawString(this.font, text, x + 4, y, LABEL, false);
        g.drawString(this.font, value, x + width - this.font.width(value), y, VALUE, false);
        return y + 11;
    }

    private int attr(GuiGraphics g, LocalPlayer player, int x, int width, int y, String id, DoubleFunction<String> format) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.parse(id));
        if (attribute == null || player.getAttribute(attribute) == null) {
            return y;
        }
        String shortName = "screen.mdvlcraft.character.short." + ResourceLocation.parse(id).getPath();
        Component label = I18n.exists(shortName) ? Component.translatable(shortName) : Component.translatable(attribute.getDescriptionId());
        return row(g, x, width, y, label, format.apply(player.getAttributeValue(attribute)));
    }

    /** The highest-level archetype the player has picked (its tab is visible), if any. */
    private static Optional<ClientCategoryData> highestArchetype() {
        return ((SkillsClientModAccessor) (Object) SkillsClientMod.getInstance()).mdvlcraft$screenData().streamCategories()
            .filter(c -> !c.getConfig().id().equals(CLASSES) && c.hasExperience())
            .max(Comparator.comparingInt(ClientCategoryData::getCurrentLevel).thenComparingInt(ClientCategoryData::getCurrentExperience));
    }

    /** An attribute's value if the given item were in the main hand instead of the held one. */
    private static double valueWith(LocalPlayer player, Attribute attribute, ItemStack stack) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return 0.0;
        }
        Set<UUID> held = new HashSet<>();
        player.getMainHandItem().getAttributeModifiers(EquipmentSlot.MAINHAND).get(attribute).forEach(m -> held.add(m.getId()));
        List<AttributeModifier> modifiers = new ArrayList<>();
        instance.getModifiers().stream().filter(m -> !held.contains(m.getId())).forEach(modifiers::add);
        Collection<AttributeModifier> item = stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(attribute);
        modifiers.addAll(item);
        double base = instance.getBaseValue();
        for (AttributeModifier m : modifiers) {
            if (m.getOperation() == AttributeModifier.Operation.ADDITION) {
                base += m.getAmount();
            }
        }
        double value = base;
        for (AttributeModifier m : modifiers) {
            if (m.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) {
                value += base * m.getAmount();
            }
        }
        for (AttributeModifier m : modifiers) {
            if (m.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) {
                value *= 1.0 + m.getAmount();
            }
        }
        return attribute.sanitizeValue(value);
    }

    /** The share of a reference hit of this kind that armour and protection enchantments stop. */
    private static String negation(LocalPlayer player, DamageSource source) {
        float armor = (float) player.getAttributeValue(Attributes.ARMOR);
        float toughness = (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float amount = REFERENCE_HIT;
        if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            amount = afterArmor(player, source, amount, armor, toughness);
        }
        int protection = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
        if (protection > 0 && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            amount = afterProtection(player, source, amount, protection);
        }
        return percent(1.0 - amount / REFERENCE_HIT);
    }

    // Apothic Attributes replaces the armour and protection formulas; use its rules when it is present
    private static Method alArmor;
    private static Method alProtection;
    private static boolean alLooked;

    private static void lookUpApothic() {
        if (alLooked) {
            return;
        }
        alLooked = true;
        try {
            Class<?> rules = Class.forName("dev.shadowsoffire.attributeslib.api.ALCombatRules");
            alArmor = rules.getMethod("getDamageAfterArmor", net.minecraft.world.entity.LivingEntity.class, DamageSource.class, float.class, float.class, float.class);
            alProtection = rules.getMethod("getDamageAfterProtection", net.minecraft.world.entity.LivingEntity.class, DamageSource.class, float.class, float.class);
        } catch (ReflectiveOperationException e) {
            alArmor = null;
            alProtection = null;
        }
    }

    private static float afterArmor(LocalPlayer player, DamageSource source, float amount, float armor, float toughness) {
        lookUpApothic();
        try {
            if (alArmor != null) {
                return (float) alArmor.invoke(null, player, source, amount, armor, toughness);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return CombatRules.getDamageAfterAbsorb(amount, armor, toughness);
    }

    private static float afterProtection(LocalPlayer player, DamageSource source, float amount, int protection) {
        lookUpApothic();
        try {
            if (alProtection != null) {
                return (float) alProtection.invoke(null, player, source, amount, (float) protection);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return CombatRules.getDamageAfterMagicAbsorb(amount, protection);
    }

    private static String fixed(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    private static String percent(double fraction) {
        return fixed(fraction * 100.0, fraction * 100.0 % 1.0 == 0.0 ? 0 : 1) + "%";
    }

    private static String signedPercent(double fraction) {
        return (fraction > 0 ? "+" : "") + percent(fraction);
    }
}
