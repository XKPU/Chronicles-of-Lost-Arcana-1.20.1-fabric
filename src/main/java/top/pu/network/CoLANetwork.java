/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.network;

import net.minecraft.util.Identifier;
import top.pu.CoLA;

/**
 * 网络包频道 Identifier 常量。
 * 仅使用 Identifier，不引用任何仅服务端/仅客户端类，client source set 可安全引用。
 * 按 Fabric Networking API v1（1.20.1）的 per-packet Identifier 方式使用。
 */
public final class CoLANetwork {

    /** S2C：魔力+奥术残晶同步，payload = writeInt(mana) + writeInt(crystals) */
    public static final Identifier MANA_SYNC_S2C =
            new Identifier(CoLA.MOD_ID, "mana_sync");

    /** C2S：释放法术，payload = writeString(spellId) */
    public static final Identifier CAST_SPELL_C2S =
            new Identifier(CoLA.MOD_ID, "cast_spell");

    /** C2S：客户端空格键按下状态（仅在状态变化时发送），payload = writeBoolean(jumping) */
    public static final Identifier JUMP_STATE_C2S =
            new Identifier(CoLA.MOD_ID, "jump_state");

    /** C2S：注入魔法到创天使法杖，payload = writeString(spellId) */
    public static final Identifier INFUSE_SPELL_C2S =
            new Identifier(CoLA.MOD_ID, "infuse_spell");

    /** C2S：切换创天使法杖选中魔法，payload = writeInt(slotIndex) */
    public static final Identifier SELECT_SPELL_C2S =
            new Identifier(CoLA.MOD_ID, "select_spell");

    /** S2C：创天使法杖注入数据同步，payload = writeInt(count) + (writeString(spellId) + writeInt(slot))×count + writeInt(selected) */
    public static final Identifier STAFF_SYNC_S2C =
            new Identifier(CoLA.MOD_ID, "staff_sync");

    private CoLANetwork() {
    }
}
