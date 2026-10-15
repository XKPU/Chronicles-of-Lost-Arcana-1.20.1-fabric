/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.item;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import top.pu.mana.ManaManager;
import top.pu.network.CoLANetwork;
import top.pu.spell.Spell;
import top.pu.spell.SpellExecutor;
import top.pu.spell.Spells;

import java.util.ArrayList;
import java.util.List;

/**
 * 创天使法杖：最多注入 8 个魔法（消耗奥术残晶），
 * shift+右键打开注入 UI，右键释放当前选中魔法，alt+滚轮切换选中魔法。
 */
public class AngelStaffItem extends Item {

    /** 最大同时注入数。 */
    public static final int MAX_INFUSIONS = 8;
    private static final String NBT_KEY = "cola_infusions";
    private static final String NBT_SELECTED = "cola_selected";

    public AngelStaffItem(Settings settings) {
        super(settings);
    }

    /** 注入的魔法 id 列表（按注入顺序）。 */
    public static List<String> getInfusions(ItemStack stack) {
        List<String> list = new ArrayList<>();
        if (stack.getNbt() == null || stack.getSubNbt(NBT_KEY) == null) {
            return list;
        }
        NbtList nbt = stack.getSubNbt(NBT_KEY).getList("spells", NbtElement.STRING_TYPE);
        for (int i = 0; i < nbt.size(); i++) {
            list.add(nbt.getString(i));
        }
        return list;
    }

    /** 当前选中的槽位（-1 = 未选中）。 */
    public static int getSelected(ItemStack stack) {
        if (stack.getNbt() == null || !stack.getNbt().contains(NBT_SELECTED)) {
            return -1;
        }
        int sel = stack.getNbt().getInt(NBT_SELECTED);
        return sel >= 0 && sel < getInfusions(stack).size() ? sel : -1;
    }

    /** 注入魔法（服务端；含残晶消耗校验）。返回是否成功。 */
    public static boolean infuse(ServerPlayerEntity player, ItemStack stack, String spellId) {
        Spell spell = Spells.byId(spellId).orElse(null);
        if (spell == null) {
            return false;
        }
        List<String> list = getInfusions(stack);
        if (list.contains(spellId)) {
            // 重复注入不消耗资源
            ManaManager.sendAlreadyInfused(player);
            return false;
        }
        if (list.size() >= MAX_INFUSIONS) {
            // 满槽需先替换，替换不返还残晶
            ManaManager.sendInfusionsFull(player);
            return false;
        }
        if (!ManaManager.tryConsumeCrystals(player, spell.crystalCost())) {
            ManaManager.sendNotEnoughCrystals(player, spell.crystalCost());
            return false;
        }
        NbtCompound root = stack.getOrCreateNbt();
        NbtCompound data = root.getCompound(NBT_KEY);
        NbtList spells = data.getList("spells", NbtElement.STRING_TYPE);
        spells.add(NbtString.of(spellId));
        data.put("spells", spells);
        root.put(NBT_KEY, data);
        if (getSelected(stack) < 0) {
            root.putInt(NBT_SELECTED, 0);
        }
        StaffSync.sendTo(player, stack);
        ManaManager.sendInfused(player, spellId);
        return true;
    }

    /** 替换指定槽位的注入魔法，不返还残晶。 */
    public static boolean replace(ServerPlayerEntity player, ItemStack stack, int slot, String newSpellId) {
        Spell spell = Spells.byId(newSpellId).orElse(null);
        List<String> list = getInfusions(stack);
        if (spell == null || slot < 0 || slot >= list.size() || list.contains(newSpellId)) {
            return false;
        }
        list.set(slot, newSpellId);
        NbtCompound root = stack.getOrCreateNbt();
        NbtCompound data = root.getCompound(NBT_KEY);
        NbtList spells = new NbtList();
        for (String id : list) {
            spells.add(NbtString.of(id));
        }
        data.put("spells", spells);
        root.put(NBT_KEY, data);
        StaffSync.sendTo(player, stack);
        ManaManager.sendInfused(player, newSpellId);
        return true;
    }

    /** 切换选中槽位（服务端权威），并回发同步。 */
    public static void select(ServerPlayerEntity player, ItemStack stack, int slot) {
        int size = getInfusions(stack).size();
        if (size == 0) {
            return;
        }
        int clamped = Math.max(0, Math.min(size - 1, slot));
        stack.getOrCreateNbt().putInt(NBT_SELECTED, clamped);
        StaffSync.sendTo(player, stack);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) user;
            if (user.isSneaking()) {
                // shift+右键：打开注入 UI
                StaffSync.sendOpenEditor(serverPlayer, stack);
                return TypedActionResult.success(stack, world.isClient());
            }
            int selected = getSelected(stack);
            List<String> list = getInfusions(stack);
            if (selected < 0 || selected >= list.size()) {
                ManaManager.sendNoSpellSelected(serverPlayer);
                return TypedActionResult.success(stack, world.isClient());
            }
            String spellId = list.get(selected);
            boolean success = SpellExecutor.execute(serverPlayer, spellId, stack);
            if (success) {
                user.getItemCooldownManager().set(this, SpellExecutor.STAFF_COOLDOWN_TICKS);
            }
            return TypedActionResult.success(stack, world.isClient());
        }

        // 客户端：成败由服务端权威，返回 success 保证手部动画
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.chronicles_of_lost_arcana.angel_staff.lore1")
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.chronicles_of_lost_arcana.angel_staff.lore2")
                .formatted(Formatting.GRAY));
        super.appendTooltip(stack, world, tooltip, context);
    }

    /**
     * 法杖注入数据 S2C 同步（payload 末尾的 openEditor 标记为 true 时客户端打开注入 UI）。
     */
    public static final class StaffSync {
        private StaffSync() {
        }

        /** 同步注入数据到客户端（ClientStaffState 缓存）。 */
        public static void sendTo(ServerPlayerEntity player, ItemStack stack) {
            send(player, stack, false);
        }

        /** 同步注入数据并请求客户端打开注入 UI。 */
        public static void sendOpenEditor(ServerPlayerEntity player, ItemStack stack) {
            send(player, stack, true);
        }

        private static void send(ServerPlayerEntity player, ItemStack stack, boolean openEditor) {
            PacketByteBuf buf = PacketByteBufs.create();
            List<String> list = getInfusions(stack);
            buf.writeVarInt(list.size());
            for (String id : list) {
                buf.writeString(id);
            }
            buf.writeVarInt(getSelected(stack));
            buf.writeBoolean(openEditor);
            ServerPlayNetworking.send(player, CoLANetwork.STAFF_SYNC_S2C, buf);
        }
    }
}
