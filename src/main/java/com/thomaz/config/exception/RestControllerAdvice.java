package com.thomaz.config.exception;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@ControllerAdvice
public class RestControllerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestControllerAdvice.class);

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<@NonNull Map<String, Object>> handleInvalidRequestException(Exception e) {
        return handleError(e, 400);
    }

    @ExceptionHandler(value = {AuthorizationException.class, CryptoException.class})
    public ResponseEntity<@NonNull Map<String, Object>> handleAuthException(Exception e) {
        return handleError(e, 401);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NonNull Map<String, Object>> handleUnexpectedException(Exception e) {
        return handleError(e, 500);
    }

    private ResponseEntity<@NonNull Map<String, Object>> handleError(Exception e, int status) {
        LOGGER.error("Unexpected error", e);
        return ResponseEntity.status(status).body(Map.of(
                "message", "Erro: " + safeGetMessage(e),
                "stackTrace", getLimitedStackTrace(e)
        ));
    }

    private static List<String> getLimitedStackTrace(Exception e) {
        if (e.getStackTrace() == null) {
            return List.of(e.getClass().getSimpleName());
        }
        final List<String> stackTrace = Stream.of(e.getStackTrace())
                .limit(100)
                .map(StackTraceElement::toString)
                .toList();
        return Stream.of(List.of(e.getClass().getSimpleName()), stackTrace)
                .flatMap(List::stream).toList();
    }

    private String safeGetMessage(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Erro inesperado";
    }

}
