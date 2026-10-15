/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * 追踪羽毛（翼·第四位阶·天羽倾落）：发射后持续追踪锁定目标，
 * 除非碰墙否则不消失；目标死亡后转为下坠模式直至落地消散。
 */
public class TrackingFeatherEntity extends ProjectileEntity implements FlyingItemEntity {

    /** 飞行速度（格/tick）。 */
    private static final double SPEED = 0.8;
    /** 每 tick 最大转向弧度（追踪灵敏度）。 */
    private static final double TURN_RATE = 0.35;
    /** 追踪目标搜索半径。 */
    private static final double TARGET_RANGE = 24.0;
    /** 单发伤害。 */
    private static final float DAMAGE = 1.5F;
    /** 下坠模式每 tick 增加的重力。 */
    private static final double FALL_GRAVITY = 0.06;
    /** 下坠模式垂直速度上限。 */
    private static final double FALL_TERMINAL = 1.2;

    /** tick 计数（拖尾节奏用）。 */
    private int lifeTicks;
    /** 下坠模式：目标已死亡，不再追踪，受重力下坠直至碰地方块消散。 */
    private boolean falling;
    /** 锁定的目标 UUID（优先追踪该目标，死亡则转下坠）。 */
    private UUID targetUuid;

    public TrackingFeatherEntity(EntityType<? extends TrackingFeatherEntity> type, World world) {
        super(type, world);
    }

    public TrackingFeatherEntity(World world, LivingEntity owner) {
        super(ModEntities.TRACKING_FEATHER, world);
        setOwner(owner);
        this.lifeTicks = 0;
        this.falling = false;
    }

    /**
     * 以玩家为源头发射一列羽毛：按序号做横向错位 + 扇形初始朝向，
     * 避免七枚同位同速完全重叠导致看上去只有一条弹道。
     * lockedTarget：施放时准星锁定的目标（所有羽毛优先追踪它）。
     */
    public static void salvo(LivingEntity owner, int count, UUID lockedTarget) {
        if (owner.getWorld().isClient) {
            return;
        }
        Vec3d look = owner.getRotationVector();
        // 右方向（水平化）与上方向，用于发射点错位
        Vec3d right = look.crossProduct(new Vec3d(0, 1, 0)).normalize();
        Vec3d up = right.crossProduct(look).normalize();
        double middle = (count - 1) / 2.0;
        for (int i = 0; i < count; i++) {
            double spread = (i - middle) / Math.max(1.0, middle); // -1..1
            TrackingFeatherEntity feather = new TrackingFeatherEntity(owner.getWorld(), owner);
            // 发射点横向错位，一眼可见七条弹道
            double ox = owner.getX() + right.x * spread * 0.8 + up.x * Math.abs(spread) * 0.2;
            double oy = owner.getEyeY() - 0.1 + up.y * spread * 0.5;
            double oz = owner.getZ() + right.z * spread * 0.8 + up.z * Math.abs(spread) * 0.2;
            feather.setPosition(ox, oy, oz);
            // 初始速度扇形偏转（绕 Y 轴 ±20°），配合追踪自然散开
            Vec3d dir = rotateY(look, Math.toRadians(spread * 20.0)).normalize().multiply(SPEED);
            feather.setVelocity(dir);
            feather.setTargetUuid(lockedTarget);
            owner.getWorld().spawnEntity(feather);
        }
    }

    /** 绕 Y 轴旋转向量。 */
    private static Vec3d rotateY(Vec3d v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3d(v.x * cos + v.z * sin, v.y, -v.x * sin + v.z * cos);
    }

    /** 设置锁定目标 UUID（施放时由准星确定）。 */
    public void setTargetUuid(UUID uuid) {
        this.targetUuid = uuid;
    }

    /** 渲染用展示贴图（FlyingItemEntity 要求，指向羽毛实体贴图）。 */
    @Override
    public ItemStack getStack() {
        return net.minecraft.item.Items.FEATHER.getDefaultStack();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            // 客户端拖尾粒子
            this.lifeTicks++;
            if (this.lifeTicks % 2 == 0) {
                this.getWorld().addParticle(ParticleTypes.END_ROD,
                        getX(), getY(), getZ(), 0, 0, 0);
            }
            return;
        }

        this.lifeTicks++;

        if (this.falling) {
            tickFalling();
            return;
        }

        // 解析锁定目标；目标死亡/消失则转下坠模式（不消散，直至落地）
        LivingEntity target = resolveTarget();
        if (target == null) {
            this.falling = true;
            return;
        }

        // 追踪转向（限制单 tick 转向角度）
        Vec3d velocity = getVelocity();
        Vec3d toTarget = new Vec3d(
                target.getX() - getX(),
                target.getBodyY(0.5) - getY(),
                target.getZ() - getZ()).normalize().multiply(velocity.length());
        Vec3d dir = velocity.normalize();
        Vec3d want = toTarget.normalize();
        double angle = Math.acos(Math.min(1.0, Math.max(-1.0, dir.dotProduct(want))));
        double t = angle > 1e-4 ? Math.min(1.0, TURN_RATE / angle) : 1.0;
        setVelocity(dir.lerp(want, t).normalize().multiply(SPEED));

        moveAndCollide();
    }

    /** 下坠模式：受重力下坠，水平速度衰减，碰地方块消散。 */
    private void tickFalling() {
        Vec3d velocity = getVelocity();
        double vy = Math.max(velocity.y - FALL_GRAVITY, -FALL_TERMINAL);
        setVelocity(velocity.x * 0.98, vy, velocity.z * 0.98);
        moveAndCollide();
        // 掉出世界兜底
        if (getY() < getWorld().getBottomY() - 16) {
            discard();
        }
    }

    /** 碰撞检测 + 位移；命中实体或方块时由 onEntityHit/onCollision 消散。 */
    private void moveAndCollide() {
        Vec3d velocity = getVelocity();
        HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            onCollision(hit);
            return;
        }
        Vec3d next = getPos().add(velocity);
        setPosition(next.x, next.y, next.z);
    }

    /** 取当前锁定目标（按 UUID 解析，须存活且非观察者），无则返回 null。 */
    private LivingEntity resolveTarget() {
        if (this.targetUuid == null) {
            return findFallbackTarget();
        }
        Entity e = ((ServerWorld) getWorld()).getEntity(this.targetUuid);
        if (e instanceof LivingEntity living && living.isAlive() && !living.isSpectator()) {
            return living;
        }
        return findFallbackTarget();
    }

    /** 锁定目标无效时的兜底：搜索范围内最近的其他有效目标。 */
    private LivingEntity findFallbackTarget() {
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
    protected void onEntityHit(EntityHitResult entityHit) {
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
        return !this.falling;
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.lifeTicks = nbt.getInt("Life");
        this.falling = nbt.getBoolean("Falling");
        if (nbt.containsUuid("Target")) {
            this.targetUuid = nbt.getUuid("Target");
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Life", this.lifeTicks);
        nbt.putBoolean("Falling", this.falling);
        if (this.targetUuid != null) {
            nbt.putUuid("Target", this.targetUuid);
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
