/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

/**
 * 锁定技目标选取。
 */
final class Targeting {

    /** 锁定最大距离。 */
    private static final double LOCK_RANGE = 64.0;

    private Targeting() {
    }

    /**
     * 取玩家准星指向的玩家（视线射线检测），无则返回 null。
     */
    static ServerPlayerEntity raycastPlayer(ServerPlayerEntity player) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVector().multiply(LOCK_RANGE));
        EntityHitResult hit = net.minecraft.entity.projectile.ProjectileUtil.raycast(
                player, eye, end, player.getBoundingBox().expand(LOCK_RANGE),
                e -> e instanceof PlayerEntity && e.isAlive() && e != player,
                LOCK_RANGE * LOCK_RANGE);
        return hit != null && hit.getEntity() instanceof ServerPlayerEntity target ? target : null;
    }
}
