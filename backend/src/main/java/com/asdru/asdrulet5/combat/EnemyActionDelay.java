package com.asdru.asdrulet5.combat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Blocks the calling (request-handling) thread briefly between two enemies'
 * actions within the same end-turn cascade — see CombatService.endTurn. Same
 * reasoning as {@code party.RoomEntryDelay}: this has to be a blocking sleep
 * on the request thread, not a client-side delay, because every subscriber
 * (including whoever's own end-turn request triggered the cascade) gets each
 * broadcast the instant it's sent regardless of what any one client's own
 * timers are doing. Sleeping here between broadcasts is what gives a
 * multi-enemy fight the same one-at-a-time clarity a player's own turn
 * already has, instead of every enemy's attack landing in the same instant.
 */
@Component
public class EnemyActionDelay {

    private final long millis;

    public EnemyActionDelay(@Value("${app.enemy-action-delay-ms:900}") long millis) {
        this.millis = millis;
    }

    public void sleep() {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
