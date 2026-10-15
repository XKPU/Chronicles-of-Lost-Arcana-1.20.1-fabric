/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * 爪·第五位阶·撕裂：以施法者视角前方一定距离与角度内造成锥形范围伤害 + 爪击特效。
 */
final class ClawRendEffect {

    private static final Random RANDOM = new Random();

    private ClawRendEffect() {
    }

    /**
     * 锥形横扫。
     *
     * @param range     前方判定距离（格）
     * @param damage    每个目标造成的伤害
     * @param halfAngle 锥形半角（度）
     */
    static void sweep(ServerPlayerEntity player, float range, float damage, float halfAngle) {
        ServerWorld world = player.getServerWorld();
        Vec3d origin = player.getPos();
        Vec3d look = player.getRotationVector().normalize();
        double cosLimit = Math.cos(Math.toRadians(halfAngle));

        DamageSource source = world.getDamageSources().create(DamageTypes.PLAYER_ATTACK, player);
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class,
                player.getBoundingBox().expand(range + 1.0), e -> e.isAlive() && e != player)) {
            Vec3d toTarget = target.getPos().subtract(origin);
            if (toTarget.lengthSquared() > (double) (range * range)) {
                continue;
            }
            Vec3d flat = new Vec3d(toTarget.x, 0, toTarget.z).normalize();
            if (flat.dotProduct(new Vec3d(look.x, 0, look.z).normalize()) < cosLimit) {
                continue;
            }
            target.damage(source, damage);
            // 击退：沿视线方向轻微推开
            Vec3d knock = flat.multiply(0.4).add(0, 0.25, 0);
            target.addVelocity(knock.x, knock.y, knock.z);
            target.velocityModified = true;
        }

        // 爪击特效：前方三道爪痕粒子 + 音效
        double spread = Math.toRadians(25);
        for (int claw = -1; claw <= 1; claw++) {
            double yawOffset = claw * spread;
            Vec3d dir = rotateY(look, yawOffset);
            for (int i = 1; i <= 5; i++) {
                double d = range * i / 5.0;
                world.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                        origin.x + dir.x * d,
                        origin.y + 1.0 + (RANDOM.nextDouble() - 0.5) * 0.6,
                        origin.z + dir.z * d,
                        1, 0, 0, 0, 0);
            }
        }
        world.playSound(null, player.getBlockPos(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, 0.7F);
    }

    /** 绕 Y 轴旋转向量。 */
    private static Vec3d rotateY(Vec3d v, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3d(v.x * cos + v.z * sin, v.y, -v.x * sin + v.z * cos);
    }
}
