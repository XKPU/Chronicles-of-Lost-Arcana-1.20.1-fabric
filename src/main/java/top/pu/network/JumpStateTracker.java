/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.network;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端记录每个玩家的空格键按下状态（由客户端 JumpStateC2S 包上报，仅在状态变化时发送）。
 * 玩家登出时清除对应状态。
 */
public final class JumpStateTracker {

    private static final Map<UUID, Boolean> JUMPING = new ConcurrentHashMap<>();

    private JumpStateTracker() {
    }

    public static void setJumping(ServerPlayerEntity player, boolean jumping) {
        JUMPING.put(player.getUuid(), jumping);
    }

    public static boolean isJumping(ServerPlayerEntity player) {
        return JUMPING.getOrDefault(player.getUuid(), false);
    }

    public static void clear(ServerPlayerEntity player) {
        JUMPING.remove(player.getUuid());
    }
}
