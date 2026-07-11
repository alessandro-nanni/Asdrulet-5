package com.asdru.asdrulet5.combat.web.dto;

import com.asdru.asdrulet5.classdata.web.dto.EffectDto;

public record ActiveEffectDto(
        EffectDto.Kind type,
        int power,
        int remainingTurns
) {
}
