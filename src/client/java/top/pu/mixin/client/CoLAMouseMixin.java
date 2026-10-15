/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.pu.client.CoLAMouseHooks;
import top.pu.item.AngelStaffItem;

/**
 * 手持创天使法杖且按住 Alt 时拦截原版滚轮（防止滚动热键栏），
 * 并把滚轮增量镜像给 CoLAMouseHooks 供切换法术使用。
 */
@Mixin(Mouse.class)
public abstract class CoLAMouseMixin {

    @Inject(method = "onMouseScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void cola$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (vertical == 0) {
            return;
        }
        boolean alt = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT);
        MinecraftClient client = MinecraftClient.getInstance();
        if (alt && client.currentScreen == null && client.player != null
                && client.player.getMainHandStack().getItem() instanceof AngelStaffItem) {
            CoLAMouseHooks.addScroll(vertical);
            ci.cancel();
        }
    }
}
