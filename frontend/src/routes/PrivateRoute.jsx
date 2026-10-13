import {
    Navigate,
    useLocation
} from "react-router-dom";

import {
    useAuth
} from "../context/AuthContext";

/**
 * Protege rutas mediante:
 *
 * - Sesión autenticada.
 * - Roles permitidos.
 * - Un permiso requerido.
 * - Cualquiera de varios permisos.
 * - Todos los permisos especificados.
 */
function PrivateRoute({
    children,
    allowedRoles,
    requiredPermission,
    anyPermissions,
    allPermissions
}) {
    const location =
        useLocation();

    const {
        token,
        authenticated,
        loading,
        hasRole,
        hasPermission,
        hasAnyPermission,
        hasAllPermissions
    } = useAuth();

    /**
     * Es importante esperar la respuesta de:
     *
     * GET /api/auth/me
     *
     * De lo contrario, React podría redirigir
     * antes de cargar los permisos.
     */
    if (loading) {
        return (
            <div
                style={{
                    minHeight: "100vh",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontFamily:
                        "Arial, sans-serif"
                }}
            >
                <div>
                    Verificando sesión...
                </div>
            </div>
        );
    }

    /**
     * Sin token o sin usuario autenticado,
     * redirigir al login.
     */
    if (
        !token ||
        !authenticated
    ) {
        return (
            <Navigate
                to="/login"
                replace
                state={{
                    from:
                        location.pathname +
                        location.search
                }}
            />
        );
    }

    /**
     * Validación de roles.
     *
     * Se mantiene para los módulos clínicos:
     *
     * PACIENTE
     * RECEPCIONISTA
     * CAJERO
     * ENFERMERIA
     * MEDICO
     * LABORATORIO
     * FARMACIA
     */
    if (
        Array.isArray(
            allowedRoles
        ) &&
        allowedRoles.length > 0
    ) {
        const hasAllowedRole =
            allowedRoles.some(
                role =>
                    hasRole(role)
            );

        if (!hasAllowedRole) {
            return (
                <Navigate
                    to="/dashboard"
                    replace
                />
            );
        }
    }

    /**
     * Validación de un permiso exacto.
     *
     * Ejemplo:
     *
     * requiredPermission="SPECIALTY_READ"
     */
    if (
        requiredPermission &&
        !hasPermission(
            requiredPermission
        )
    ) {
        return (
            <Navigate
                to="/dashboard"
                replace
            />
        );
    }

    /**
     * Validación de al menos un permiso.
     */
    if (
        Array.isArray(
            anyPermissions
        ) &&
        anyPermissions.length > 0 &&
        !hasAnyPermission(
            anyPermissions
        )
    ) {
        return (
            <Navigate
                to="/dashboard"
                replace
            />
        );
    }

    /**
     * Validación de todos los permisos.
     */
    if (
        Array.isArray(
            allPermissions
        ) &&
        allPermissions.length > 0 &&
        !hasAllPermissions(
            allPermissions
        )
    ) {
        return (
            <Navigate
                to="/dashboard"
                replace
            />
        );
    }

    return children;
}

export default PrivateRoute;