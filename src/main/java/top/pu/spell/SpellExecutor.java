/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import top.pu.item.AngelStaffItem;
import top.pu.mana.ManaManager;

/**
 * 法术执行器：服务端权威执行法术效果。
 */
public final class SpellExecutor {

    /** 翼行效果时长：60 秒 = 1200 ticks。 */
    public static final int WING_FLIGHT_DURATION_TICKS = 1200;
    /** 法杖使用冷却：1 秒。 */
    public static final int STAFF_COOLDOWN_TICKS = 20;

    private SpellExecutor() {
    }

    /**
     * 释放翼行。
     *
     * @param free  true = 不消耗魔力与残晶（免耗入口）
     */
    public static boolean executeWingFlight(ServerPlayerEntity player, boolean free) {
        Spell spell = Spells.WING_FLIGHT;
        if (!free) {
            if (!ManaManager.tryConsume(player, spell.manaCost())) {
                ManaManager.sendNotEnoughMana(player);
                return false;
            }
        }
        // 施加翼行效果（覆盖旧的同名效果，重新计时）
        player.addStatusEffect(new StatusEffectInstance(
                ModStatusEffects.WING_FLIGHT, WING_FLIGHT_DURATION_TICKS, 0,
                false, true, true));
        player.getWorld().playSound(null, player.getBlockPos(),
                SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.8F, 1.2F);
        ManaManager.sendCastSuccess(player, spell.nameKey());
        return true;
    }

    /**
     * 通过法术 id 执行（供 CastSpellC2S / 法杖右键调用）。
     *
     * @param stack 法杖物品（非 null 时校验残晶需求并按法术冷却设置物品 CD）
     */
    public static boolean execute(ServerPlayerEntity player, String spellId, ItemStack stack) {
        Spell spell = Spells.byId(spellId).orElse(null);
        if (spell == null) {
            return false;
        }
        boolean free = stack == null;
        // 法杖释放：校验奥术残晶需求
        if (!free && stack.getItem() instanceof AngelStaffItem
                && !ManaManager.tryConsumeCrystals(player, spell.crystalCost())) {
            ManaManager.sendNotEnoughCrystals(player, spell.crystalCost());
            return false;
        }
        boolean success = switch (spellId) {
            case "wing_flight" -> executeWingFlight(player, free);
            default -> false;
        };
        if (success && stack != null && spell.cooldownTicks() > 0) {
            // 法术冷却挂在该物品上（1.20.1 冷却管理器位于玩家）
            player.getItemCooldownManager().set(stack.getItem(), spell.cooldownTicks());
        }
        return success;
    }

    /** 兼容旧入口：无物品上下文（正常魔力消耗）。 */
    public static boolean execute(ServerPlayerEntity player, String spellId, boolean free) {
        return switch (spellId) {
            case "wing_flight" -> executeWingFlight(player, free);
            default -> false;
        };
    }
}
