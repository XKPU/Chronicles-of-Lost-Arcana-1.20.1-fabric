/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import top.pu.CoLA;

/**
 * 状态效果注册。
 *
 * <p>注意（1.20.1 yarn）：StatusEffect 构造器是 protected，需以子类实例化。
 * 效果图标默认按 {@code assets/<modid>/textures/mob_effect/<path>.png} 绘制。</p>
 */
public final class ModStatusEffects {

    /** 翼行：60 秒，按住空格上升，松开缓慢下降。 */
    public static final StatusEffect WING_FLIGHT = register("wing_flight",
            new SimpleStatusEffect(StatusEffectCategory.BENEFICIAL, 0x87CEEB));

    /** 翼行缓降：翼行自然到期（未落地）时自动施加，触地/死亡清除。 */
    public static final StatusEffect WING_FLIGHT_SLOW_FALL = register("wing_flight_slow_fall",
            new SimpleStatusEffect(StatusEffectCategory.BENEFICIAL, 0xB0E0E6));

    /** 最小 StatusEffect 子类：仅暴露 protected 构造器，无附加行为。 */
    private static final class SimpleStatusEffect extends StatusEffect {
        private SimpleStatusEffect(StatusEffectCategory category, int color) {
            super(category, color);
        }
    }

    private static StatusEffect register(String path, StatusEffect effect) {
        return Registry.register(Registries.STATUS_EFFECT,
                new Identifier(CoLA.MOD_ID, path), effect);
    }

    public static void init() {
        // 类加载时完成注册；显式空方法供 CoLA.onInitialize 调用。
    }

    private ModStatusEffects() {
    }
}
