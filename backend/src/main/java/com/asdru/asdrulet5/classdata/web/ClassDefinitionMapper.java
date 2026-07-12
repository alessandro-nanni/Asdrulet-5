package com.asdru.asdrulet5.classdata.web;

import com.asdru.asdrulet5.classdata.domain.Ability;
import com.asdru.asdrulet5.classdata.domain.AbilityEffect;
import com.asdru.asdrulet5.classdata.domain.BasicAbility;
import com.asdru.asdrulet5.classdata.domain.BuffDamageEffect;
import com.asdru.asdrulet5.classdata.domain.BuffDefenseEffect;
import com.asdru.asdrulet5.classdata.domain.ClassDefinition;
import com.asdru.asdrulet5.classdata.domain.DamageEffect;
import com.asdru.asdrulet5.classdata.domain.HealEffect;
import com.asdru.asdrulet5.classdata.domain.Stats;
import com.asdru.asdrulet5.classdata.domain.UltimateAbility;
import com.asdru.asdrulet5.classdata.web.dto.AbilityDto;
import com.asdru.asdrulet5.classdata.web.dto.ClassDefinitionDto;
import com.asdru.asdrulet5.classdata.web.dto.EffectDto;
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
                stats.damage(),
                stats.defense(),
                stats.speed(),
                stats.maxStamina()
        );
    }

    private AbilityDto toDto(Ability ability) {
        return switch (ability) {
            case BasicAbility basic -> new AbilityDto(
                    basic.id(), basic.name(), basic.description(), basic.targetType(),
                    AbilityDto.AbilityKind.BASIC, basic.staminaCost(), null, toDto(basic.effect()));
            case UltimateAbility ultimate -> new AbilityDto(
                    ultimate.id(), ultimate.name(), ultimate.description(), ultimate.targetType(),
                    AbilityDto.AbilityKind.ULTIMATE, null, ultimate.chargeThreshold(), toDto(ultimate.effect()));
        };
    }

    public EffectDto toDto(AbilityEffect effect) {
        return switch (effect) {
            case DamageEffect damage -> new EffectDto(EffectDto.Kind.DAMAGE, damage.power(), 0);
            case HealEffect heal -> new EffectDto(EffectDto.Kind.HEAL, heal.power(), 0);
            case BuffDefenseEffect buff -> new EffectDto(EffectDto.Kind.BUFF_DEFENSE, buff.power(), buff.durationTurns());
            case BuffDamageEffect buff -> new EffectDto(EffectDto.Kind.BUFF_DAMAGE, buff.power(), buff.durationTurns());
        };
    }
}
