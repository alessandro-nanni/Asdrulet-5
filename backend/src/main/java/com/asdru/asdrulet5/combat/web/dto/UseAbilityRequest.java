package com.asdru.asdrulet5.combat.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UseAbilityRequest(
        @NotBlank String abilityId,
        String targetId
) {
}
