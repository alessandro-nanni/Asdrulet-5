package com.asdru.asdrulet5.classdata.domain;

final class Preconditions {

    private Preconditions() {
    }

    static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
