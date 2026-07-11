package com.asdru.asdrulet5.combat;

import com.asdru.asdrulet5.combat.domain.Combat;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds all combat state purely in memory for the lifetime of the process,
 * keyed by party code — same pattern as InMemoryPartyRepository.
 */
@Repository
public class InMemoryCombatRepository implements CombatRepository {

    private final ConcurrentHashMap<String, Combat> combats = new ConcurrentHashMap<>();

    @Override
    public Combat save(Combat combat) {
        combats.put(combat.code(), combat);
        return combat;
    }

    @Override
    public Optional<Combat> findByCode(String code) {
        return Optional.ofNullable(combats.get(code));
    }
}
