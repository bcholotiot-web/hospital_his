package com.hospital.his.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja errores específicos de autenticación:
     *
     * 401: credenciales incorrectas.
     * 403: cuenta inactiva.
     * 423: cuenta temporalmente bloqueada.
     */
    @ExceptionHandler(LoginAuthenticationException.class)
    public ResponseEntity<ErrorResponse>
    handleLoginAuthenticationException(
            LoginAuthenticationException exception,
            HttpServletRequest request
    ) {
        HttpStatus httpStatus = exception.getHttpStatus();

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(httpStatus.value())
                .error(httpStatus.name())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .remainingAttempts(
                        exception.getRemainingAttempts()
                )
                .blocked(exception.isBlocked())
                .lockedUntil(exception.getLockedUntil())
                .build();

        return ResponseEntity
                .status(httpStatus)
                .body(response);
    }

    /**
     * Maneja las validaciones declaradas con anotaciones como:
     *
     * @NotBlank
     * @NotNull
     * @Size
     * @Pattern
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse>
    handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        for (
                FieldError fieldError
                : exception.getBindingResult().getFieldErrors()
        ) {
            validationErrors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .message(
                        "Existen campos obligatorios o inválidos."
                )
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Manejador de respaldo para las RuntimeException
     * existentes en el proyecto.
     *
     * Se conserva temporalmente para evitar romper servicios
     * que todavía lanzan RuntimeException.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse>
    handleRuntimeException(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.name())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Manejador final para errores inesperados.
     *
     * No se devuelve el detalle técnico al frontend para evitar
     * exponer información interna del servidor.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse>
    handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                )
                .error(
                        HttpStatus.INTERNAL_SERVER_ERROR.name()
                )
                .message(
                        "Ocurrió un error interno. Intente nuevamente."
                )
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}