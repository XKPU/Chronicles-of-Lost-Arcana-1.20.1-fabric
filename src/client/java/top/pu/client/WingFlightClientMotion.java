/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import top.pu.network.CoLANetwork;

/**
 * 翼行/缓降的客户端运动逻辑。
 *
 * <p>水平移动复用原版创造飞行模型（服务端授予 allowFlying，客户端按需
 * 开关 abilities.flying）。垂直逻辑：翼行按住空格上升、未按自动缓慢下降；
 * 缓降未按自动下降、按住空格仅允许极轻微上升。</p>
 */
public final class WingFlightClientMotion {

    /** 翼行未按住空格时的滑降速度（自动缓慢下降）。 */
    private static final double GLIDE_FALL_SPEED = -0.08;
    /** 翼行缓降未按住空格时的下降速度。 */
    private static final double SLOW_FALL_SPEED = -0.08;
    /** 翼行缓降按住空格时允许的最大上升速度（防止创造飞行点空格发射）。 */
    private static final double SLOW_FALL_JUMP_RISE_CAP = 0.05;

    private static boolean lastJumping = false;

    private WingFlightClientMotion() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(WingFlightClientMotion::tick);
    }

    private static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            lastJumping = false;
            return;
        }

        boolean hasWingFlight = player.hasStatusEffect(ClientManaState.WING_FLIGHT_EFFECT);
        boolean hasSlowFall = player.hasStatusEffect(ClientManaState.WING_FLIGHT_SLOW_FALL_EFFECT);

        KeyBinding jumpKey = client.options.jumpKey;
        boolean jumping = jumpKey != null && jumpKey.isPressed();

        if (hasWingFlight) {
            applyWingFlightMotion(player, jumping);
        } else if (hasSlowFall) {
            applySlowFallMotion(player, jumping);
        }

        // 空格状态变化上报（仅变化时发送）
        if (jumping != lastJumping) {
            lastJumping = jumping;
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(jumping);
            ClientPlayNetworking.send(CoLANetwork.JUMP_STATE_C2S, buf);
        }
    }

    /**
     * 翼行运动：创造飞行手感 + 修改升降。
     * 地面未按键 → 正常行走（不开飞行）；按空格或已在空中 → 飞行模式。
     * 按住空格 → 原版飞行上升；未按住 → 自动收敛到滑降速度缓慢下降。
     */
    private static void applyWingFlightMotion(ClientPlayerEntity player, boolean jumping) {
        var abilities = player.getAbilities();
        boolean airborne = !player.isOnGround();
        boolean wantFly = jumping || airborne;

        if (wantFly && !abilities.flying) {
            abilities.flying = true; // 服务端已授予 allowFlying
        } else if (!wantFly && abilities.flying) {
            abilities.flying = false;
        }

        if (abilities.flying && !jumping) {
            // 未按住空格：垂直速度收敛到滑降值
            player.setVelocity(player.getVelocity().x, GLIDE_FALL_SPEED, player.getVelocity().z);
        }
        // 按住空格：原版 flying 上升逻辑生效；水平移动交给原版 flying 模型。

        player.fallDistance = 0.0F;
    }

    /**
     * 翼行缓降运动：关闭飞行（若有）；未按住空格自动缓慢下降，按住空格钳制上升上限。
     */
    private static void applySlowFallMotion(ClientPlayerEntity player, boolean jumping) {
        var abilities = player.getAbilities();
        if (abilities.flying) {
            abilities.flying = false;
        }

        double vy = player.getVelocity().y;
        if (jumping) {
            // 按住空格：钳制上升上限
            if (vy > SLOW_FALL_JUMP_RISE_CAP) {
                player.setVelocity(player.getVelocity().x, SLOW_FALL_JUMP_RISE_CAP, player.getVelocity().z);
            }
        } else {
            // 未按住空格：自动缓慢下降
            player.setVelocity(player.getVelocity().x, SLOW_FALL_SPEED, player.getVelocity().z);
        }
        player.fallDistance = 0.0F;
    }
}
