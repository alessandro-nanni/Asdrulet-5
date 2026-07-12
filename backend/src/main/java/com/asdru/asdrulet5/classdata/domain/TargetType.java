package com.asdru.asdrulet5.classdata.domain;

public enum TargetType {
    /** Always the actor themselves, auto-resolved — no target selection needed. */
    SELF,
    /** Any alive ally, chosen explicitly — the actor is a valid choice too. */
    SINGLE_ALLY,
    ALL_ALLIES,
    SINGLE_ENEMY,
    ALL_ENEMIES
}
