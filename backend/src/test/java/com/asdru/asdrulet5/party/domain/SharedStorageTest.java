package com.asdru.asdrulet5.party.domain;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SharedStorageTest {

    @Test
    void freshStorageIsAllEmptyCellsOfTheGivenSize() {
        SharedStorage storage = new SharedStorage(4);

        assertThat(storage.list()).hasSize(4).allMatch(java.util.Objects::isNull);
        assertThat(storage.size()).isEqualTo(4);
    }

    @Test
    void seedFillsCellsFromTheFrontAndLeavesTheRestEmpty() {
        SharedStorage storage = new SharedStorage(4);

        storage.seed(List.of("rusted-sword", "flame-edge"));

        assertThat(storage.list()).containsExactly("rusted-sword", "flame-edge", null, null);
    }

    @Test
    void seedIgnoresItemsBeyondStorageSize() {
        SharedStorage storage = new SharedStorage(2);

        storage.seed(List.of("a", "b", "c"));

        assertThat(storage.list()).containsExactly("a", "b");
    }

    @Test
    void addFirstEmptyFillsTheFirstNullCell() {
        SharedStorage storage = new SharedStorage(3);
        storage.set(0, "rusted-sword");

        storage.addFirstEmpty("flame-edge");

        assertThat(storage.at(1)).isEqualTo("flame-edge");
    }

    @Test
    void addFirstEmptyIsANoOpWhenFull() {
        SharedStorage storage = new SharedStorage(2);
        storage.seed(List.of("a", "b"));

        storage.addFirstEmpty("c");

        assertThat(storage.list()).doesNotContain("c");
    }

    @Test
    void listIsUnmodifiable() {
        SharedStorage storage = new SharedStorage(1);
        List<String> list = storage.list();

        assertThatThrownBy(() -> list.set(0, "rusted-sword"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void setReplacesASingleCell() {
        SharedStorage storage = new SharedStorage(2);
        storage.seed(List.of("a", "b"));

        storage.set(0, null);

        assertThat(storage.list()).containsExactly(null, "b");
        assertThat(Collections.frequency(storage.list(), "b")).isEqualTo(1);
    }
}
