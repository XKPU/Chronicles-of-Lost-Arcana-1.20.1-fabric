/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import top.pu.spell.ModEntities;

/**
 * Chronicles of Lost Arcana 客户端入口。
 */
public class CoLAClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 魔力同步接收 + 空格键状态上报
        CoLAClientNetworking.register();

        // 翼行/缓降客户端运动逻辑（客户端权威，见类注释）
        WingFlightClientMotion.register();

        // 魔力 HUD
        ManaHud.register();

        // 创天使法杖：alt+滚轮切换选中魔法
        StaffScrollHandler.register();

        // 追踪羽毛实体渲染器
        EntityRendererRegistry.register(ModEntities.TRACKING_FEATHER, TrackingFeatherRenderer::new);
    }
}
