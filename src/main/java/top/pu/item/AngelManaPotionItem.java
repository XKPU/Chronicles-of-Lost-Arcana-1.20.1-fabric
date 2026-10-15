/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import top.pu.mana.ManaComponent;
import top.pu.mana.ManaManager;

/**
 * 创天使魔力药水：食用动画完成后立刻恢复 50 点魔力（不超过上限），仅创造模式获取。
 */
public class AngelManaPotionItem extends Item {

    /** 食用后恢复的魔力数。 */
    private static final int MANA_RESTORE = 50;
    /** 食用后的物品冷却（1 秒）。 */
    private static final int USE_COOLDOWN_TICKS = 20;

    public AngelManaPotionItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof ServerPlayerEntity serverPlayer) {
            int current = ManaManager.getMana(serverPlayer);
            int restored = Math.min(current + MANA_RESTORE, ManaComponent.MAX_MANA);
            ManaManager.setMana(serverPlayer, restored);
            serverPlayer.getItemCooldownManager().set(this, USE_COOLDOWN_TICKS);
            if (!serverPlayer.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }
        return stack;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        // 交给父类（食物组件）启动食用动画
        return super.use(world, user, hand);
    }
}
