package com.asdru.asdrulet5.party.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinPartyRequest(
        @NotBlank @Size(max = 24) String displayName,
        @Size(max = 200_000) String avatarUrl
) {
}
