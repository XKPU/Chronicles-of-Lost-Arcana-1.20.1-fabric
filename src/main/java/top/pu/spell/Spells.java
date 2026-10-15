/*
 * SPDX-License-Identifier: GPL-3.0
 * Copyright (c) 2026 K_PU
 */
package top.pu.spell;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 法术注册表（简单静态注册）。
 */
public final class Spells {

    public static final Spell WING_FLIGHT = new Spell(
            "wing_flight",
            "spell.chronicles_of_lost_arcana.wing_flight",
            5,
            1800,
            3,
            "spell.chronicles_of_lost_arcana.wing_flight.desc"
    );

    private static final Map<String, Spell> REGISTRY = new LinkedHashMap<>();

    static {
        register(WING_FLIGHT);
    }

    private static void register(Spell spell) {
        REGISTRY.put(spell.id(), spell);
    }

    public static Optional<Spell> byId(String id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static Map<String, Spell> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    private Spells() {
    }
}
