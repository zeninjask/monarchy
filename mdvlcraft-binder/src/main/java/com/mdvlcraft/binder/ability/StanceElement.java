package com.mdvlcraft.binder.ability;

import com.mdvlcraft.binder.attribute.BinderAttributes;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public enum StanceElement {
    FIRE(16734740, () -> Attributes.f_22283_, 0.15, Operation.MULTIPLY_TOTAL, "4b9b3c1e-6f2a-4a8e-9d61-0f1b6a5c1001"),
    WIND(11075538, BinderAttributes.DODGE_DISTANCE, 0.4, Operation.ADDITION, "4b9b3c1e-6f2a-4a8e-9d61-0f1b6a5c1002"),
    WATER(3049215, BinderAttributes.PARRY_WINDOW, 0.5, Operation.ADDITION, "4b9b3c1e-6f2a-4a8e-9d61-0f1b6a5c1003"),
    EARTH(11565614, () -> Attributes.f_22281_, 0.15, Operation.MULTIPLY_TOTAL, "4b9b3c1e-6f2a-4a8e-9d61-0f1b6a5c1004");

    public final int colour;
    final Supplier<Attribute> attribute;
    final AttributeModifier modifier;

    private StanceElement(int colour, Supplier<Attribute> attribute, double amount, Operation operation, String uuid) {
        this.colour = colour;
        this.attribute = attribute;
        this.modifier = new AttributeModifier(UUID.fromString(uuid), "mdvlcraft:stance_" + this.name().toLowerCase(), amount, operation);
    }

    public StanceElement next() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    public Component displayName() {
        return Component.m_237115_("stance.mdvlcraft." + this.name().toLowerCase()).m_130938_(style -> style.m_131148_(TextColor.m_131266_(this.colour)));
    }

    public Component effect() {
        return Component.m_237115_("stance.mdvlcraft." + this.name().toLowerCase() + ".effect");
    }
}
