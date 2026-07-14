package com.asdru.asdrulet5.party.web.dto;

import com.asdru.asdrulet5.party.domain.Party;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record EquipFromStorageRequest(@Min(0) @Max(Party.STORAGE_SIZE - 1) int storageIndex) {
}
