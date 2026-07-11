package com.asdru.asdrulet5.classdata.web.dto;

/**
 * Wire shape for an AbilityEffect. Kind is a wire-format discriminator only —
 * the domain model no longer has an equivalent field; the mapper derives it
 * from which sealed AbilityEffect subtype it's mapping.
 */
public record EffectDto(
        Kind type,
        int power,
        int durationTurns
) {
    public enum Kind { DAMAGE, HEAL, BUFF_DEFENSE, BUFF_DAMAGE }
}
