package com.asdru.asdrulet5.party.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SetTurnOrderRequest(@NotEmpty List<String> memberIds) {
}
