package com.asdru.asdrulet5.classdata.web;

import com.asdru.asdrulet5.classdata.domain.AddAbility;
import com.asdru.asdrulet5.classdata.domain.SkillNode;
import com.asdru.asdrulet5.classdata.domain.SkillTree;
import com.asdru.asdrulet5.classdata.domain.UpgradeAbility;
import com.asdru.asdrulet5.classdata.web.dto.SkillNodeDto;
import com.asdru.asdrulet5.classdata.web.dto.SkillTreeDto;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SkillTreeMapper {

    public SkillTreeDto toDto(SkillTree tree) {
        return new SkillTreeDto(
                tree.characterClass(),
                tree.nodes().stream().map(SkillTreeMapper::toDto).toList()
        );
    }

    private SkillNodeDto toDto(SkillNode node) {
        return switch (node.effect()) {
            case UpgradeAbility upgrade -> new SkillNodeDto(
                    node.id(), node.name(), node.description(), node.manaCost(), node.parentId(),
                    SkillNodeDto.SkillNodeEffectKind.UPGRADE, ClassDefinitionMapper.toDto(upgrade.replacement()));
            case AddAbility add -> new SkillNodeDto(
                    node.id(), node.name(), node.description(), node.manaCost(), node.parentId(),
                    SkillNodeDto.SkillNodeEffectKind.ADD, ClassDefinitionMapper.toDto(add.newAbility()));
        };
    }
}
