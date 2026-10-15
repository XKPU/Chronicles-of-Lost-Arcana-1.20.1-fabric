/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import top.pu.CoLA;

/**
 * 物品与创造模式物品组注册。
 * 创天使法杖：maxCount(1)、Rarity.EPIC、仅创造模式获取（无合成/掉落/战利品表）。
 */
public final class ModItems {

    /** 创天使法杖。 */
    public static final Item ANGEL_STAFF = register("angel_staff",
            new AngelStaffItem(new Item.Settings()
                    .maxCount(1)
                    .rarity(Rarity.EPIC)));

    /** 自定义创造模式物品组：魔法（图标用法杖）。 */
    public static final ItemGroup MAGIC_GROUP = FabricItemGroup.builder()
                    .displayName(Text.translatable("itemgroup.chronicles_of_lost_arcana.magic"))
                    .icon(() -> new ItemStack(ANGEL_STAFF))
                    .entries((context, entries) -> entries.add(ANGEL_STAFF))
                    .build();

    private static Item register(String path, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(CoLA.MOD_ID, path), item);
    }

    public static void init() {
        // 注册物品组（物品已在静态初始化中注册）
        Registry.register(Registries.ITEM_GROUP, new Identifier(CoLA.MOD_ID, "magic"), MAGIC_GROUP);
    }

    private ModItems() {
    }
}
