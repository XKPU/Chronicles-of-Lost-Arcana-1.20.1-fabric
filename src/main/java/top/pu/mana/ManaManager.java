/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.mana;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import top.pu.item.AngelStaffItem;
import top.pu.network.CoLANetwork;
import top.pu.spell.SpellCooldowns;
import top.pu.spell.Spells;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 魔力管理器：魔力消耗 / 自然恢复（5 秒 1 点）/ 奥术残晶 / 同步 / 跨维度与重生的数据保留。
 */
public final class ManaManager {

    /** 同步间隔（服务器 tick），每 5 tick 同步一次。 */
    public static final int SYNC_INTERVAL_TICKS = 5;

    /** 魔力自然恢复进度：uuid → 已累计 tick 数（内存态，不持久化）。 */
    private static final Map<UUID, Integer> REGEN_ACC = new ConcurrentHashMap<>();

    private ManaManager() {
    }

    /** 当前魔力。 */
    public static int getMana(ServerPlayerEntity player) {
        return ManaComponent.get(player.getServer()).getMana(player.getUuid());
    }

    /**
     * 尝试消耗魔力；不足时返回 false 且不消耗。
     */
    public static boolean tryConsume(ServerPlayerEntity player, int amount) {
        if (amount <= 0) {
            return true;
        }
        ManaComponent state = ManaComponent.get(player.getServer());
        int current = state.getMana(player.getUuid());
        if (current < amount) {
            return false;
        }
        state.setMana(player.getUuid(), current - amount);
        sync(player);
        return true;
    }

    /** 设置魔力（内部/维护用）。 */
    public static void setMana(ServerPlayerEntity player, int value) {
        ManaComponent.get(player.getServer()).setMana(player.getUuid(), value);
        sync(player);
    }

    /** 当前奥术残晶数。 */
    public static int getCrystals(ServerPlayerEntity player) {
        return ManaComponent.get(player.getServer()).getCrystals(player.getUuid());
    }

    /**
     * 增加奥术残晶（击杀掉落等入口），返回增加后的数量。
     */
    public static int addCrystals(ServerPlayerEntity player, int amount) {
        ManaComponent state = ManaComponent.get(player.getServer());
        int value = state.getCrystals(player.getUuid()) + amount;
        state.setCrystals(player.getUuid(), value);
        sync(player);
        return value;
    }

    /**
     * 尝试消耗奥术残晶；不足时返回 false 且不消耗。
     */
    public static boolean tryConsumeCrystals(ServerPlayerEntity player, int amount) {
        if (amount <= 0) {
            return true;
        }
        ManaComponent state = ManaComponent.get(player.getServer());
        int current = state.getCrystals(player.getUuid());
        if (current < amount) {
            return false;
        }
        state.setCrystals(player.getUuid(), current - amount);
        sync(player);
        return true;
    }

    /**
     * 每个服务器 tick 调用：魔力自然恢复 + 按固定间隔把数据同步到客户端（用于 HUD）。
     */
    public static void tickSync(MinecraftServer server) {
        SpellCooldowns.tick(server);
        tickRegen(server);
        if (server.getTicks() % SYNC_INTERVAL_TICKS != 0) {
            return;
        }
        ManaComponent state = ManaComponent.get(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID uuid = player.getUuid();
            sendManaSync(player, state.getMana(uuid), state.getCrystals(uuid));
        }
    }

    /** 魔力自然恢复：每 REGEN_INTERVAL_TICKS ticks 恢复 1 点（满魔力不累计）。 */
    private static void tickRegen(MinecraftServer server) {
        ManaComponent state = ManaComponent.get(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID uuid = player.getUuid();
            if (state.getMana(uuid) >= ManaComponent.MAX_MANA) {
                REGEN_ACC.remove(uuid);
                continue;
            }
            int acc = REGEN_ACC.merge(uuid, 1, Integer::sum);
            if (acc >= ManaComponent.REGEN_INTERVAL_TICKS) {
                REGEN_ACC.remove(uuid);
                state.setMana(uuid, state.getMana(uuid) + 1);
                sendManaSync(player, state.getMana(uuid), state.getCrystals(uuid));
            }
        }
    }

    /** 立即同步魔力与残晶到客户端。 */
    public static void sync(ServerPlayerEntity player) {
        ManaComponent state = ManaComponent.get(player.getServer());
        sendManaSync(player, state.getMana(player.getUuid()), state.getCrystals(player.getUuid()));
    }

    private static void sendManaSync(ServerPlayerEntity player, int mana, int crystals) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(mana);
        buf.writeInt(crystals);
        // 手持创天使法杖时附带选中法术的剩余冷却 ticks（0 = 无冷却），供 HUD 显示
        buf.writeVarInt(getStaffCooldownRemaining(player));
        ServerPlayNetworking.send(player, CoLANetwork.MANA_SYNC_S2C, buf);
    }

    /** 手持创天使法杖时其选中法术的剩余冷却 ticks（0 = 未持法杖/无冷却）。 */
    private static int getStaffCooldownRemaining(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof AngelStaffItem staff)) {
            return 0;
        }
        int selected = AngelStaffItem.getSelected(stack);
        List<String> list = AngelStaffItem.getInfusions(stack);
        if (selected < 0 || selected >= list.size()) {
            return 0;
        }
        return SpellCooldowns.remainingTicks(player, list.get(selected));
    }

    /**
     * respawn（重生/跨维度结束传送）后调用：数据在同一存档的 PersistentState 中，无需复制，
     * 但需要立即同步一次新实体。
     */
    public static void afterRespawn(ServerPlayerEntity newPlayer, boolean alive) {
        REGEN_ACC.remove(newPlayer.getUuid());
        sync(newPlayer);
    }

    /**
     * COPY_FROM（死亡重生/跨维度）时调用：PersistentState 全局按 UUID 存储，
     * 数据天然保留；此方法保留以满足将来改为 per-player 存储的扩展点。
     */
    public static void copyFrom(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        // 魔力存在全局 PersistentState（按 UUID 键），跨实体复制无需操作。
    }

    /**
     * 登入时同步一次。
     */
    public static void onJoin(ServerPlayerEntity player) {
        sync(player);
    }

    /**
     * 给玩家发送魔力不足的 actionbar 柔和提示（中文）。
     */
    public static void sendNotEnoughMana(ServerPlayerEntity player) {
        player.sendMessage(Text.translatable("message.chronicles_of_lost_arcana.mana_not_enough"), true);
    }

    /**
     * 奥术残晶不足提示（actionbar）。
     */
    public static void sendNotEnoughCrystals(ServerPlayerEntity player, int required) {
        player.sendMessage(Text.translatable("message.chronicles_of_lost_arcana.crystals_not_enough", required), true);
    }

    /** 该魔法已注入提示（actionbar）。 */
    public static void sendAlreadyInfused(ServerPlayerEntity player) {
        player.sendMessage(Text.translatable("message.chronicles_of_lost_arcana.already_infused"), true);
    }

    /** 注入槽位已满提示（actionbar）。 */
    public static void sendInfusionsFull(ServerPlayerEntity player) {
        player.sendMessage(Text.translatable("message.chronicles_of_lost_arcana.infusions_full"), true);
    }

    /** 注入成功提示（actionbar）。 */
    public static void sendInfused(ServerPlayerEntity player, String spellId) {
        Spells.byId(spellId).ifPresent(spell -> player.sendMessage(
                Text.translatable("message.chronicles_of_lost_arcana.infused",
                        Text.translatable(spell.nameKey())), true));
    }

    /** 未选中魔法提示（actionbar）。 */
    public static void sendNoSpellSelected(ServerPlayerEntity player) {
        player.sendMessage(Text.translatable("message.chronicles_of_lost_arcana.no_spell_selected"), true);
    }

    /**
     * 释放成功提示（actionbar）。
     */
    public static void sendCastSuccess(ServerPlayerEntity player, String spellNameKey) {
        player.sendMessage(Text.translatable("message.chronicles_of_lost_arcana.cast_success",
                Text.translatable(spellNameKey)), true);
    }
}
