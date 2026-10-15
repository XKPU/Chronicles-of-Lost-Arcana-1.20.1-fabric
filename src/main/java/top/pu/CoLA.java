/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.pu.item.ModItems;
import top.pu.mana.ManaManager;
import top.pu.network.CoLAServerNetworking;
import top.pu.network.JumpStateTracker;
import top.pu.spell.ArcaneCrystals;
import top.pu.spell.ModStatusEffects;
import top.pu.spell.WingFlightTickHandler;

/**
 * Chronicles of Lost Arcana 主入口。
 */
public class CoLA implements ModInitializer {

    public static final String MOD_ID = "chronicles_of_lost_arcana";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // 注册：状态效果（翼行 / 翼行缓降）
        ModStatusEffects.init();

        // 注册：物品（两把法杖）与创造模式物品组（魔法）
        ModItems.init();

        // 注册：服务端网络接收器（C2S：cast_spell / jump_state）
        CoLAServerNetworking.register();

        // 注册：翼行效果的服务端 tick 逻辑（含魔力周期同步）
        WingFlightTickHandler.register();

        // 注册：奥术残晶击杀掉落
        ArcaneCrystals.register();

        // 登入时立即同步一次魔力
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ManaManager.onJoin(handler.getPlayer()));

        // 玩家登出时清除空格键状态缓存
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                JumpStateTracker.clear(handler.getPlayer()));

        // 死亡重生/跨维度：魔力数据在全局 PersistentState 中按 UUID 保留，无需复制
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                ManaManager.copyFrom(oldPlayer, newPlayer, alive));

        // 重生后：同步魔力 + 收回飞行能力兜底
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            ManaManager.afterRespawn(newPlayer, alive);
            WingFlightTickHandler.onRespawn(newPlayer);
        });

        LOGGER.info("[CoLA] Magic system initialized: mana, wing flight spell, staves.");
    }
}
