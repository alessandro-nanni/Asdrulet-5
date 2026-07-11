package com.asdru.asdrulet5.party.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AddFakeMembersRequest(@Min(1) @Max(10) int count) {
}
