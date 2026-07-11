package com.asdru.asdrulet5.combat;

import com.asdru.asdrulet5.combat.domain.Combat;

import java.util.Optional;

public interface CombatRepository {

    Combat save(Combat combat);

    Optional<Combat> findByCode(String code);
}
