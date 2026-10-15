/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.projectile.ProjectileEntity;

/**
 * 法术伤害源工具。
 */
final class DamageSourceUtil {

    private DamageSourceUtil() {
    }

    /**
     * 构造追踪羽毛的飞行物伤害源（算作投射物，归因给施法者）。
     */
    static DamageSource feather(ProjectileEntity projectile) {
        return projectile.getWorld().getDamageSources()
                .create(DamageTypes.ARROW, projectile, projectile.getOwner());
    }
}
