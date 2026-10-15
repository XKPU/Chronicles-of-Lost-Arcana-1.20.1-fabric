/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.server.network.ServerPlayerEntity;
import top.pu.mana.ManaManager;

import java.util.Random;

/**
 * 奥术残晶：非物品资源，仅击杀敌对生物（Monster）稀有掉落、
 * Boss（末影龙/凋灵）必掉；动物、村民等非敌对生物与玩家不掉落。
 * 仅在创天使法杖的注入/UI 中可见与消耗。
 */
public final class ArcaneCrystals {

    /** 基础掉落概率：敌对生物 8%。 */
    private static final double BASE_CHANCE = 0.08;
    /** 精英（大体型/重甲）敌对生物的额外概率加成。 */
    private static final double ELITE_BONUS = 0.07;
    /** Boss（末影龙/凋灵）必掉数量。 */
    private static final int BOSS_DROP = 12;

    private static final Random RANDOM = new Random();

    private ArcaneCrystals() {
    }

    /** 注册击杀掉落监听。 */
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(ArcaneCrystals::onDeath);
    }

    /**
     * 击杀掉落判定：敌对生物 8% 概率掉 1 个（大体型/重甲 15%），
     * Boss（末影龙/凋灵）必掉 12 个。
     */
    private static void onDeath(LivingEntity entity, net.minecraft.entity.damage.DamageSource source) {
        if (entity.getWorld().isClient) {
            return;
        }
        LivingEntity killer = entity.getAttacker();
        if (!(killer instanceof ServerPlayerEntity player)) {
            return;
        }
        if (entity instanceof EnderDragonEntity || entity instanceof WitherEntity) {
            ManaManager.addCrystals(player, BOSS_DROP);
            return;
        }
        if (!(entity instanceof Monster)) {
            return;
        }
        double chance = BASE_CHANCE + eliteBonus(entity);
        if (RANDOM.nextDouble() < chance) {
            ManaManager.addCrystals(player, 1);
        }
    }

    /** 精英判定：大体型或重甲（护甲值 ≥ 10）给额外概率。 */
    private static double eliteBonus(LivingEntity entity) {
        boolean big = entity.getWidth() >= 1.2F || entity.getHeight() >= 2.5F;
        boolean armored = entity.getArmor() >= 10.0F;
        return big || armored ? ELITE_BONUS : 0.0;
    }
}
