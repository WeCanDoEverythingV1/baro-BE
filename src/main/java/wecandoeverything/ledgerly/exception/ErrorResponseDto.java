package wecandoeverything.ledgerly.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class ErrorResponseDto {
    private Instant timestamp;
    private int status;
    private String message;
    private Map<String, String> fieldErrors; // null when not a validation error
}