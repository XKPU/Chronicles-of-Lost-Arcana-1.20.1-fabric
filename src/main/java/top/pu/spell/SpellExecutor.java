/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import top.pu.mana.ManaManager;

/**
 * 法术执行器：服务端权威执行法术效果。
 */
public final class SpellExecutor {

    /** 翼行效果时长：60 秒 = 1200 ticks。 */
    public static final int WING_FLIGHT_DURATION_TICKS = 1200;

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
                false, false, true));
        player.getWorld().playSound(null, player.getBlockPos(),
                SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.8F, 1.2F);
        ManaManager.sendCastSuccess(player, spell.nameKey());
        return true;
    }

    /**
     * 通过法术 id 执行（供 CastSpellC2S / 法杖右键调用）。
     *
     * @param stack 法杖物品（非 null 时按法术冷却设置玩家法术 CD；残晶仅在注入时消耗）
     */
    public static boolean execute(ServerPlayerEntity player, String spellId, ItemStack stack) {
        Spell spell = Spells.byId(spellId).orElse(null);
        if (spell == null) {
            return false;
        }
        boolean free = stack == null;
        // 法术冷却挂在玩家 + 法术上（与法杖无关）
        if (!free && SpellCooldowns.isCoolingDown(player, spellId)) {
            sendCooldown(player, spell, SpellCooldowns.remainingTicks(player, spellId));
            return false;
        }
        // 残晶仅在注入时消耗，释放法术不消耗
        boolean success = switch (spellId) {
            case "wing_flight" -> executeWingFlight(player, free);
            case "feather_ward" -> executeFeatherWard(player, free);
            case "sky_feather_rain" -> executeSkyFeatherRain(player, free);
            case "beloved_wing" -> executeBelovedWing(player, free);
            case "claw_rend" -> executeClawRend(player, free);
            default -> false;
        };
        if (success && !free && spell.cooldownTicks() > 0) {
            SpellCooldowns.set(player, spellId, spell.cooldownTicks());
        }
        return success;
    }

    /** 兼容旧入口：无物品上下文（正常魔力消耗）。 */
    public static boolean execute(ServerPlayerEntity player, String spellId, boolean free) {
        Spell spell = Spells.byId(spellId).orElse(null);
        if (spell == null) {
            return false;
        }
        if (SpellCooldowns.isCoolingDown(player, spellId)) {
            sendCooldown(player, spell, SpellCooldowns.remainingTicks(player, spellId));
            return false;
        }
        boolean success = switch (spellId) {
            case "wing_flight" -> executeWingFlight(player, free);
            case "feather_ward" -> executeFeatherWard(player, free);
            case "sky_feather_rain" -> executeSkyFeatherRain(player, free);
            case "beloved_wing" -> executeBelovedWing(player, free);
            case "claw_rend" -> executeClawRend(player, free);
            default -> false;
        };
        if (success && spell.cooldownTicks() > 0) {
            SpellCooldowns.set(player, spellId, spell.cooldownTicks());
        }
        return success;
    }

    /** 冷却中提示（actionbar，含剩余秒数）。 */
    private static void sendCooldown(ServerPlayerEntity player, Spell spell, int remainingTicks) {
        int seconds = Math.max(1, (int) Math.ceil(remainingTicks / 20.0));
        player.sendMessage(Text.translatable(
                "message.chronicles_of_lost_arcana.spell_cooldown",
                Text.translatable(spell.nameKey()), seconds), true);
    }

    /**
     * 释放翼·第五位阶·羽庇：自身获得原版伤害吸收 III（十五秒）。
     */
    private static boolean executeFeatherWard(ServerPlayerEntity player, boolean free) {
        Spell spell = Spells.FEATHER_WARD;
        if (!free && !ManaManager.tryConsume(player, spell.manaCost())) {
            ManaManager.sendNotEnoughMana(player);
            return false;
        }
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.ABSORPTION, 20 * 15, 2, false, true, true));
        player.getWorld().playSound(null, player.getBlockPos(),
                SoundEvents.ENTITY_ALLAY_ITEM_GIVEN, SoundCategory.PLAYERS, 0.9F, 0.8F);
        ManaManager.sendCastSuccess(player, spell.nameKey());
        return true;
    }

    /**
     * 释放翼·第四位阶·天羽倾落：向前方发射七枚追踪羽毛（锁定技）。
     */
    private static boolean executeSkyFeatherRain(ServerPlayerEntity player, boolean free) {
        Spell spell = Spells.SKY_FEATHER_RAIN;
        if (!free && !ManaManager.tryConsume(player, spell.manaCost())) {
            ManaManager.sendNotEnoughMana(player);
            return false;
        }
        TrackingFeatherEntity.salvo(player, 7);
        ManaManager.sendCastSuccess(player, spell.nameKey());
        return true;
    }

    /**
     * 释放翼·第六位阶·吾羽翼之所爱：对准星玩家施加原版伤害吸收 II（八秒）。
     */
    private static boolean executeBelovedWing(ServerPlayerEntity player, boolean free) {
        Spell spell = Spells.BELOVED_WING;
        if (!free && !ManaManager.tryConsume(player, spell.manaCost())) {
            ManaManager.sendNotEnoughMana(player);
            return false;
        }
        ServerPlayerEntity target = Targeting.raycastPlayer(player);
        if (target != null) {
            target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.ABSORPTION, 20 * 8, 1, false, true, true));
            target.getWorld().playSound(null, target.getBlockPos(),
                    SoundEvents.ENTITY_ALLAY_ITEM_GIVEN, SoundCategory.PLAYERS, 0.9F, 1.2F);
        }
        ManaManager.sendCastSuccess(player, spell.nameKey());
        return true;
    }

    /**
     * 释放爪·第五位阶·撕裂：前方 4.5 格 90° 锥形范围伤害（4.5 点）+ 爪击特效。
     */
    private static boolean executeClawRend(ServerPlayerEntity player, boolean free) {
        Spell spell = Spells.CLAW_REND;
        if (!free && !ManaManager.tryConsume(player, spell.manaCost())) {
            ManaManager.sendNotEnoughMana(player);
            return false;
        }
        ClawRendEffect.sweep(player, 4.5F, 4.5F, 90.0F);
        ManaManager.sendCastSuccess(player, spell.nameKey());
        return true;
    }
}
