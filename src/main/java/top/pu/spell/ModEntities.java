/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import top.pu.CoLA;

/**
 * 新实体注册。
 */
public final class ModEntities {

    public static final EntityType<TrackingFeatherEntity> TRACKING_FEATHER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(CoLA.MOD_ID, "tracking_feather"),
            EntityType.Builder.<TrackingFeatherEntity>create(TrackingFeatherEntity::new, EntityType.MISC)
                    .dimensions(0.25F, 0.25F)
                    .trackingTickInterval(4)
                    .build("tracking_feather"));

    private ModEntities() {
    }

    public static void init() {
        // 类加载时完成注册；显式空方法供 CoLA.onInitialize 调用。
    }
}
