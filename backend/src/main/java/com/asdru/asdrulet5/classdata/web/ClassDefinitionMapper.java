package com.asdru.asdrulet5.classdata.web;

import com.asdru.asdrulet5.classdata.domain.*;
import com.asdru.asdrulet5.classdata.web.dto.AbilityDto;
import com.asdru.asdrulet5.classdata.web.dto.ClassDefinitionDto;
import com.asdru.asdrulet5.classdata.web.dto.StatsDto;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ClassDefinitionMapper {

    public ClassDefinitionDto toDto(ClassDefinition definition) {
        return new ClassDefinitionDto(
                definition.characterClass(),
                definition.displayName(),
                definition.flavorText(),
                toDto(definition.stats()),
                definition.abilities().stream().map(ClassDefinitionMapper::toDto).toList()
        );
    }

    private StatsDto toDto(Stats stats) {
        return new StatsDto(
                stats.maxHealth(),
                stats.defense(),
                stats.maxStamina()
        );
    }

    private AbilityDto toDto(Ability ability) {
        return switch (ability) {
            case BasicAbility basic -> new AbilityDto(
                    basic.id(), basic.name(), basic.description(), basic.effectSummary(), basic.targetType(),
                    AbilityDto.AbilityKind.BASIC, basic.staminaCost(), null);
            case UltimateAbility ultimate -> new AbilityDto(
                    ultimate.id(), ultimate.name(), ultimate.description(), ultimate.effectSummary(), ultimate.targetType(),
                    AbilityDto.AbilityKind.ULTIMATE, null, ultimate.chargeThreshold());
        };
    }
}
