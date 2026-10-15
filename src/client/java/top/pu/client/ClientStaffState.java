/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.client;

import java.util.List;

/**
 * 客户端缓存：创天使法杖注入数据（服务端 StaffSyncS2C 更新），供 HUD 与滚轮切换使用；
 * 法术冷却不在其中（冷却按玩家 + 法术计，随 MANA_SYNC_S2C 同步到 ClientManaState）。
 */
public final class ClientStaffState {

    private static volatile List<String> infusions = List.of();
    private static volatile int selected = -1;

    private ClientStaffState() {
    }

    public static List<String> getInfusions() {
        return infusions;
    }

    public static int getSelected() {
        int size = infusions.size();
        return selected >= 0 && selected < size ? selected : -1;
    }

    /** 更新注入数据（服务端权威）。 */
    public static void set(List<String> newInfusions, int newSelected) {
        infusions = List.copyOf(newInfusions);
        selected = newSelected;
    }
}
