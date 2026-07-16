package com.asdru.asdrulet5.classdata.domain;

import java.util.Objects;

/**
 * Grants {@code newAbility} outright — its id must not collide with any
 * ability the class already has, base or previously added. See
 * {@link SkillTreeResolver}.
 */
public record AddAbility(Ability newAbility) implements SkillNodeEffect {
    public AddAbility {
        Objects.requireNonNull(newAbility, "newAbility");
    }
}
