/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.client;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import top.pu.CoLA;

/**
 * 客户端缓存：当前魔力值（由 ManaSyncS2C 包更新），供 HUD 渲染；
 * 以及翼行相关状态效果的客户端引用（客户端运动逻辑用）。
 */
public final class ClientManaState {

    /** 翼行效果（客户端按 registry id 查找，服务端注册后再查）。 */
    public static final StatusEffect WING_FLIGHT_EFFECT = RegistryEntryHelper.get("wing_flight");
    /** 翼行缓降效果。 */
    public static final StatusEffect WING_FLIGHT_SLOW_FALL_EFFECT = RegistryEntryHelper.get("wing_flight_slow_fall");

    private static volatile int mana = 0;
    private static volatile int crystals = 0;
    private static volatile boolean received = false;
    /** 手持创天使法杖时选中法术的剩余冷却 ticks（0 = 无冷却）。 */
    private static volatile int staffCooldownTicks = 0;
    /** 收到同步时的客户端世界时间，用于在同步间隔内插值。 */
    private static volatile long staffCooldownSyncTime = 0;

    private ClientManaState() {
    }

    public static int getMana() {
        return mana;
    }

    public static int getCrystals() {
        return crystals;
    }

    public static void setMana(int value) {
        mana = value;
        received = true;
    }

    /** 同步奥术残晶数（非物品资源，仅创天使法杖 UI 可见）。 */
    public static void setCrystals(int value) {
        crystals = value;
    }

    /** 同步法杖选中法术的剩余冷却 ticks（服务端权威）。 */
    public static void setStaffCooldown(int ticks) {
        staffCooldownTicks = Math.max(0, ticks);
        staffCooldownSyncTime = System.nanoTime();
    }

    /**
     * 当前剩余冷却 ticks：以最近同步值为基准按真实时间递减（同步间隔 5 tick 内平滑）。
     */
    public static int getStaffCooldownTicks() {
        int base = staffCooldownTicks;
        if (base <= 0) {
            return 0;
        }
        long elapsedNanos = System.nanoTime() - staffCooldownSyncTime;
        long elapsedTicks = elapsedNanos / 50_000_000L;
        return (int) Math.max(0, base - elapsedTicks);
    }

    /** 是否已收到过至少一次同步（避免首包前渲染无意义 HUD）。 */
    public static boolean hasReceived() {
        return received;
    }
}

/** 按 id 从注册表查效果（延迟到类加载时执行，服务端注册先于客户端查询）。 */
final class RegistryEntryHelper {
    private RegistryEntryHelper() {
    }

    static StatusEffect get(String path) {
        // 客户端单机/集成服务器时服务端注册已完成；若未注册则返回 null（逻辑里判空）。
        return Registries.STATUS_EFFECT.containsId(new Identifier(CoLA.MOD_ID, path))
                ? Registries.STATUS_EFFECT.get(new Identifier(CoLA.MOD_ID, path))
                : null;
    }
}
