package com.asdru.asdrulet5.party.dev;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;
import java.util.List;

@UtilityClass
public class FakeNameGenerator {

    private final List<String> NAMES = List.of(
            "Grog", "Brynn", "Thistle", "Kael", "Mira", "Doran", "Sable", "Finch", "Orin", "Lyra"
    );

    private final SecureRandom RANDOM = new SecureRandom();

    public String next() {
        return NAMES.get(RANDOM.nextInt(NAMES.size()));
    }
}
