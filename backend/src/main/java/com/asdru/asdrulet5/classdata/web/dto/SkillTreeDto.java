package com.asdru.asdrulet5.classdata.web.dto;

import com.asdru.asdrulet5.party.domain.CharacterClass;

import java.util.List;

public record SkillTreeDto(
        CharacterClass characterClass,
        List<SkillNodeDto> nodes
) {
}
