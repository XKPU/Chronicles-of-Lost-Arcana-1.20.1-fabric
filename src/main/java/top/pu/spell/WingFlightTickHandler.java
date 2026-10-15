/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import top.pu.mana.ManaManager;
import top.pu.network.JumpStateTracker;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 翼行效果的服务端生命周期管理。
 *
 * <p>飞行复用原版创造飞行模型：服务端授予 {@code abilities.allowFlying}，
 * 客户端按需开关 {@code abilities.flying}（见 {@code WingFlightClientMotion}）。
 * 翼行落地不清除，自然到期施加无限时翼行缓降；缓降触地清除。</p>
 */
public final class WingFlightTickHandler {

    /** allowFlying 由本系统授予的玩家集合，效果结束时可靠收回。 */
    private static final java.util.Set<UUID> FLYING_GRANTED = ConcurrentHashMap.newKeySet();

    private WingFlightTickHandler() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ManaManager.tickSync(server);
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tickPlayer(player);
            }
        });
        // 翼行/缓降期间免疫摔落伤害（服务端兜底，客户端另有 fallDistance=0）
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player
                    && source.isOf(DamageTypes.FALL)
                    && (player.hasStatusEffect(ModStatusEffects.WING_FLIGHT)
                    || player.hasStatusEffect(ModStatusEffects.WING_FLIGHT_SLOW_FALL))) {
                return false;
            }
            return true;
        });
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        boolean hasWingFlight = player.hasStatusEffect(ModStatusEffects.WING_FLIGHT);

        if (hasWingFlight) {
            StatusEffectInstance wing = player.getStatusEffect(ModStatusEffects.WING_FLIGHT);
            int remaining = wing.getDuration();

            // 授予飞行能力（客户端按需开关 flying）
            grantFlying(player);

            // 翼行期间不应残留翼行缓降（获得翼行时自动清除缓降）
            if (player.hasStatusEffect(ModStatusEffects.WING_FLIGHT_SLOW_FALL)) {
                player.removeStatusEffect(ModStatusEffects.WING_FLIGHT_SLOW_FALL);
            }

            // 自然到期 → 施加翼行缓降并收回飞行能力
            if (remaining <= 1) {
                player.addStatusEffect(new StatusEffectInstance(
                        ModStatusEffects.WING_FLIGHT_SLOW_FALL,
                        StatusEffectInstance.INFINITE, 0, false, true, true));
                revokeFlying(player);
            }
        } else {
            // 效果已不在：收回本系统授予的飞行能力
            revokeFlying(player);
        }

        // 翼行缓降：触地清除（垂直钳制由客户端执行）
        if (player.hasStatusEffect(ModStatusEffects.WING_FLIGHT_SLOW_FALL)
                && player.isOnGround()) {
            player.removeStatusEffect(ModStatusEffects.WING_FLIGHT_SLOW_FALL);
        }
    }

    /**
     * 授予飞行能力（若本系统尚未授予）。
     */
    private static void grantFlying(ServerPlayerEntity player) {
        if (FLYING_GRANTED.add(player.getUuid()) && !player.getAbilities().allowFlying) {
            player.getAbilities().allowFlying = true;
            player.sendAbilitiesUpdate();
        }
    }

    /**
     * 收回本系统授予的飞行能力。不触碰其他来源（如创造模式）的 allowFlying。
     */
    private static void revokeFlying(ServerPlayerEntity player) {
        if (FLYING_GRANTED.remove(player.getUuid()) && player.getAbilities().allowFlying
                && player.interactionManager.getGameMode().isSurvivalLike()) {
            player.getAbilities().allowFlying = false;
            player.getAbilities().flying = false;
            player.sendAbilitiesUpdate();
        }
    }

    /**
     * 玩家重生后的兜底：收回飞行能力。
     */
    public static void onRespawn(ServerPlayerEntity newPlayer) {
        revokeFlying(newPlayer);
        JumpStateTracker.clear(newPlayer);
    }
}
