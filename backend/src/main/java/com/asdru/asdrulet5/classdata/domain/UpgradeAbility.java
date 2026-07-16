package com.asdru.asdrulet5.classdata.domain;

import java.util.Objects;

/**
 * Replaces whichever ability currently has {@code replacement.id()} with
 * {@code replacement} — the id must match an ability the member already has
 * (either a base class ability, or one added by an earlier-unlocked
 * {@link AddAbility} node), which is exactly what makes it an "upgrade"
 * rather than a new unlock. See {@link SkillTreeResolver}.
 */
public record UpgradeAbility(Ability replacement) implements SkillNodeEffect {
    public UpgradeAbility {
        Objects.requireNonNull(replacement, "replacement");
    }
}
