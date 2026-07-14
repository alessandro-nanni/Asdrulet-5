package com.asdru.asdrulet5.dungeon.web.dto;

import jakarta.validation.constraints.NotEmpty;

public record SelectNodeRequest(@NotEmpty String nodeId) {
}
