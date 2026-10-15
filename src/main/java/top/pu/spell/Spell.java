/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

/**
 * 法术记录：id、名称、魔力消耗、冷却、奥术残晶需求。
 *
 * @param cooldownTicks 冷却（ticks），0 = 无冷却
 * @param crystalCost   注入/释放所需奥术残晶数
 */
public record Spell(String id, String nameKey, int manaCost, int cooldownTicks,
                    int crystalCost, String descKey) {

    /** 法术名称的翻译 key。 */
    public String translationKey() {
        return nameKey;
    }
}
