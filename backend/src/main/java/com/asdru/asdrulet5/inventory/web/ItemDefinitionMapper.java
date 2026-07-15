package com.asdru.asdrulet5.inventory.web;

import com.asdru.asdrulet5.inventory.domain.ItemDefinition;
import com.asdru.asdrulet5.inventory.domain.ItemPassive;
import com.asdru.asdrulet5.inventory.web.dto.ItemDefinitionDto;
import com.asdru.asdrulet5.inventory.web.dto.PassiveEffectDto;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ItemDefinitionMapper {

    public ItemDefinitionDto toDto(ItemDefinition definition) {
        return new ItemDefinitionDto(
                definition.id(),
                definition.displayName(),
                definition.slot(),
                definition.description(),
                toDto(definition.passive()),
                definition.price()
        );
    }

    /**
     * Only the flat stat bonuses are numerically summarized here — items
     * whose behavior comes from onX() hooks (lifesteal, thorns, heal-on-kill,
     * ...) show as all-zero here and rely on the item's description text
     * instead, same as how Ability/ActiveEffect pair a mechanical effect with
     * a hand-authored summary rather than trying to derive one.
     */
    private PassiveEffectDto toDto(ItemPassive passive) {
        return new PassiveEffectDto(
                passive.bonusMaxHealth(),
                passive.bonusMaxStamina(),
                passive.bonusDefense(),
                passive.damagePercent()
        );
    }
}
