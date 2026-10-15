/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import top.pu.network.CoLANetwork;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端网络：
 * <ul>
 *   <li>接收 S2C 魔力+残晶同步 → 缓存到 {@link ClientManaState}；</li>
 *   <li>接收 S2C 法杖注入数据 → 缓存到 {@link ClientStaffState}，openEditor 时打开注入 UI；</li>
 *   <li>空格键状态上报与 alt+滚轮切换由 {@link StaffScrollHandler} 处理。</li>
 * </ul>
 */
public final class CoLAClientNetworking {

    private CoLAClientNetworking() {
    }

    public static void register() {
        // S2C：魔力 + 奥术残晶同步
        ClientPlayNetworking.registerGlobalReceiver(CoLANetwork.MANA_SYNC_S2C,
                (client, handler, buf, responseSender) -> {
                    int mana = buf.readInt();
                    int crystals = buf.readInt();
                    client.execute(() -> {
                        ClientManaState.setMana(mana);
                        ClientManaState.setCrystals(crystals);
                    });
                });

        // S2C：法杖注入数据（openEditor = true 时打开注入 UI）
        ClientPlayNetworking.registerGlobalReceiver(CoLANetwork.STAFF_SYNC_S2C,
                (client, handler, buf, responseSender) -> {
                    List<String> list = readInfusions(buf);
                    int selected = buf.readVarInt();
                    int cooldownTicks = buf.readVarInt();
                    boolean openEditor = buf.readBoolean();
                    client.execute(() -> {
                        ClientStaffState.set(list, selected, cooldownTicks);
                        if (openEditor) {
                            openEditorScreen(client, list, selected);
                        }
                    });
                });
    }

    private static List<String> readInfusions(PacketByteBuf buf) {
        int count = buf.readVarInt();
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(buf.readString(256));
        }
        return list;
    }

    private static void openEditorScreen(MinecraftClient client, List<String> infusions, int selected) {
        if (client.currentScreen == null) {
            client.setScreen(new StaffInfusionScreen(infusions, selected));
        }
    }
}
