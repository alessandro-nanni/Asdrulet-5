package com.asdru.asdrulet5.party;

import com.asdru.asdrulet5.party.domain.Party;

import java.util.Optional;

public interface PartyRepository {

    Party save(Party party);

    Optional<Party> findByCode(String code);

    String generateUniqueCode();
}
