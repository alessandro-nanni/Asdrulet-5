package com.asdru.asdrulet5.inventory.web.dto;

public record PassiveEffectDto(
        int bonusMaxHealth,
        int bonusMaxStamina,
        int bonusDefense,
        int bonusDamage
) {
}
