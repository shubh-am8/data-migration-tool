package com.migration.notifications;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionAlertAdvice {
    private final GspaceNotifier gspaceNotifier;

    public ApiExceptionAlertAdvice(GspaceNotifier gspaceNotifier) {
        this.gspaceNotifier = gspaceNotifier;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", ex.getMessage() != null ? ex.getMessage() : "Bad request"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
            "error", ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> unexpected(RuntimeException ex) {
        gspaceNotifier.sendException("API", ex.getClass().getSimpleName(),
            ex.getMessage() != null ? ex.getMessage() : "unknown", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Internal Server Error"));
    }
}
