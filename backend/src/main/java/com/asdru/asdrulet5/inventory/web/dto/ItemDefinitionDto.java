package com.asdru.asdrulet5.inventory.web.dto;

import com.asdru.asdrulet5.inventory.domain.ItemSlot;

public record ItemDefinitionDto(
        String id,
        String displayName,
        ItemSlot slot,
        String description,
        PassiveEffectDto passiveEffect
) {
}
