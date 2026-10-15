/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.mixin.client;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.pu.client.CoLAMouseHooks;

/**
 * 把原版鼠标滚轮事件镜像给 CoLAMouseHooks，供 alt+滚轮切换法术使用。
 */
@Mixin(Mouse.class)
public abstract class CoLAMouseMixin {

    @Inject(method = "onMouseScroll(JDD)V", at = @At("HEAD"))
    private void cola$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (vertical != 0) {
            CoLAMouseHooks.addScroll(vertical);
        }
    }
}
