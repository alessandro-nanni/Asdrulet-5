package com.asdru.asdrulet5.dungeon.web;

import com.asdru.asdrulet5.dungeon.exception.DungeonNotFoundException;
import com.asdru.asdrulet5.dungeon.exception.InvalidNodeTransitionException;
import com.asdru.asdrulet5.dungeon.exception.NoRoomSelectedException;
import com.asdru.asdrulet5.dungeon.exception.RoomAlreadyEnteredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * NotPartyLeaderException is intentionally not handled here — it's already
 * mapped to 403 by PartyExceptionHandler, and Spring throws on ambiguous
 * @ExceptionHandler mappings if two advice beans both claim the same type.
 */
@RestControllerAdvice
public class DungeonExceptionHandler {

    @ExceptionHandler(DungeonNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(DungeonNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidNodeTransitionException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTransition(InvalidNodeTransitionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RoomAlreadyEnteredException.class)
    public ResponseEntity<Map<String, String>> handleRoomAlreadyEntered(RoomAlreadyEnteredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NoRoomSelectedException.class)
    public ResponseEntity<Map<String, String>> handleNoRoomSelected(NoRoomSelectedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
