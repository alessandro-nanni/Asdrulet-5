package com.asdru.asdrulet5.party;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Blocks the calling (request-handling) thread briefly before a room-entry
 * transition (combat start or auto-clear) actually happens. This has to be a
 * blocking sleep on the request thread rather than a client-side delay: the
 * frontend's "entering" swirl animation needs the whole transition — both
 * this request's own HTTP response AND the websocket broadcast it triggers
 * — held back together. A client-side-only delay (e.g. a plain
 * {@code setTimeout} before applying the HTTP response) doesn't work,
 * because the party's other subscribers (including the tab that made the
 * request) get the broadcast the instant the server sends it, regardless of
 * what the initiating request's own promise chain is doing — that broadcast
 * would swap the screen out from under the animation before it can play.
 * Sleeping here delays the broadcast itself, so every client sees the
 * transition at the same, animation-friendly moment.
 */
@Component
public class RoomEntryDelay {

    private final long millis;

    public RoomEntryDelay(@Value("${app.room-entry-delay-ms:700}") long millis) {
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
