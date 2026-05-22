package io.agentscope.rag.kb.web.advice;

import io.agentscope.rag.kb.faq.FaqBootstrapAdapter;
import io.agentscope.rag.kb.web.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(e -> e.getField() + ": " + e.getDefaultMessage())
                        .orElse("Validation failed");
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(400, "Bad Request", message, request.getRequestURI()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(400, "Bad Request", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(FaqBootstrapAdapter.FaqLoadException.class)
    public ResponseEntity<ApiErrorResponse> handleFaqLoad(
            FaqBootstrapAdapter.FaqLoadException ex, HttpServletRequest request) {
        log.error("FAQ load failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(500, "FAQ Load Error", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        String hint =
                request.getRequestURI().contains("/chat")
                        ? "Use POST with JSON body {\"message\":\"...\"}"
                        : "See Swagger: /swagger-ui.html";
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(404, "Not Found", hint, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        new ApiErrorResponse(
                                500,
                                "Internal Server Error",
                                "Unexpected error, see server logs.",
                                request.getRequestURI()));
    }
}
