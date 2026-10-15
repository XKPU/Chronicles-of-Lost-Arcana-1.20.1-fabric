/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import top.pu.item.AngelStaffItem;
import top.pu.spell.Spell;
import top.pu.spell.Spells;

/**
 * 魔力 HUD：屏幕左下方能量条（条内显示 当前/上限）；
 * 手持创天使法杖时在能量条上方显示当前选中的魔法。
 */
public final class ManaHud {

    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 9;
    /** 魔力条颜色（天蓝）。 */
    private static final int MANA_COLOR = 0xFF87CEEB;
    private static final int BAR_BG = 0xAA202030;
    private static final int BAR_BORDER = 0xFF3A3A55;
    private static final int TEXT_COLOR = 0xFFE8F6FF;
    private static final int SPELL_COLOR = 0xFF9AD1FF;

    private ManaHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(ManaHud::render);
    }

    private static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }
        if (!ClientManaState.hasReceived()) {
            return;
        }

        int mana = ClientManaState.getMana();
        int max = top.pu.mana.ManaComponent.MAX_MANA;

        var window = client.getWindow();
        int x = 6;
        int y = window.getScaledHeight() - 16;

        // 能量条（含描边）
        context.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BAR_BORDER);
        context.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BAR_BG);
        int fill = (int) ((BAR_WIDTH - 2) * Math.max(0, Math.min(1, mana / (double) max)));
        if (fill > 0) {
            context.fill(x + 1, y + 1, x + 1 + fill, y + BAR_HEIGHT - 1, MANA_COLOR);
        }

        // 条内文字：当前/上限
        String text = mana + "/" + max;
        int tw = client.textRenderer.getWidth(text);
        context.drawTextWithShadow(client.textRenderer, text,
                x + (BAR_WIDTH - tw) / 2, y + (BAR_HEIGHT - 8) / 2, TEXT_COLOR);

        // 手持创天使法杖：上方显示选中魔法与冷却剩余
        ClientPlayerEntity player = client.player;
        if (player.getMainHandStack().getItem() instanceof AngelStaffItem) {
            int selected = ClientStaffState.getSelected();
            var infusions = ClientStaffState.getInfusions();
            String label;
            if (selected < 0 || infusions.isEmpty()) {
                label = Text.translatable("hud.chronicles_of_lost_arcana.no_spell").getString();
            } else {
                Spell spell = Spells.byId(infusions.get(selected)).orElse(null);
                label = (selected + 1) + ". " + (spell == null ? infusions.get(selected)
                        : Text.translatable(spell.nameKey()).getString());
            }
            context.drawTextWithShadow(client.textRenderer, Text.literal(label),
                    x, y - 12, SPELL_COLOR);

            // 冷却中：名称右侧追加灰色剩余秒数
            var cooldown = player.getItemCooldownManager();
            int totalTicks = ClientStaffState.getSelectedCooldownTicks();
            if (totalTicks > 0 && cooldown.isCoolingDown(player.getMainHandStack().getItem())) {
                float progress = cooldown.getCooldownProgress(
                        player.getMainHandStack().getItem(), tickDelta);
                int remainSeconds = Math.max(1,
                        (int) Math.ceil(progress * totalTicks / 20.0F));
                String cd = " (" + remainSeconds + "s)";
                context.drawTextWithShadow(client.textRenderer, Text.literal(cd),
                        x + client.textRenderer.getWidth(label), y - 12, 0xFFAAAAAA);
            }
        }
    }
}
