package com.hospital.his.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
public class LoginAuthenticationException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final Integer remainingAttempts;
    private final boolean blocked;
    private final LocalDateTime lockedUntil;

    public LoginAuthenticationException(
            String message,
            HttpStatus httpStatus,
            Integer remainingAttempts,
            boolean blocked,
            LocalDateTime lockedUntil
    ) {
        super(message);

        this.httpStatus = httpStatus;
        this.remainingAttempts = remainingAttempts;
        this.blocked = blocked;
        this.lockedUntil = lockedUntil;
    }

    /**
     * Crea una excepción para credenciales incorrectas
     * cuando todavía quedan intentos disponibles.
     */
    public static LoginAuthenticationException invalidCredentials(
            int remainingAttempts
    ) {
        return new LoginAuthenticationException(
                "Usuario o contraseña incorrectos.",
                HttpStatus.UNAUTHORIZED,
                remainingAttempts,
                false,
                null
        );
    }

    /**
     * Crea una excepción cuando el último intento disponible
     * provoca el bloqueo temporal de la cuenta.
     */
    public static LoginAuthenticationException accountBlocked(
            LocalDateTime lockedUntil
    ) {
        return new LoginAuthenticationException(
                "Usuario bloqueado temporalmente.",
                HttpStatus.LOCKED,
                0,
                true,
                lockedUntil
        );
    }

    /**
     * Crea una excepción para una cuenta que ya se encuentra
     * bajo un bloqueo temporal vigente.
     */
    public static LoginAuthenticationException currentlyBlocked(
            LocalDateTime lockedUntil
    ) {
        return new LoginAuthenticationException(
                "La cuenta se encuentra bloqueada temporalmente.",
                HttpStatus.LOCKED,
                0,
                true,
                lockedUntil
        );
    }

    /**
     * Crea una excepción para cuentas inactivas.
     */
    public static LoginAuthenticationException inactiveAccount() {
        return new LoginAuthenticationException(
                "La cuenta se encuentra inactiva.",
                HttpStatus.FORBIDDEN,
                null,
                false,
                null
        );
    }
}