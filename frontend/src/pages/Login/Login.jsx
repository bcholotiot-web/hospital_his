import { useState } from "react";
import { login } from "../../api/authApi";
import { Link, useNavigate, useLocation } from "react-router-dom";

import "./Login.css";
import "../../styles/ui.css";

function Login() {
    //const [username, setUsername] = useState("");
    //const [password, setPassword] = useState("");
    //const navigate = useNavigate();
    const navigate = useNavigate();
    const location = useLocation();
    const usernameFromVerification = location.state?.username || "";

    const [username, setUsername] = useState(usernameFromVerification);
    const [password, setPassword] = useState("");
    /*
     * Mensaje principal devuelto por el backend.
     */
    const [errorMessage, setErrorMessage] = useState("");

    /*
     * Número de intentos restantes antes del bloqueo.
     */
    const [remainingAttempts, setRemainingAttempts] =
        useState(null);

    /*
     * Indica si el backend informó que la cuenta
     * está temporalmente bloqueada.
     */
    const [blocked, setBlocked] = useState(false);

    /*
     * Fecha y hora en que termina el bloqueo.
     */
    const [lockedUntil, setLockedUntil] = useState(null);

    const redirectTo = location.state?.from || "/dashboard";
    const fromAppointment = location.state?.fromAppointment === true;
    const informationMessage = location.state?.informationMessage || "";

    /*
     * Evita múltiples solicitudes mientras el inicio
     * de sesión se encuentra en proceso.
     */
    const [loading, setLoading] = useState(false);


    /**
     * Limpia los mensajes mostrados anteriormente.
     *
     * Se utiliza cuando la persona comienza a modificar
     * el usuario o la contraseña.
     */
    const clearLoginMessages = () => {
        setErrorMessage("");
        setRemainingAttempts(null);
        setBlocked(false);
        setLockedUntil(null);
    };

    /**
     * Convierte la fecha enviada por Spring Boot a un
     * formato legible para Guatemala.
     *
     * Ejemplo recibido:
     * 2026-09-05T12:45:00
     *
     * Ejemplo mostrado:
     * 05/09/2026, 12:45
     */
    const formatLockedUntil = (value) => {
        if (!value) {
            return "";
        }

        const date = new Date(value);

        /*
         * Si el navegador no puede interpretar la fecha,
         * se muestra el valor original enviado por el backend.
         */
        if (Number.isNaN(date.getTime())) {
            return value;
        }

        return date.toLocaleString("es-GT", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
            hour12: false
        });
    };

    /**
     * Obtiene un mensaje de validación cuando el backend
     * devuelve validationErrors.
     */
    const getValidationMessage = (errorData) => {
        const validationErrors =
            errorData?.validationErrors;

        if (!validationErrors) {
            return null;
        }

        /*
         * Obtiene el primer mensaje de validación.
         *
         * Ejemplo:
         * {
         *   username: "El nombre de usuario es obligatorio."
         * }
         */
        const messages =
            Object.values(validationErrors);

        if (messages.length === 0) {
            return null;
        }

        return messages[0];
    };

    /**
     * Procesa una respuesta de error enviada por el backend.
     *
     * Casos contemplados:
     *
     * 400: campos inválidos.
     * 401: credenciales incorrectas.
     * 403: cuenta inactiva.
     * 423: cuenta temporalmente bloqueada.
     */
    const handleBackendError = (error) => {
        const status = error.response?.status;
        const errorData = error.response?.data;

        console.error(
            "Error de autenticación:",
            {
                status,
                data: errorData
            }
        );

        const validationMessage =
            getValidationMessage(errorData);

        const backendMessage =
            validationMessage ||
            errorData?.message ||
            errorData?.error ||
            "No fue posible iniciar sesión.";

        /*
         * Se utiliza ?? porque remainingAttempts
         * puede tener legítimamente el valor 0.
         */
        const backendRemainingAttempts =
            errorData?.remainingAttempts ?? null;

        const backendBlocked =
            errorData?.blocked === true ||
            status === 423;

        const backendLockedUntil =
            errorData?.lockedUntil ?? null;

        setErrorMessage(backendMessage);
        setBlocked(backendBlocked);
        setLockedUntil(backendLockedUntil);

        if (
            backendRemainingAttempts !== null &&
            backendRemainingAttempts !== undefined &&
            !Number.isNaN(
                Number(backendRemainingAttempts)
            )
        ) {
            setRemainingAttempts(
                Number(backendRemainingAttempts)
            );
        } else {
            setRemainingAttempts(null);
        }
    };

    /**
     * Procesa el formulario de inicio de sesión.
     */
    const handleLogin = async (event) => {
        event.preventDefault();

        /*
         * Evita enviar dos solicitudes al mismo tiempo.
         */
        if (loading) {
            return;
        }

        clearLoginMessages();

        const normalizedUsername =
            username.trim();

        /*
         * Validaciones básicas del frontend.
         *
         * Estas validaciones no reemplazan las del backend.
         * Solamente evitan solicitudes vacías.
         */
        if (!normalizedUsername) {
            setErrorMessage(
                "Debe ingresar un nombre de usuario."
            );
            return;
        }

        if (!password) {
            setErrorMessage(
                "Debe ingresar una contraseña."
            );
            return;
        }

        setLoading(true);

        try {
            const response = await login({
                username: normalizedUsername,
                password
            });

            const loginData = response.data;

            /*
             * Validar que el backend haya devuelto el token.
             */
            if (!loginData?.token) {
                setErrorMessage(
                    "El servidor no devolvió un token de autenticación válido."
                );
                return;
            }

            /*
             * Guardar información de la sesión.
             */
            localStorage.setItem(
                "token",
                loginData.token
            );

            localStorage.setItem(
                "role",
                loginData.role || ""
            );

            localStorage.setItem(
                "userId",
                String(loginData.userId)
            );

            localStorage.setItem(
                "fullName",
                loginData.fullName || ""
            );

            /*
             * Limpiar cualquier mensaje anterior.
             */
            clearLoginMessages();

            /*
             * Redirigir al dashboard.
             *
             * replace evita regresar al login con el botón
             * Atrás del navegador después de autenticarse.
             */
            navigate("/dashboard", {
                replace: true
            });
        } catch (error) {
            /*
             * Si existe error.response, el backend sí respondió.
             *
             * Puede ser:
             * 400, 401, 403 o 423.
             */
            if (error.response) {
                handleBackendError(error);
            } else {
                /*
                 * Si no existe error.response, normalmente
                 * existe un problema de red o el backend
                 * está apagado.
                 */
                console.error(
                    "Error de conexión:",
                    error
                );

                setErrorMessage(
                    "No fue posible comunicarse con el servidor. Intente nuevamente."
                );

                setRemainingAttempts(null);
                setBlocked(false);
                setLockedUntil(null);
            }
        } finally {
            setLoading(false);
        }
    };

    const formattedLockedUntil =
        formatLockedUntil(lockedUntil);

    return (
        <div className="login-page">
            <div className="login-card">
                <h1>Hospital HIS</h1>
                <h2>Iniciar Sesión</h2>

                {informationMessage && !errorMessage && (
                    <div
                        className="login-message login-message-info"
                        role="status"
                        aria-live="polite">
                        {informationMessage}
                    </div>
                )}

                <form onSubmit={handleLogin}>
                    {/* Nombre de usuario */}
                    <div className="form-group">
                        <label htmlFor="login-username">
                            Usuario
                        </label>

                        <input
                            id="login-username"
                            type="text"
                            value={username}
                            autoComplete="username"
                            placeholder="Ingrese su usuario"
                            className={
                                errorMessage
                                    ? "login-input-error"
                                    : ""
                            }
                            onChange={(event) => {
                                setUsername(
                                    event.target.value
                                );
                            }}
                            disabled={loading}
                        />
                    </div>

                    {/* Contraseña */}
                    <div className="form-group">
                        <label htmlFor="login-password">
                            Contraseña
                        </label>

                        <input
                            id="login-password"
                            type="password"
                            value={password}
                            autoComplete="current-password"
                            placeholder="Ingrese su contraseña"
                            className={
                                errorMessage
                                    ? "login-input-error"
                                    : ""
                            }
                            onChange={(event) => {
                                setPassword(
                                    event.target.value
                                );

                                clearLoginMessages();
                            }}
                            disabled={loading}
                        />
                    </div>

                    {/* Mensajes de autenticación */}
                    {errorMessage && (
                        <div
                            className={
                                blocked
                                    ? "login-message login-message-blocked"
                                    : "login-message login-message-error"
                            }
                            role="alert"
                            aria-live="assertive"
                        >
                            <div className="login-message-title">
                                {blocked
                                    ? "Acceso bloqueado"
                                    : "No fue posible iniciar sesión"}
                            </div>

                            <div className="login-message-description">
                                {errorMessage}
                            </div>

                            {/*
                             * Mostrar intentos únicamente si:
                             *
                             * 1. La cuenta no está bloqueada.
                             * 2. El backend devolvió el campo.
                             * 3. Todavía queda al menos un intento.
                             */}
                            {!blocked &&
                                remainingAttempts !== null &&
                                remainingAttempts > 0 && (
                                    <div className="attempts-message">
                                        Tiene{" "}
                                        <strong>
                                            {
                                                remainingAttempts
                                            }
                                        </strong>{" "}
                                        {remainingAttempts === 1
                                            ? "intento restante"
                                            : "intentos restantes"}{" "}
                                        antes del bloqueo
                                        temporal.
                                    </div>
                                )}

                            {/*
                             * Mostrar la fecha cuando existe
                             * un bloqueo temporal.
                             */}
                            {blocked &&
                                formattedLockedUntil && (
                                    <div className="blocked-until">
                                        Podrá intentar
                                        nuevamente después de:{" "}
                                        <strong>
                                            {
                                                formattedLockedUntil
                                            }
                                        </strong>
                                    </div>
                                )}

                            {/*
                             * Respaldo para un bloqueo donde
                             * el backend no envíe lockedUntil.
                             */}
                            {blocked &&
                                !formattedLockedUntil && (
                                    <div className="blocked-until">
                                        Espere el tiempo de
                                        bloqueo indicado antes de
                                        intentar nuevamente.
                                    </div>
                                )}
                        </div>
                    )}

                    {/* Botón de inicio */}
                    <button
                        type="submit"
                        className="btn btn-primary login-submit-button"
                        disabled={loading}
                    >
                        {loading
                            ? "Validando..."
                            : "Iniciar Sesión"}
                    </button>
                </form>

                <div className="login-actions">
                    <Link to="/">
                        Volver al portal
                    </Link>

                    <Link to="/register">
                        Registrarse
                    </Link>
                </div>
            </div>
        </div>
    );
}

export default Login;