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
import top.pu.mana.ManaManager;

/**
 * 奥术残晶（实体道具）：食用动画完成后直接获得 50 点奥术残晶，仅创造模式获取。
 */
public class ArcaneCrystalItem extends Item {

    /** 食用后获得的残晶数。 */
    private static final int CRYSTAL_AMOUNT = 50;
    /** 食用后的物品冷却（1 秒）。 */
    private static final int USE_COOLDOWN_TICKS = 20;

    public ArcaneCrystalItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof ServerPlayerEntity serverPlayer) {
            ManaManager.addCrystals(serverPlayer, CRYSTAL_AMOUNT);
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
