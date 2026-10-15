/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.mana;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 魔力与奥术残晶的持久化存储（NBT）。
 * 以一个全局 PersistentState 挂在世界存档（主世界）的 PersistentStateManager 上，
 * 按玩家 UUID 存当前魔力值与残晶数。初始魔力 100 / 最大 100。
 */
public class ManaComponent extends PersistentState {

    public static final int MAX_MANA = 100;
    public static final int DEFAULT_MANA = 100;
    /** 魔力恢复间隔：5 秒 = 100 ticks 恢复 1 点。 */
    public static final int REGEN_INTERVAL_TICKS = 100;

    private static final String DATA_NAME = "chronicles_of_lost_arcana_mana";
    private static final String KEY_ENTRIES = "mana_entries";
    private static final String KEY_CRYSTALS = "arcane_crystals";

    private final Map<UUID, Integer> manaMap = new HashMap<>();
    private final Map<UUID, Integer> crystalMap = new HashMap<>();

    public ManaComponent() {
    }

    /** 从存档 NBT 反序列化。 */
    public static ManaComponent fromNbt(NbtCompound nbt) {
        ManaComponent state = new ManaComponent();
        NbtList list = nbt.getList(KEY_ENTRIES, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            try {
                UUID uuid = UUID.fromString(entry.getString("uuid"));
                state.manaMap.put(uuid, entry.getInt("mana"));
                state.crystalMap.put(uuid, entry.getInt("crystals"));
            } catch (IllegalArgumentException ignored) {
                // 损坏的 UUID 条目直接跳过
            }
        }
        return state;
    }

    /** 获取（或创建）挂在 server 主世界上的魔力状态。 */
    public static ManaComponent get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(ManaComponent::fromNbt, ManaComponent::new, DATA_NAME);
    }

    public int getMana(UUID uuid) {
        return manaMap.getOrDefault(uuid, DEFAULT_MANA);
    }

    public void setMana(UUID uuid, int value) {
        int clamped = Math.max(0, Math.min(MAX_MANA, value));
        manaMap.put(uuid, clamped);
        markDirty();
    }

    /** 当前奥术残晶数（非物品资源，仅创天使法杖可见/可用）。 */
    public int getCrystals(UUID uuid) {
        return crystalMap.getOrDefault(uuid, 0);
    }

    public void setCrystals(UUID uuid, int value) {
        crystalMap.put(uuid, Math.max(0, value));
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Integer> e : manaMap.entrySet()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("uuid", e.getKey().toString());
            entry.putInt("mana", e.getValue());
            entry.putInt("crystals", crystalMap.getOrDefault(e.getKey(), 0));
            list.add(entry);
        }
        nbt.put(KEY_ENTRIES, list);
        return nbt;
    }
}
