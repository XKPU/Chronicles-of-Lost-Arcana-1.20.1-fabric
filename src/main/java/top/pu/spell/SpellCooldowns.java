/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 法术冷却：按玩家 + 法术计算（与法杖无关，换法杖不清零）。
 */
public final class SpellCooldowns {

    /** uuid → (spellId → 冷却结束的服务器 tick)。 */
    private static final Map<UUID, Map<String, Long>> END_TICKS = new ConcurrentHashMap<>();

    private SpellCooldowns() {
    }

    /**
     * 该玩家该法术是否在冷却中。
     */
    public static boolean isCoolingDown(ServerPlayerEntity player, String spellId) {
        Map<String, Long> map = END_TICKS.get(player.getUuid());
        if (map == null) {
            return false;
        }
        Long end = map.get(spellId);
        return end != null && end > player.getWorld().getTime();
    }

    /**
     * 冷却剩余 ticks（0 = 无冷却）。
     */
    public static int remainingTicks(ServerPlayerEntity player, String spellId) {
        Map<String, Long> map = END_TICKS.get(player.getUuid());
        if (map == null) {
            return 0;
        }
        Long end = map.get(spellId);
        if (end == null) {
            return 0;
        }
        return (int) Math.max(0, end - player.getWorld().getTime());
    }

    /**
     * 设置冷却（服务端 tick 计时，跨法杖生效）。
     */
    public static void set(ServerPlayerEntity player, String spellId, int cooldownTicks) {
        if (cooldownTicks <= 0) {
            return;
        }
        END_TICKS.computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>())
                .put(spellId, player.getWorld().getTime() + cooldownTicks);
    }

    /**
     * 服务器 tick 驱动：清理过期条目；冷却中且刚进入冷却时提示（用 actionbar）。
     */
    public static void tick(MinecraftServer server) {
        long time = server.getOverworld().getTime();
        for (Map.Entry<UUID, Map<String, Long>> entry : END_TICKS.entrySet()) {
            entry.getValue().values().removeIf(end -> end <= time);
            if (entry.getValue().isEmpty()) {
                END_TICKS.remove(entry.getKey());
            }
        }
    }
}
