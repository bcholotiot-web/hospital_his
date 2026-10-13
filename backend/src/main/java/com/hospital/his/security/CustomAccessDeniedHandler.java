package com.hospital.his.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Devuelve una respuesta JSON cuando el usuario
 * está autenticado, pero no posee el rol o permiso
 * requerido para ejecutar la operación.
 *
 * Ejemplos:
 *
 * - El usuario tiene SPECIALTY_READ,
 *   pero no SPECIALTY_CREATE.
 *
 * - El usuario tiene ROLE_FARMACIA,
 *   pero no INVENTORY_UPDATE.
 */
@Component
public class CustomAccessDeniedHandler
        implements AccessDeniedHandler {

    /**
     * Atiende una operación rechazada por
     * Spring Security.
     *
     * No se utiliza ObjectMapper para evitar depender
     * directamente de una versión específica de Jackson.
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {

        /*
         * Evitar escribir otra respuesta si el servidor
         * ya comenzó a enviarla al cliente.
         */
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN
        );

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                "UTF-8"
        );

        /*
         * Obtener y proteger la ruta antes de insertarla
         * dentro de la respuesta JSON.
         */
        String requestPath =
                escapeJson(
                        request.getRequestURI()
                );

        String timestamp =
                LocalDateTime.now()
                        .toString();

        String message =
                "No tiene permisos para realizar esta operación.";

        /*
         * Construir una respuesta compatible con
         * ErrorResponse y GlobalExceptionHandler.
         */
        String responseBody =
                "{"
                        + "\"timestamp\":\""
                        + timestamp
                        + "\","
                        + "\"status\":"
                        + HttpStatus.FORBIDDEN.value()
                        + ","
                        + "\"error\":\""
                        + HttpStatus.FORBIDDEN.name()
                        + "\","
                        + "\"message\":\""
                        + escapeJson(message)
                        + "\","
                        + "\"path\":\""
                        + requestPath
                        + "\""
                        + "}";

        response.getWriter().write(
                responseBody
        );

        response.getWriter().flush();
    }

    /**
     * Escapa caracteres especiales para evitar
     * generar una respuesta JSON inválida.
     */
    private String escapeJson(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\r",
                        "\\r"
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\t",
                        "\\t"
                );
    }
}