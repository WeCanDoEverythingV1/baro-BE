package wecandoeverything.ledgerly.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApprovalRequestNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(ApprovalRequestNotFoundException ex) {
        log.warn("Approval request not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                fieldErrors.put(err.getField(), err.getDefaultMessage()));

        log.warn("Validation failed: {}", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch on parameter '{}': {}", ex.getName(), ex.getValue());
        String message = "Invalid value for parameter '%s': %s".formatted(ex.getName(), ex.getValue());
        return build(HttpStatus.BAD_REQUEST, message, null);
    }

    @ExceptionHandler(ReceiptUnreadableException.class)
    public ResponseEntity<ErrorResponseDto> handleUnreadableReceipt(ReceiptUnreadableException ex) {
        log.warn("Receipt unreadable: {}", ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        log.warn("Upload exceeded size limit");
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the 10MB limit", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleBadArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    /**
     * Thrown by GeminiClient/EmailDraftService when the AI response can't be
     * parsed into the expected shape. Distinct from a generic 500 because the
     * client can reasonably retry — it's a transient upstream issue, not a bug
     * in the request itself.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDto> handleUpstreamFailure(IllegalStateException ex) {
        log.error("Upstream AI call failed", ex);
        return build(HttpStatus.BAD_GATEWAY, "AI service returned an unexpected response. Please try again.", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleMalformedJson(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException ife && ife.getTargetType().isEnum()) {
            String allowedValues = Arrays.stream(ife.getTargetType().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            String message = "Invalid value \"%s\". Allowed values: %s"
                    .formatted(ife.getValue(), allowedValues);

            log.warn("Invalid enum value: {}", message);
            return build(HttpStatus.BAD_REQUEST, message, null);
        }

        log.warn("Malformed request body: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or malformed JSON", null);
    }

    // Catch-all — must stay last conceptually, though @ExceptionHandler
    // resolution picks the most specific match regardless of method order.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    private ResponseEntity<ErrorResponseDto> build(HttpStatus status, String message, Map<String, String> fieldErrors) {
        ErrorResponseDto body = ErrorResponseDto.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .message(message)
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}