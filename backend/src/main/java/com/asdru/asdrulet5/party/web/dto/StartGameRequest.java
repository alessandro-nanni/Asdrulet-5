package com.asdru.asdrulet5.party.web.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StartGameRequest(@NotEmpty List<String> memberIds) {
}
