/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.client;

import java.util.List;

/**
 * 客户端缓存：创天使法杖注入数据（服务端 StaffSyncS2C 更新），供 HUD 与滚轮切换使用。
 */
public final class ClientStaffState {

    private static volatile List<String> infusions = List.of();
    private static volatile int selected = -1;
    /** 当前选中法术的总冷却 ticks（0 = 无冷却）。 */
    private static volatile int cooldownTicks = 0;

    private ClientStaffState() {
    }

    public static List<String> getInfusions() {
        return infusions;
    }

    public static int getSelected() {
        int size = infusions.size();
        return selected >= 0 && selected < size ? selected : -1;
    }

    public static int getSelectedCooldownTicks() {
        return cooldownTicks;
    }

    /** 更新注入数据（服务端权威）。 */
    public static void set(List<String> newInfusions, int newSelected) {
        set(newInfusions, newSelected, 0);
    }

    /** 更新注入数据与选中法术冷却（服务端权威）。 */
    public static void set(List<String> newInfusions, int newSelected, int newCooldownTicks) {
        infusions = List.copyOf(newInfusions);
        selected = newSelected;
        cooldownTicks = Math.max(0, newCooldownTicks);
    }
}
