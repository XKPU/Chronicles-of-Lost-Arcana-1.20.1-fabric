/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import top.pu.spell.TrackingFeatherEntity;

/**
 * 追踪羽毛实体渲染器：按飞行物品（雪球类）方式渲染，
 * 贴图取自实体 getStack()（羽毛物品贴图）。
 */
public class TrackingFeatherRenderer extends FlyingItemEntityRenderer<TrackingFeatherEntity> {

    public TrackingFeatherRenderer(EntityRendererFactory.Context context) {
        super(context, 0.5F, true);
    }
}
