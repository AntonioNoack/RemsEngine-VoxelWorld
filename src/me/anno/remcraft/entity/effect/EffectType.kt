package me.anno.remcraft.entity.effect

import me.anno.language.translation.NameDesc

enum class EffectType(val naming: NameDesc) {
    DAMAGE(NameDesc("Damage")),
    POISON(NameDesc("Poison")),
    HEALING(NameDesc("Healing")),
    BURNING(NameDesc("Burning")),
    DROWNING(NameDesc("Drowning")),
}