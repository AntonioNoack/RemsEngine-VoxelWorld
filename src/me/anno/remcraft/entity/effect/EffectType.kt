package me.anno.remcraft.entity.effect

import me.anno.language.translation.NameDesc

enum class EffectType(val naming: NameDesc) {
    DAMAGE(NameDesc("Damage")),
    POISON(NameDesc("Poison")),
    HEALING(NameDesc("Healing")),
    BURNING(NameDesc("Burning")),
    DROWNING(NameDesc("Drowning")),

    // todo join or separate with tool types?
    LUCK(NameDesc("Luck")), // aka looting
    EFFICIENCY(NameDesc("Efficiency")), // aka speed, aka sharpness?
    RETRIBUTION(NameDesc("Retribution")), // on armor: deal damage back
    SUNLIGHT_HEALING(NameDesc("Moss")),
    MOONLIGHT_HEALING(NameDesc("DarkMoss")),
    XP_HEALING(NameDesc("Mending")),
}