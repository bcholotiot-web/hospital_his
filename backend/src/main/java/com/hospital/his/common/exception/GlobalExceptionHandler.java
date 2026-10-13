package com.hospital.his.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

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
     * Maneja solicitudes realizadas con un método HTTP
     * no permitido para la ruta.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                        .error(HttpStatus.METHOD_NOT_ALLOWED.name())
                        .message("El método HTTP " + exception.getMethod() + " no está permitido " + "para esta ruta.")
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * Maneja modificaciones simultáneas sobre
     * el mismo registro.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(OptimisticLockingFailureException exception, HttpServletRequest request) {
        ErrorResponse response =
                ErrorResponse.builder()
                        .timestamp(
                                LocalDateTime.now()
                        )
                        .status(
                                HttpStatus.CONFLICT.value()
                        )
                        .error(
                                HttpStatus.CONFLICT.name()
                        )
                        .message(
                                "El registro fue actualizado por "
                                        + "otro usuario. Recargue la "
                                        + "información e intente nuevamente."
                        )
                        .path(
                                request.getRequestURI()
                        )
                        .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    /**
     * Evita exponer SQL cuando se viola una restricción
     * única o una llave foránea.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.CONFLICT.value())
                        .error(HttpStatus.CONFLICT.name())
                        .message(
                                "No fue posible completar la operación. "
                                        + "El registro puede estar duplicado "
                                        + "o relacionado con otros datos.")
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
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