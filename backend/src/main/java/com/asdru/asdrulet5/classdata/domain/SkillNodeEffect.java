package com.asdru.asdrulet5.classdata.domain;

/**
 * What unlocking a {@link SkillNode} does to a member's effective ability
 * list — see {@link SkillTreeResolver}. Open to exactly these two shapes:
 * replace an existing ability wholesale, or add a brand new one. Not a
 * numeric-delta system deliberately — each variant is authored as a
 * complete, self-contained {@link Ability} using the same
 * {@link AbilityEffect} factories every base class ability already does, so
 * a "deeper" upgrade for the same ability just restates its full new
 * numbers rather than needing to compose on top of an earlier tier.
 */
public sealed interface SkillNodeEffect permits UpgradeAbility, AddAbility {
}
