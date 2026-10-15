/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import top.pu.item.AngelStaffItem;
import top.pu.mana.ManaManager;
import top.pu.spell.SpellExecutor;
import top.pu.spell.Spells;

/**
 * 服务端网络接收器注册（Fabric Networking API v1，per-packet Identifier，1.20.1 五参签名）。
 */
public final class CoLAServerNetworking {

    private CoLAServerNetworking() {
    }

    public static void register() {
        // C2S：释放法术（走魔力消耗）
        ServerPlayNetworking.registerGlobalReceiver(CoLANetwork.CAST_SPELL_C2S,
                (server, player, handler, buf, responseSender) -> {
                    String spellId = buf.readString(256);
                    server.execute(() -> handleCastSpell(player, spellId));
                });

        // C2S：空格键状态（仅状态变化时发送）
        ServerPlayNetworking.registerGlobalReceiver(CoLANetwork.JUMP_STATE_C2S,
                (server, player, handler, buf, responseSender) -> {
                    boolean jumping = buf.readBoolean();
                    server.execute(() -> JumpStateTracker.setJumping(player, jumping));
                });

        // C2S：注入魔法到创天使法杖
        ServerPlayNetworking.registerGlobalReceiver(CoLANetwork.INFUSE_SPELL_C2S,
                (server, player, handler, buf, responseSender) -> {
                    String spellId = buf.readString(256);
                    server.execute(() -> handleInfuse(player, spellId));
                });

        // C2S：替换创天使法杖指定槽位注入
        ServerPlayNetworking.registerGlobalReceiver(CoLANetwork.REPLACE_SPELL_C2S,
                (server, player, handler, buf, responseSender) -> {
                    int slot = buf.readVarInt();
                    String spellId = buf.readString(256);
                    server.execute(() -> handleReplace(player, slot, spellId));
                });

        // C2S：切换创天使法杖选中魔法
        ServerPlayNetworking.registerGlobalReceiver(CoLANetwork.SELECT_SPELL_C2S,
                (server, player, handler, buf, responseSender) -> {
                    int slot = buf.readVarInt();
                    server.execute(() -> handleSelect(player, slot));
                });
    }

    private static void handleCastSpell(ServerPlayerEntity player, String spellId) {
        if (Spells.byId(spellId).isEmpty()) {
            player.sendMessage(Text.translatable("message.chronicles_of_lost_arcana.unknown_spell"), false);
            return;
        }
        // 非法杖释放（如命令/其他入口）按正常魔力消耗处理
        SpellExecutor.execute(player, spellId, false);
    }

    /** 手持创天使法杖时注入魔法。 */
    private static void handleInfuse(ServerPlayerEntity player, String spellId) {
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof AngelStaffItem)) {
            return;
        }
        AngelStaffItem.infuse(player, stack, spellId);
    }

    /** 手持创天使法杖时切换选中槽位（客户端滚轮）。 */
    private static void handleSelect(ServerPlayerEntity player, int slot) {
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof AngelStaffItem)) {
            return;
        }
        AngelStaffItem.select(player, stack, slot);
    }

    /** 手持创天使法杖时替换指定槽位注入（不返还残晶）。 */
    private static void handleReplace(ServerPlayerEntity player, int slot, String spellId) {
        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof AngelStaffItem)) {
            return;
        }
        AngelStaffItem.replace(player, stack, slot, spellId);
    }
}
