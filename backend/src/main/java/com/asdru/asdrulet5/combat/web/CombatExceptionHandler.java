package com.asdru.asdrulet5.combat.web;

import com.asdru.asdrulet5.combat.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class CombatExceptionHandler {

    @ExceptionHandler(CombatNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(CombatNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotYourTurnException.class)
    public ResponseEntity<Map<String, String>> handleNotYourTurn(NotYourTurnException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({InsufficientResourceException.class, InvalidTargetException.class, UnknownAbilityException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CombatNotInProgressException.class)
    public ResponseEntity<Map<String, String>> handleNotInProgress(CombatNotInProgressException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}
