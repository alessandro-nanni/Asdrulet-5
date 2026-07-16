package com.asdru.asdrulet5.classdata.web.dto;

/**
 * Wire shape for one {@code SkillNode} — resultingAbility is the ability as
 * it looks once this node is unlocked (the base ability being upgraded, or
 * the new one being added), so the client can render it with the same
 * {@code AbilityCard} it already uses for the static class catalog.
 */
public record SkillNodeDto(
        String id,
        String name,
        String description,
        int manaCost,
        String parentId,
        SkillNodeEffectKind effectKind,
        AbilityDto resultingAbility
) {
    public enum SkillNodeEffectKind {UPGRADE, ADD}
}
