/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;
import top.pu.item.ModItems;

import java.util.HashMap;
import java.util.Map;

/**
 * 创天使法杖宝箱注入：向原版各类宝箱战利品表（loot-api-v2 MODIFY 事件）
 * 追加一个独立概率的法杖池，不影响原有掉落。
 */
public final class ChestLootInjector {

    /** 常规宝箱（村庄/矿穴/沉船/地牢等）：3% 概率。 */
    private static final String[] COMMON_CHESTS = {
            "minecraft:chests/village/village_weaponsmith",
            "minecraft:chests/village/village_temple",
            "minecraft:chests/village/village_toolsmith",
            "minecraft:chests/abandoned_mineshaft",
            "minecraft:chests/shipwreck_treasure",
            "minecraft:chests/simple_dungeon",
            "minecraft:chests/underwater_ruin_small",
            "minecraft:chests/underwater_ruin_big",
            "minecraft:chests/buried_treasure",
            "minecraft:chests/desert_pyramid",
            "minecraft:chests/jungle_temple",
            "minecraft:chests/igloo_chest"
    };

    /** 稀有宝箱（要塞/林地府邸/堡垒遗迹/远古城市等）：5% 概率。 */
    private static final String[] RARE_CHESTS = {
            "minecraft:chests/stronghold_library",
            "minecraft:chests/stronghold_crossing",
            "minecraft:chests/woodland_mansion",
            "minecraft:chests/bastion_treasure",
            "minecraft:chests/ancient_city"
    };

    /** 顶级宝箱（末地城/下界要塞等）：6% 概率。 */
    private static final String[] EPIC_CHESTS = {
            "minecraft:chests/end_city_treasure",
            "minecraft:chests/nether_bridge"
    };

    /** 战利品表 id → 法杖注入概率。 */
    private static final Map<Identifier, Float> CHANCES = new HashMap<>();

    private ChestLootInjector() {
    }

    /** 注册战利品表修改监听（初始化时调用一次）。 */
    public static void init() {
        for (String id : COMMON_CHESTS) {
            CHANCES.put(new Identifier(id), 0.03F);
        }
        for (String id : RARE_CHESTS) {
            CHANCES.put(new Identifier(id), 0.05F);
        }
        for (String id : EPIC_CHESTS) {
            CHANCES.put(new Identifier(id), 0.06F);
        }
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            // 只注入内建（原版/模组自带）表，数据包覆盖的表不处理
            if (!source.isBuiltin()) {
                return;
            }
            Float chance = CHANCES.get(id);
            if (chance == null) {
                return;
            }
            LootPool pool = LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1.0F))
                    .conditionally(RandomChanceLootCondition.builder(chance))
                    .with(ItemEntry.builder(ModItems.ANGEL_STAFF))
                    .build();
            tableBuilder.pool(pool);
        });
    }
}
