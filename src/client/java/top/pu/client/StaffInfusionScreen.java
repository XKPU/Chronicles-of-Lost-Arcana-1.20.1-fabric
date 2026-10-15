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

import java.util.ArrayList;
import java.util.List;

/**
 * 创天使法杖注入 UI：左侧 8 槽注入列表 + 右侧可注入魔法卡片，点击注入
 * （消耗奥术残晶），满槽时点击左侧槽位选中替换目标再点右侧魔法替换（不返还残晶）。
 */
public class StaffInfusionScreen extends Screen {

    private static final int PANEL_WIDTH = 246;
    private static final int PANEL_HEIGHT = 176;
    private static final int SLOT_SIZE = 20;
    private static final int CARD_HEIGHT = 16;
    /** 面板配色（深蓝调）。 */
    private static final int PANEL_BG = 0xF010101C;
    private static final int PANEL_BORDER = 0xFF5B7FC7;
    private static final int SLOT_BORDER = 0xFF3E5688;
    private static final int SLOT_BG = 0xFF161C30;
    private static final int HIGHLIGHT = 0xFFFFD966;
    private static final int CARD_BG = 0xE0182036;
    private static final int CARD_HOVER = 0xE02A3A5E;

    private final List<String> infusions;
    private int selected;
    /** 满槽替换时选中的目标槽位（-1 = 无）。 */
    private int replaceTarget = -1;

    public StaffInfusionScreen(List<String> infusions, int selected) {
        super(Text.translatable("screen.chronicles_of_lost_arcana.staff_infusion"));
        this.infusions = new ArrayList<>(infusions);
        this.selected = selected;
    }

    @Override
    protected void init() {
        int px = (this.width - PANEL_WIDTH) / 2;
        int py = (this.height - PANEL_HEIGHT) / 2;

        // 关闭按钮
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(px + PANEL_WIDTH / 2 - 40, py + PANEL_HEIGHT - 24, 80, 18)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int px = (this.width - PANEL_WIDTH) / 2;
        int py = (this.height - PANEL_HEIGHT) / 2;

        // 面板底与描边
        context.fill(px - 1, py - 1, px + PANEL_WIDTH + 1, py + PANEL_HEIGHT + 1, PANEL_BORDER);
        context.fill(px, py, px + PANEL_WIDTH, py + PANEL_HEIGHT, PANEL_BG);

        // 标题与残晶
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, py + 6, 0xFFFFFF);
        Text crystals = Text.translatable("hud.chronicles_of_lost_arcana.crystals")
                .append(": " + Formatting.AQUA + ClientManaState.getCrystals());
        context.drawCenteredTextWithShadow(this.textRenderer, crystals,
                this.width / 2, py + 18, 0x7FDFFF);

        renderSlots(context, px, py, mouseX, mouseY);
        renderCards(context, px, py, mouseX, mouseY);
        renderHint(context, px, py);

        super.render(context, mouseX, mouseY, delta);
    }

    /** 左列：8 个注入槽位（两列 4 行小格），点击选中替换目标。 */
    private void renderSlots(DrawContext context, int px, int py, int mouseX, int mouseY) {
        int sx = px + 10;
        int sy = py + 34;
        for (int i = 0; i < AngelStaffItem.MAX_INFUSIONS; i++) {
            int col = i / 4;
            int row = i % 4;
            int x = sx + col * (SLOT_SIZE + 26);
            int y = sy + row * (SLOT_SIZE + 4);

            boolean filled = i < infusions.size();
            boolean isTarget = i == replaceTarget;
            boolean isSelected = i == selected;

            // 槽位底：选中替换目标高亮，选中项黄框
            int border = isTarget ? HIGHLIGHT : (isSelected ? 0xFFE0B040 : SLOT_BORDER);
            context.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, border);
            context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BG);

            if (filled) {
                // 槽位序号 + 魔法名（空间有限只画首字与序号，名称画在格右侧）
                Spell spell = Spells.byId(infusions.get(i)).orElse(null);
                String name = spell == null ? infusions.get(i)
                        : Text.translatable(spell.nameKey()).getString();
                int nameWidth = Math.min(this.textRenderer.getWidth(name), 24);
                context.drawTextWithShadow(this.textRenderer, name, x + 4, y + 6,
                        isTarget ? HIGHLIGHT : 0xC8D8F0);
                if (this.textRenderer.getWidth(name) > nameWidth) {
                    // 超宽截断由裁剪近似：直接覆盖画省略号
                    context.fill(x + 4 + nameWidth - 6, y + 6, x + 4 + nameWidth, y + 14, SLOT_BG);
                    context.drawTextWithShadow(this.textRenderer, "…",
                            x + 4 + nameWidth - 6, y + 6, 0xC8D8F0);
                }
            } else {
                // 空槽：居中小菱形占位
                int cx = x + SLOT_SIZE / 2;
                int cy = y + SLOT_SIZE / 2;
                context.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFF31405F);
            }
        }
    }

    /** 右列：可注入魔法卡片（未注入的），点击注入或替换。 */
    private void renderCards(DrawContext context, int px, int py, int mouseX, int mouseY) {
        int cx = px + 122;
        int cy = py + 34;
        int cardWidth = PANEL_WIDTH - 132;
        List<Spell> available = new ArrayList<>();
        for (Spell spell : Spells.all().values()) {
            if (!infusions.contains(spell.id())) {
                available.add(spell);
            }
        }
        for (int i = 0; i < available.size(); i++) {
            Spell spell = available.get(i);
            int y = cy + i * (CARD_HEIGHT + 4);
            boolean hovered = mouseX >= cx && mouseX < cx + cardWidth
                    && mouseY >= y && mouseY < y + CARD_HEIGHT;

            int bg = hovered ? CARD_HOVER : CARD_BG;
            context.fill(cx, y, cx + cardWidth, y + CARD_HEIGHT, bg);
            // 左侧色条
            context.fill(cx, y, cx + 2, y + CARD_HEIGHT, 0xFF4F8FE8);

            String name = Text.translatable(spell.nameKey()).getString();
            context.drawTextWithShadow(this.textRenderer, name, cx + 8, y + 4, 0xE8F2FF);
            String cost = spell.crystalCost() + "◆";
            context.drawTextWithShadow(this.textRenderer, cost,
                    cx + cardWidth - this.textRenderer.getWidth(cost) - 5, y + 4, 0xFF7FDFFF);
        }
    }

    private void renderHint(DrawContext context, int px, int py) {
        int hy = py + PANEL_HEIGHT - 34;
        Text hint = replaceTarget >= 0
                ? Text.translatable("screen.chronicles_of_lost_arcana.replace_pick", replaceTarget + 1)
                        .formatted(Formatting.YELLOW)
                : Text.translatable("screen.chronicles_of_lost_arcana.replace_hint").formatted(Formatting.GRAY);
        context.drawCenteredTextWithShadow(this.textRenderer, hint,
                this.width / 2, hy, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int px = (this.width - PANEL_WIDTH) / 2;
        int py = (this.height - PANEL_HEIGHT) / 2;

        // 槽位点击：满槽时选中替换目标
        int sx = px + 10;
        int sy = py + 34;
        for (int i = 0; i < AngelStaffItem.MAX_INFUSIONS && i < infusions.size(); i++) {
            int col = i / 4;
            int row = i % 4;
            int x = sx + col * (SLOT_SIZE + 26);
            int y = sy + row * (SLOT_SIZE + 4);
            if (mouseX >= x - 1 && mouseX < x + SLOT_SIZE + 1
                    && mouseY >= y - 1 && mouseY < y + SLOT_SIZE + 1) {
                if (infusions.size() >= AngelStaffItem.MAX_INFUSIONS) {
                    replaceTarget = (replaceTarget == i) ? -1 : i;
                }
                return true;
            }
        }

        // 卡片点击：注入或替换
        int cx = px + 122;
        int cy = py + 34;
        int cardWidth = PANEL_WIDTH - 132;
        List<Spell> available = new ArrayList<>();
        for (Spell spell : Spells.all().values()) {
            if (!infusions.contains(spell.id())) {
                available.add(spell);
            }
        }
        for (int i = 0; i < available.size(); i++) {
            int y = cy + i * (CARD_HEIGHT + 4);
            if (mouseX >= cx && mouseX < cx + cardWidth && mouseY >= y && mouseY < y + CARD_HEIGHT) {
                String spellId = available.get(i).id();
                if (replaceTarget >= 0) {
                    sendReplace(replaceTarget, spellId);
                } else {
                    sendInfuse(spellId);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

    private void sendReplace(int slot, String newSpellId) {
        PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeVarInt(slot);
        buf.writeString(newSpellId);
        ClientPlayNetworking.send(CoLANetwork.REPLACE_SPELL_C2S, buf);
        // 本地乐观更新
        if (slot >= 0 && slot < infusions.size()) {
            infusions.set(slot, newSpellId);
            selected = slot;
        }
        replaceTarget = -1;
        this.clearAndInit();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
