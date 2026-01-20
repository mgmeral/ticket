package dev.mgmeral.ticket.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(EntityNotFoundException e) {
        return e.getMessage();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(IllegalArgumentException e) {
        return e.getMessage();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, fe -> String.valueOf(fe.getDefaultMessage()), (a, b) -> a));
        return Map.of("message", "validation_failed", "errors", errors);
    }

    @ExceptionHandler(dev.mgmeral.ticket.exception.DuplicatePaymentRefException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicatePaymentRef(dev.mgmeral.ticket.exception.DuplicatePaymentRefException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "code", "DUPLICATE_PAYMENT_REF",
                "message", ex.getMessage(),
                "existingPurchaseId", ex.getExistingPurchaseId()
        ));
    }

}