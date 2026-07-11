package com.asdru.asdrulet5.party.web.dto;

import com.asdru.asdrulet5.party.domain.CharacterClass;
import jakarta.validation.constraints.NotNull;

public record SelectClassRequest(@NotNull CharacterClass characterClass) {
}
