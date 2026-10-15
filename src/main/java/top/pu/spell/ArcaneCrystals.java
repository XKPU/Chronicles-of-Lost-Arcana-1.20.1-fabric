/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import top.pu.mana.ManaManager;

import java.util.Random;

/**
 * 奥术残晶：非物品资源，击杀怪物时掉落（每只 0.3~2 个取整），
 * 仅在创天使法杖的注入/UI 中可见与消耗。
 */
public final class ArcaneCrystals {

    /** 掉落概率的最小粒度（1/1000）。 */
    private static final double MIN_CHANCE = 0.3;
    private static final double MAX_CHANCE = 2.0;

    private static final Random RANDOM = new Random();

    private ArcaneCrystals() {
    }

    /** 注册击杀掉落监听。 */
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(ArcaneCrystals::onDeath);
    }

    /** 怪物死亡：击杀者为玩家时按 0.3~2 的期望掉落残晶（非物品，直接入账）。 */
    private static void onDeath(LivingEntity entity, net.minecraft.entity.damage.DamageSource source) {
        if (entity.getWorld().isClient) {
            return;
        }
        if (entity instanceof ServerPlayerEntity) {
            // 玩家死亡不掉落
            return;
        }
        LivingEntity killer = entity.getAttacker();
        if (!(killer instanceof ServerPlayerEntity player)) {
            return;
        }
        double roll = MIN_CHANCE + RANDOM.nextDouble() * (MAX_CHANCE - MIN_CHANCE);
        int amount = (int) roll;
        // 小数部分按概率进位：0.3 期望 = 30% 掉 1 个
        if (RANDOM.nextDouble() < roll - amount) {
            amount++;
        }
        if (amount > 0) {
            ManaManager.addCrystals(player, amount);
        }
    }
}
