package com.asdru.asdrulet5.party.web;

import com.asdru.asdrulet5.party.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class PartyExceptionHandler {

    @ExceptionHandler(PartyNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(PartyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotPartyLeaderException.class)
    public ResponseEntity<Map<String, String>> handleNotLeader(NotPartyLeaderException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotPartyMemberException.class)
    public ResponseEntity<Map<String, String>> handleNotMember(NotPartyMemberException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTurnOrderException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTurnOrder(InvalidTurnOrderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MissingClassSelectionException.class)
    public ResponseEntity<Map<String, String>> handleMissingClassSelection(MissingClassSelectionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ClassAlreadyTakenException.class)
    public ResponseEntity<Map<String, String>> handleClassAlreadyTaken(ClassAlreadyTakenException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DevToolsDisabledException.class)
    public ResponseEntity<Void> handleDevToolsDisabled() {
        // 404 rather than 403, so the feature's existence isn't revealed when disabled (e.g. in prod).
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(NotAFakeMemberException.class)
    public ResponseEntity<Map<String, String>> handleNotAFakeMember(NotAFakeMemberException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(PartyFullException.class)
    public ResponseEntity<Map<String, String>> handlePartyFull(PartyFullException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotACombatRoomException.class)
    public ResponseEntity<Map<String, String>> handleNotACombatRoom(NotACombatRoomException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
