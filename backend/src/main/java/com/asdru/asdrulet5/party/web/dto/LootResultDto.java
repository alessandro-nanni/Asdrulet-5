package com.asdru.asdrulet5.party.web.dto;

import java.util.List;

public record LootResultDto(
        int coins,
        List<String> itemIds
) {
}
