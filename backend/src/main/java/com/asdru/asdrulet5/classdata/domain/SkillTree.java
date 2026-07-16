package com.asdru.asdrulet5.classdata.domain;

import com.asdru.asdrulet5.classdata.exception.UnknownSkillNodeException;
import com.asdru.asdrulet5.party.domain.CharacterClass;

import java.util.List;
import java.util.Objects;

/**
 * One class's full skill tree — a flat list of {@link SkillNode}s forming a
 * DAG via each node's own {@code parentId} (null marks a root). See
 * {@link SkillTreeResolver} for turning a member's unlocked node ids into
 * their actual effective ability list.
 */
public record SkillTree(CharacterClass characterClass, List<SkillNode> nodes) {

    public SkillTree {
        Objects.requireNonNull(characterClass, "characterClass");
        nodes = List.copyOf(nodes);
    }

    public SkillNode nodeById(String id) {
        return nodes.stream()
                .filter(node -> node.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new UnknownSkillNodeException(characterClass, id));
    }
}
