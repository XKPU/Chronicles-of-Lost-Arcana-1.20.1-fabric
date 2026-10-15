/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import top.pu.network.CoLANetwork;

/**
 * 手持创天使法杖时 alt+滚轮切换选中魔法（C2S 上报，服务端权威回发同步）。
 */
public final class StaffScrollHandler {

    /** 上一帧 alt 是否按下（沿检测）。 */
    private static boolean prevAlt = false;
    /** 滚轮累积（1 格 = 120 单位）。 */
    private static double scrollAcc = 0;

    private StaffScrollHandler() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(StaffScrollHandler::onTick);
    }

    private static void onTick(MinecraftClient client) {
        boolean alt = hasAltDown();
        if (client.player == null || client.currentScreen != null) {
            prevAlt = alt;
            scrollAcc = 0;
            return;
        }
        // 仅手持创天使法杖时生效
        if (!(client.player.getMainHandStack().getItem() instanceof top.pu.item.AngelStaffItem)) {
            scrollAcc = 0;
            prevAlt = alt;
            return;
        }

        // 鼠标滚轮增量（由 CoLAMouseMixin 镜像，1 格 = 120）
        double scroll = takeScroll();
        if (alt && scroll != 0) {
            int dir = scroll > 0 ? -1 : 1; // 滚轮向上 = 上一项（与物品栏热键方向一致）
            int current = ClientStaffState.getSelected();
            int size = ClientStaffState.getInfusions().size();
            if (size > 0) {
                int next = ((current < 0 ? 0 : current) + dir + size) % size;
                sendSelect(next);
            }
        }
        prevAlt = alt;
    }

    private static double takeScroll() {
        // 滚轮增量由 CoLAMouseMixin 镜像到 CoLAMouseHooks
        double v = CoLAMouseHooks.consumeScroll();
        if (v != 0) {
            scrollAcc += v;
            double steps = Math.floor(scrollAcc / 120.0);
            if (steps != 0) {
                scrollAcc -= steps * 120.0;
                return steps;
            }
        }
        return 0;
    }

    private static boolean hasAltDown() {
        return net.minecraft.client.util.InputUtil.isKeyPressed(
                MinecraftClient.getInstance().getWindow().getHandle(),
                org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT)
                || net.minecraft.client.util.InputUtil.isKeyPressed(
                MinecraftClient.getInstance().getWindow().getHandle(),
                org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT);
    }

    private static void sendSelect(int slot) {
        PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeVarInt(slot);
        ClientPlayNetworking.send(CoLANetwork.SELECT_SPELL_C2S, buf);
    }
}
