package com.asdru.asdrulet5.inventory.web;

import com.asdru.asdrulet5.inventory.exception.UnknownItemDefinitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Handles UnknownItemDefinitionException wherever it's thrown — both direct
 * item lookups here and the equip flow in PartyController funnel through
 * this one handler, since Spring resolves @ExceptionHandler by exception
 * type globally rather than per-controller (a second handler for the same
 * type would be an ambiguous mapping error at startup).
 */
@RestControllerAdvice
public class ItemDefinitionExceptionHandler {

    @ExceptionHandler(UnknownItemDefinitionException.class)
    public ResponseEntity<Map<String, String>> handleUnknown(UnknownItemDefinitionException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
