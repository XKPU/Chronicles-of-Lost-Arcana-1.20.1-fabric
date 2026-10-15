/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.client;

/**
 * 鼠标滚轮钩子：由客户端 Mixin（CoLAMouseMixin）在滚轮事件时写入，
 * 供 {@link StaffScrollHandler} 消费。
 */
public final class CoLAMouseHooks {

    private static double pendingScroll = 0;

    private CoLAMouseHooks() {
    }

    /** Mixin 调用：记录本次滚轮增量（GLFW 原始增量，1 格 = 1.0）。 */
    public static void addScroll(double vertical) {
        pendingScroll += vertical;
    }

    /** 取走并清空待处理的滚轮增量。 */
    public static double consumeScroll() {
        double v = pendingScroll;
        pendingScroll = 0;
        return v;
    }
}
