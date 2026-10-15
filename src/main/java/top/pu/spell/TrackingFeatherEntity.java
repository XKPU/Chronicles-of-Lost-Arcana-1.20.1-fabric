/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 追踪羽毛（翼·第四位阶·天羽倾落）：发射后追踪最近的有效目标，
 * 按飞行物结算伤害，可被地形拦截。
 */
public class TrackingFeatherEntity extends ProjectileEntity {

    /** 飞行速度（格/tick）。 */
    private static final double SPEED = 0.8;
    /** 每 tick 最大转向弧度（追踪灵敏度）。 */
    private static final double TURN_RATE = 0.35;
    /** 追踪目标搜索半径。 */
    private static final double TARGET_RANGE = 24.0;
    /** 单发伤害。 */
    private static final float DAMAGE = 1.5F;
    /** 最长存活时间（tick），防呆。 */
    private static final int LIFETIME_TICKS = 100;

    private int lifeTicks;

    public TrackingFeatherEntity(EntityType<? extends TrackingFeatherEntity> type, World world) {
        super(type, world);
    }

    public TrackingFeatherEntity(World world, LivingEntity owner) {
        super(ModEntities.TRACKING_FEATHER, world);
        setOwner(owner);
        setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        setVelocity(owner.getRotationVector().multiply(SPEED));
        this.lifeTicks = 0;
    }

    /** 以玩家为源头发射一列羽毛（扇形散布）。 */
    public static void salvo(LivingEntity owner, int count) {
        if (owner.getWorld().isClient) {
            return;
        }
        for (int i = 0; i < count; i++) {
            TrackingFeatherEntity feather = new TrackingFeatherEntity(owner.getWorld(), owner);
            owner.getWorld().spawnEntity(feather);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            // 客户端拖尾粒子
            this.getWorld().addParticle(ParticleTypes.END_ROD,
                    getX(), getY(), getZ(), 0, 0, 0);
            return;
        }

        if (++this.lifeTicks > LIFETIME_TICKS) {
            discard();
            return;
        }

        // 每 tick 重新锁定最近目标（无目标则直线飞行）
        LivingEntity target = findTarget();
        Vec3d velocity = getVelocity();
        if (target != null) {
            Vec3d toTarget = new Vec3d(
                    target.getX() - getX(),
                    target.getBodyY(0.5) - getY(),
                    target.getZ() - getZ()).normalize().multiply(velocity.length());
            // 限制单 tick 转向角度，保留少量不可取消的惯性
            Vec3d dir = velocity.normalize();
            Vec3d want = toTarget.normalize();
            double angle = Math.acos(Math.min(1.0, dir.dotProduct(want)));
            double t = angle > 1e-4 ? Math.min(1.0, TURN_RATE / angle) : 1.0;
            velocity = dir.lerp(want, t).normalize().multiply(SPEED);
            setVelocity(velocity);
        }

        // 地形拦截与实体命中（ projectileHitCallback 会调用 onEntityHit/onCollision ）
        HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            onCollision(hit);
            return;
        }

        Vec3d next = getPos().add(velocity);
        setPosition(next.x, next.y, next.z);
    }

    /** 搜索最近的可伤害目标（非 owner、非队友简化为：任何其他生物）。 */
    private LivingEntity findTarget() {
        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        LivingEntity best = null;
        double bestDist = TARGET_RANGE * TARGET_RANGE;
        for (Entity e : getWorld().getOtherEntities(this, getBoundingBox().expand(TARGET_RANGE))) {
            if (!(e instanceof LivingEntity living) || !living.isAlive()
                    || living.isSpectator() || e == owner) {
                continue;
            }
            double dist = squaredDistanceTo(e);
            if (dist < bestDist) {
                bestDist = dist;
                best = living;
            }
        }
        return best;
    }

    @Override
    protected void onEntityHit(net.minecraft.util.hit.EntityHitResult entityHit) {
        super.onEntityHit(entityHit);
        if (getWorld() instanceof ServerWorld
                && entityHit.getEntity() instanceof LivingEntity living && living.isAlive()) {
            DamageSource source = DamageSourceUtil.feather(this);
            living.damage(source, DAMAGE);
        }
        discard();
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (!getWorld().isClient) {
            // 地形拦截或其他碰撞：消散
            discard();
        }
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.lifeTicks = nbt.getInt("Life");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Life", this.lifeTicks);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
