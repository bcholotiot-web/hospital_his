import {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState
} from "react";

import {
    getAuthenticatedUser
} from "../api/authApi";

/**
 * Contexto global de autenticación.
 *
 * Almacena:
 *
 * - Token JWT.
 * - Usuario autenticado.
 * - Rol actual.
 * - Autoridades y permisos.
 * - Estado de carga de la sesión.
 */
const AuthContext = createContext(null);

/**
 * Proveedor global de autenticación.
 *
 * Debe envolver la aplicación desde main.jsx.
 */
export function AuthProvider({ children }) {
    /**
     * Recuperar el token almacenado al iniciar React.
     */
    const [token, setToken] = useState(() => {
        return localStorage.getItem("token");
    });

    /**
     * Información devuelta por:
     *
     * GET /api/auth/me
     */
    const [user, setUser] = useState(null);

    /**
     * Autoridades vigentes del usuario.
     *
     * Ejemplos:
     *
     * ROLE_ADMIN
     * SPECIALTY_READ
     * SPECIALTY_CREATE
     * INVENTORY_READ
     */
    const [authorities, setAuthorities] = useState([]);

    /**
     * Indica si React está verificando la sesión.
     */
    const [loading, setLoading] = useState(() => {
        return Boolean(
            localStorage.getItem("token")
        );
    });

    /**
     * Limpia la información de la sesión.
     *
     * No realiza navegación para mantener el contexto
     * independiente de React Router.
     */
    const clearSession = useCallback(() => {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        localStorage.removeItem("userId");
        localStorage.removeItem("fullName");
        localStorage.removeItem("username");
        localStorage.removeItem("branchId");
        localStorage.removeItem("branchName");

        setToken(null);
        setUser(null);
        setAuthorities([]);
        setLoading(false);
    }, []);

    /**
     * Alias público para cerrar sesión.
     */
    const logout = useCallback(() => {
        clearSession();
    }, [clearSession]);

    /**
     * Guarda la información básica del usuario
     * para mantener compatibilidad con componentes
     * anteriores.
     */
    const saveUserInLocalStorage = useCallback(
        sessionUser => {
            if (!sessionUser) {
                return;
            }

            localStorage.setItem(
                "role",
                sessionUser.role || ""
            );

            localStorage.setItem(
                "userId",
                sessionUser.userId != null
                    ? String(sessionUser.userId)
                    : ""
            );

            localStorage.setItem(
                "fullName",
                sessionUser.fullName || ""
            );

            localStorage.setItem(
                "username",
                sessionUser.username || ""
            );

            if (sessionUser.branchId != null) {
                localStorage.setItem(
                    "branchId",
                    String(sessionUser.branchId)
                );
            } else {
                localStorage.removeItem("branchId");
            }

            if (sessionUser.branchName) {
                localStorage.setItem(
                    "branchName",
                    sessionUser.branchName
                );
            } else {
                localStorage.removeItem("branchName");
            }
        },
        []
    );

    /**
     * Consulta la sesión actual en el backend.
     *
     * GET /api/auth/me
     *
     * Este endpoint devuelve los permisos vigentes
     * desde PostgreSQL.
     */
    const refreshSession = useCallback(
        async sessionToken => {
            const effectiveToken =
                sessionToken ||
                localStorage.getItem("token");

            if (!effectiveToken) {
                setUser(null);
                setAuthorities([]);
                setLoading(false);

                return null;
            }

            try {
                setLoading(true);

                const response =
                    await getAuthenticatedUser(
                        effectiveToken
                    );

                const sessionUser =
                    response?.data;

                if (!sessionUser) {
                    throw new Error(
                        "El backend no devolvió los datos de la sesión."
                    );
                }

                const effectiveAuthorities =
                    Array.isArray(
                        sessionUser.authorities
                    )
                        ? sessionUser.authorities
                            .filter(Boolean)
                            .map(authority =>
                                String(authority)
                                    .trim()
                                    .toUpperCase()
                            )
                        : [];

                setToken(effectiveToken);
                setUser(sessionUser);
                setAuthorities(
                    effectiveAuthorities
                );

                localStorage.setItem(
                    "token",
                    effectiveToken
                );

                saveUserInLocalStorage(
                    sessionUser
                );

                return sessionUser;

            } catch (error) {
                console.error(
                    "No fue posible recuperar la sesión autenticada:",
                    error
                );

                clearSession();

                return null;

            } finally {
                setLoading(false);
            }
        },
        [
            clearSession,
            saveUserInLocalStorage
        ]
    );

    /**
     * Inicia la sesión utilizando la respuesta
     * de POST /api/auth/login.
     *
     * Después consulta /api/auth/me para obtener
     * todos los permisos efectivos.
     */
    const startSession = useCallback(
        async loginResponse => {
            const newToken =
                loginResponse?.token;

            if (!newToken) {
                throw new Error(
                    "La respuesta del inicio de sesión no contiene un token."
                );
            }

            localStorage.setItem(
                "token",
                newToken
            );

            /*
             * Guardado temporal para mantener compatibilidad
             * mientras /api/auth/me termina de cargar.
             */
            if (loginResponse.role) {
                localStorage.setItem(
                    "role",
                    loginResponse.role
                );
            }

            if (loginResponse.userId != null) {
                localStorage.setItem(
                    "userId",
                    String(loginResponse.userId)
                );
            }

            if (loginResponse.fullName) {
                localStorage.setItem(
                    "fullName",
                    loginResponse.fullName
                );
            }

            setToken(newToken);

            /*
             * Recuperar inmediatamente el usuario y
             * sus autoridades desde PostgreSQL.
             */
            return await refreshSession(
                newToken
            );
        },
        [refreshSession]
    );

    /**
     * Cargar la sesión cuando la aplicación
     * se abre o se recarga.
     */
    useEffect(() => {
        const storedToken =
            localStorage.getItem("token");

        if (!storedToken) {
            setLoading(false);
            return;
        }

        refreshSession(storedToken);
    }, [refreshSession]);

    /**
     * Verifica una autoridad exacta.
     *
     * Ejemplo:
     *
     * hasPermission("SPECIALTY_CREATE")
     */
    const hasPermission = useCallback(
        permission => {
            if (!permission) {
                return true;
            }

            const normalizedPermission =
                String(permission)
                    .trim()
                    .toUpperCase();

            return authorities.includes(
                normalizedPermission
            );
        },
        [authorities]
    );

    /**
     * Verifica si el usuario posee al menos
     * uno de los permisos proporcionados.
     */
    const hasAnyPermission = useCallback(
        permissions => {
            if (
                !Array.isArray(permissions) ||
                permissions.length === 0
            ) {
                return true;
            }

            return permissions.some(
                permission =>
                    hasPermission(permission)
            );
        },
        [hasPermission]
    );

    /**
     * Verifica que el usuario posea todos
     * los permisos indicados.
     */
    const hasAllPermissions = useCallback(
        permissions => {
            if (
                !Array.isArray(permissions) ||
                permissions.length === 0
            ) {
                return true;
            }

            return permissions.every(
                permission =>
                    hasPermission(permission)
            );
        },
        [hasPermission]
    );

    /**
     * Verifica el rol actual.
     *
     * Ejemplo:
     *
     * hasRole("ADMIN")
     */
    const hasRole = useCallback(
        expectedRole => {
            if (!expectedRole) {
                return true;
            }

            const currentRole =
                normalizeRole(
                    user?.role ||
                    localStorage.getItem("role")
                );

            return (
                currentRole ===
                normalizeRole(expectedRole)
            );
        },
        [user]
    );

    /**
     * Indica si existe un token y un usuario
     * autenticado cargado.
     */
    const authenticated =
        Boolean(token && user);

    /**
     * Memorizar el valor del contexto para evitar
     * renderizados innecesarios.
     */
    const contextValue = useMemo(
        () => ({
            token,
            user,
            role:
                user?.role ||
                localStorage.getItem("role") ||
                null,
            authorities,
            loading,
            authenticated,
            startSession,
            refreshSession,
            logout,
            clearSession,
            hasPermission,
            hasAnyPermission,
            hasAllPermissions,
            hasRole
        }),
        [
            token,
            user,
            authorities,
            loading,
            authenticated,
            startSession,
            refreshSession,
            logout,
            clearSession,
            hasPermission,
            hasAnyPermission,
            hasAllPermissions,
            hasRole
        ]
    );

    return (
        <AuthContext.Provider
            value={contextValue}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context =
        useContext(AuthContext);

    if (!context) {
        throw new Error(
            "useAuth debe utilizarse dentro de AuthProvider."
        );
    }

    return context;
}

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
        .replace(
            /\s+/g,
            "_"
        )
        .toUpperCase();
}

export default AuthContext;