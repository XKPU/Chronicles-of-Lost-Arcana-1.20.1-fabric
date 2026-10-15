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
            35,
            3600,
            25,
            "spell.chronicles_of_lost_arcana.wing_flight.desc"
    );

    public static final Spell FEATHER_WARD = new Spell(
            "feather_ward",
            "spell.chronicles_of_lost_arcana.feather_ward",
            12,
            1800,
            10,
            "spell.chronicles_of_lost_arcana.feather_ward.desc"
    );

    public static final Spell SKY_FEATHER_RAIN = new Spell(
            "sky_feather_rain",
            "spell.chronicles_of_lost_arcana.sky_feather_rain",
            16,
            700,
            7,
            "spell.chronicles_of_lost_arcana.sky_feather_rain.desc"
    );

    public static final Spell BELOVED_WING = new Spell(
            "beloved_wing",
            "spell.chronicles_of_lost_arcana.beloved_wing",
            10,
            1200,
            4,
            "spell.chronicles_of_lost_arcana.beloved_wing.desc"
    );

    public static final Spell CLAW_REND = new Spell(
            "claw_rend",
            "spell.chronicles_of_lost_arcana.claw_rend",
            11,
            460,
            8,
            "spell.chronicles_of_lost_arcana.claw_rend.desc"
    );

    private static final Map<String, Spell> REGISTRY = new LinkedHashMap<>();

    static {
        register(WING_FLIGHT);
        register(FEATHER_WARD);
        register(SKY_FEATHER_RAIN);
        register(BELOVED_WING);
        register(CLAW_REND);
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
