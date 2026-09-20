import {
    Navigate
} from "react-router-dom";

/**
 * Protege las rutas que requieren autenticación
 * y, opcionalmente, un rol determinado.
 */
function PrivateRoute({
    children,
    allowedRoles
}) {
    const token =
        localStorage.getItem("token");

    const role =
        localStorage.getItem("role");

    /*
     * Sin token, la persona debe iniciar sesión.
     */
    if (!token) {
        return (
            <Navigate
                to="/login"
                replace
            />
        );
    }

    /*
     * Si la ruta define roles permitidos,
     * comparar después de normalizarlos.
     */
    if (allowedRoles) {
        const normalizedCurrentRole =
            normalizeRole(role);

        const normalizedAllowedRoles =
            allowedRoles.map(
                normalizeRole
            );

        if (
            !normalizedAllowedRoles.includes(
                normalizedCurrentRole
            )
        ) {
            return (
                <Navigate
                    to="/dashboard"
                    replace
                />
            );
        }
    }

    return children;
}

/**
 * Normaliza un rol para evitar problemas por:
 *
 * - Mayúsculas.
 * - Minúsculas.
 * - Espacios.
 * - Tildes.
 *
 * Ejemplos:
 *
 * Médico     → MEDICO
 * Enfermería → ENFERMERIA
 * Farmacia   → FARMACIA
 */
function normalizeRole(value) {
    if (!value) {
        return "";
    }

    return String(value)
        .normalize("NFD")
        .replace(
            /[\u0300-\u036f]/g,
            ""
        )
        .trim()
        .toUpperCase();
}

export default PrivateRoute;