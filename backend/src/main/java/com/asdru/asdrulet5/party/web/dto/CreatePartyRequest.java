package com.asdru.asdrulet5.party.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePartyRequest(
        @NotBlank @Size(max = 100) String id,
        @NotBlank @Size(max = 24) String displayName,
        // A client-compressed data: URL image, not a hotlinked http(s) URL —
        // capped generously above what a reasonably-sized avatar needs, as a
        // backstop against an abusive client rather than a real budget.
        @Size(max = 200_000) String avatarUrl
) {
}
