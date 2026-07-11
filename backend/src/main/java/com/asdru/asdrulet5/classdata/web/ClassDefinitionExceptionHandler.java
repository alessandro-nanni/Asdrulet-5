package com.asdru.asdrulet5.classdata.web;

import com.asdru.asdrulet5.classdata.exception.UnknownClassDefinitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ClassDefinitionExceptionHandler {

    @ExceptionHandler(UnknownClassDefinitionException.class)
    public ResponseEntity<Map<String, String>> handleUnknown(UnknownClassDefinitionException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
