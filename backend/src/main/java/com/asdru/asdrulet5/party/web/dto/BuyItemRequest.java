package com.asdru.asdrulet5.party.web.dto;

import jakarta.validation.constraints.NotBlank;

public record BuyItemRequest(@NotBlank String itemId) {
}
