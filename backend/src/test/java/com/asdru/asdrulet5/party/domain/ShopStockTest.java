package com.asdru.asdrulet5.party.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShopStockTest {

    @Test
    void freshStockIsEmpty() {
        ShopStock stock = new ShopStock();

        assertThat(stock.list()).isEmpty();
        assertThat(stock.contains("rusted-sword")).isFalse();
    }

    @Test
    void rollReplacesWhateverWasThereBefore() {
        ShopStock stock = new ShopStock();
        stock.roll(List.of("rusted-sword"));

        stock.roll(List.of("flame-edge", "plate-armor"));

        assertThat(stock.list()).containsExactly("flame-edge", "plate-armor");
    }

    @Test
    void removeDropsOnlyTheGivenItem() {
        ShopStock stock = new ShopStock();
        stock.roll(List.of("rusted-sword", "flame-edge"));

        stock.remove("rusted-sword");

        assertThat(stock.list()).containsExactly("flame-edge");
        assertThat(stock.contains("rusted-sword")).isFalse();
    }
}
