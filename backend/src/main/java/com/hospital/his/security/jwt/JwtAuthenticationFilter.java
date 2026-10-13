package com.hospital.his.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.hospital.his.security.UserAuthorityService;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter{
    private final JwtService jwtService;
    /**
     * Recupera el rol y los permisos actuales
     * desde PostgreSQL.
     */
    private final UserAuthorityService
            userAuthorityService;

    public JwtAuthenticationFilter(JwtService jwtService, UserAuthorityService userAuthorityService){
        this.jwtService = jwtService;
        this.userAuthorityService = userAuthorityService;
    }


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader =
                request.getHeader(
                        "Authorization"
                );

        /*
         * Si la petición no contiene Bearer Token,
         * continuar. SecurityConfig decidirá si la
         * ruta permite acceso anónimo.
         */
        if (
                authHeader == null ||
                        !authHeader.startsWith(
                                "Bearer "
                        )
        ) {
            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String token =
                authHeader.substring(7);

        /*
         * Validar firma y fecha de vencimiento.
         */
        if (!jwtService.isTokenValid(token)) {
            writeUnauthorizedResponse(
                    response,
                    "El token no es válido o ha expirado."
            );

            return;
        }

        try {
            String username =
                    jwtService.extractUsername(
                            token
                    );

            /*
             * Evitar reemplazar una autenticación
             * previamente establecida.
             */
            if (
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication()
                            == null
            ) {
                var authorities =
                        userAuthorityService
                                .getAuthorities(
                                        username
                                );

                UsernamePasswordAuthenticationToken
                        authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities
                        );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                                authentication
                        );
            }

            filterChain.doFilter(
                    request,
                    response
            );

        } catch (RuntimeException exception) {
            SecurityContextHolder
                    .clearContext();

            writeUnauthorizedResponse(
                    response,
                    "La sesión no es válida o la cuenta no tiene acceso."
            );
        }
    }


    /**
     * Escribe una respuesta JSON segura para errores
     * de autenticación ocurridos dentro del filtro.
     */
    private void writeUnauthorizedResponse(
            HttpServletResponse response,
            String message
    ) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                "UTF-8"
        );

        String safeMessage =
                message != null
                        ? message.replace(
                        "\"",
                        "\\\""
                )
                        : "No autorizado.";

        response.getWriter().write(
                "{"
                        + "\"status\":401,"
                        + "\"error\":\"UNAUTHORIZED\","
                        + "\"message\":\""
                        + safeMessage
                        + "\""
                        + "}"
        );
    }

}
