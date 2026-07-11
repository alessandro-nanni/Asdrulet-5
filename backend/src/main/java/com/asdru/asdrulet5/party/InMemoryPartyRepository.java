package com.asdru.asdrulet5.party;

import com.asdru.asdrulet5.party.domain.Party;
import org.springframework.stereotype.Repository;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds all party state purely in memory for the lifetime of the process.
 * Swap this out for a persistent implementation later without touching PartyService.
 */
@Repository
public class InMemoryPartyRepository implements PartyRepository {

    // Excludes visually ambiguous characters (0/O, 1/I) since codes are read off a screen or a QR fallback.
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final ConcurrentHashMap<String, Party> parties = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    @Override
    public Party save(Party party) {
        parties.put(party.code(), party);
        return party;
    }

    @Override
    public Optional<Party> findByCode(String code) {
        return Optional.ofNullable(parties.get(code));
    }

    @Override
    public String generateUniqueCode() {
        String code;
        do {
            code = randomCode();
        } while (parties.containsKey(code));
        return code;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
