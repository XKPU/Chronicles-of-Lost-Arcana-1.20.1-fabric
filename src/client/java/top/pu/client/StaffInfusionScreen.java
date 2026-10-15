/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import top.pu.item.AngelStaffItem;
import top.pu.network.CoLANetwork;
import top.pu.spell.Spell;
import top.pu.spell.Spells;

import java.util.List;

/**
 * 创天使法杖注入 UI：展示已注入魔法与可注入魔法，点击注入（消耗奥术残晶），
 * 已满 8 槽时点击可注入魔法替换对应槽位（不返还残晶）。
 */
public class StaffInfusionScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int ROW_HEIGHT = 20;

    private final List<String> infusions;
    private int selected;

    public StaffInfusionScreen(List<String> infusions, int selected) {
        super(Text.translatable("screen.chronicles_of_lost_arcana.staff_infusion"));
        this.infusions = infusions;
        this.selected = selected;
    }

    @Override
    protected void init() {
        int x = (this.width - PANEL_WIDTH) / 2;
        int y = 40;

        // 已注入槽位：点击替换为"待注入"魔法？此处仅展示；替换通过点击下方可注入魔法完成
        // 可注入魔法按钮
        int by = y + 30 + Math.max(infusions.size(), 1) * ROW_HEIGHT + 10;
        for (Spell spell : Spells.all().values()) {
            if (infusions.contains(spell.id())) {
                continue;
            }
            String spellId = spell.id();
            addDrawableChild(ButtonWidget.builder(
                            Text.translatable(spell.nameKey())
                                    .append(" [" + spell.crystalCost() + "◆]")
                                    .formatted(Formatting.AQUA),
                            b -> sendInfuse(spellId))
                    .dimensions(x, by, PANEL_WIDTH, ROW_HEIGHT)
                    .build());
            by += ROW_HEIGHT + 2;
        }

        // 关闭按钮
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"),
                        b -> close())
                .dimensions(x, this.height - 30, PANEL_WIDTH, ROW_HEIGHT)
                .build());
    }

    private void sendInfuse(String spellId) {
        PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeString(spellId);
        ClientPlayNetworking.send(CoLANetwork.INFUSE_SPELL_C2S, buf);
        // 服务端会回发同步；本地乐观更新展示
        if (!infusions.contains(spellId) && infusions.size() < AngelStaffItem.MAX_INFUSIONS) {
            infusions.add(spellId);
            selected = infusions.size() - 1;
        }
        this.clearAndInit();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int x = (this.width - PANEL_WIDTH) / 2;
        int y = 40;

        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, y - 16, 0xFFFFFF);

        // 残晶数量
        Text crystals = Text.translatable("hud.chronicles_of_lost_arcana.crystals")
                .append(": " + ClientManaState.getCrystals());
        context.drawTextWithShadow(this.textRenderer, crystals, x, y, 0x7FDFFF);

        // 已注入槽位（最多 8 个，含空槽占位）
        int sy = y + 14;
        for (int i = 0; i < AngelStaffItem.MAX_INFUSIONS; i++) {
            int rowY = sy + i * ROW_HEIGHT;
            boolean filled = i < infusions.size();
            String label;
            int color;
            if (filled) {
                Spell spell = Spells.byId(infusions.get(i)).orElse(null);
                label = (i + 1) + ". " + (spell == null ? infusions.get(i)
                        : Text.translatable(spell.nameKey()).getString());
                color = i == selected ? 0xFFFF55 : 0xAAAAAA;
            } else {
                label = (i + 1) + ". " + Text.translatable(
                        "screen.chronicles_of_lost_arcana.empty_slot").getString();
                color = 0x555555;
            }
            context.drawTextWithShadow(this.textRenderer, label, x, rowY, color);
        }

        // 提示：注入满后替换不返还残晶
        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("screen.chronicles_of_lost_arcana.replace_hint"),
                x, sy + AngelStaffItem.MAX_INFUSIONS * ROW_HEIGHT + 2, 0x777777);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
