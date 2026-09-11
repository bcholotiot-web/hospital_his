package com.hospital.his.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /*
     * Momento en que ocurrió el error.
     */
    private LocalDateTime timestamp;

    /*
     * Código numérico HTTP.
     * Ejemplos: 400, 401, 403 y 423.
     */
    private Integer status;

    /*
     * Nombre del código HTTP.
     * Ejemplos:
     * BAD_REQUEST
     * UNAUTHORIZED
     * FORBIDDEN
     * LOCKED
     */
    private String error;

    /*
     * Mensaje principal que debe mostrar el frontend.
     */
    private String message;

    /*
     * Ruta donde ocurrió el error.
     * Ejemplo: /api/auth/login
     */
    private String path;

    /*
     * Cantidad de intentos disponibles antes del bloqueo.
     * Se utiliza únicamente en errores de autenticación.
     */
    private Integer remainingAttempts;

    /*
     * Indica si la cuenta se encuentra temporalmente bloqueada.
     */
    private Boolean blocked;

    /*
     * Fecha y hora en que terminará el bloqueo.
     */
    private LocalDateTime lockedUntil;

    /*
     * Errores individuales de validación.
     * Ejemplo:
     * {
     *   "username": "El nombre de usuario es obligatorio."
     * }
     */
    private Map<String, String> validationErrors;
}