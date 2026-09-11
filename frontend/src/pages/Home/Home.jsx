import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { verifyDpi } from "../../api/authApi";
import "./Home.css";

function Home() {
    const [showDpiModal, setShowDpiModal] = useState(false);
    const [dpi, setDpi] = useState("");
    const [dpiError, setDpiError] = useState("");
    const [serverMessage, setServerMessage] = useState("");
    const [messageType, setMessageType] = useState("");
    const [verifying, setVerifying] = useState(false);
    const navigate = useNavigate();

    const openDpiModal = () => {
        setDpi("");
        setDpiError("");
        setServerMessage("");
        setMessageType("");
        setShowDpiModal(true);
    };

    const closeDpiModal = () => {
        if (verifying) {
            return; // Prevent closing the modal while verifying
        }
        setShowDpiModal(false);
        setDpi("");
        setDpiError("");
        setServerMessage("");
        setMessageType("");
    };

    const handleDpiChange = (event) => {
        const numericValue = event.target.value.replace(/\D/g, "").slice(0, 13); // Remove non-numeric characters and  limit to 10 characters
        setDpi(numericValue);
        setDpiError(""); // Clear error message when user types
        setServerMessage(""); // Clear server message when user types
        setMessageType(""); // Clear message type when user types
    };

    const validateDpi = (dpi) => {
        if (!dpi.trim()) {
            setDpiError("El campo DPI es obligatorio. Por favor, ingrese su número de DPI.");
            return false;
        }
        if (!/^\d+$/.test(dpi)) {
            setDpiError("El DPI debe contener únicamente númweros.");
            return false;
        }
        if (dpi.length !== 13) {
            setDpiError("El DPI debe contener exactamente 13 dígitos.");
            return false;
        }
        return true;
    };

    const handleVerifyDpi = async (event) => {
        event.preventDefault();
        if (verifying) {
            return; // Prevent multiple submissions
        }
        setDpiError(""); // Clear previous error message
        setServerMessage(""); // Clear previous server message
        setMessageType(""); // Clear previous message type

        if (!validateDpi(dpi)) {
            return;
        }
        setVerifying(true);
        try {
            const response = await verifyDpi({ dpi });
            const result = response.data;

            switch (result.action) {
                case "LOGIN":
                    navigate("/login", {
                        state: {
                            username: result.username || "",
                            redirectTo: "/appointments/new",
                            fromAppointment: true,
                            verifiedDpi: dpi,
                            informationMessage:
                                result.message || "DPI verificado. Por favor, inicie sesión para continuar con el agendamiento de su cita.",
                        }
                    });
                    break;
                case "REGISTER":
                    const registerMessage = result.message || "No se encontró un registro asociado a este DPI. Será redirigido al formulario de registro.";
                    setMessageType("info");
                    setServerMessage(registerMessage);
                    await new Promise((resolve) => setTimeout(resolve, 2500)); // Wait for 2 seconds before redirecting
                    navigate("/register", {
                        state: {
                            dpi,
                            redirectTo: "/appointments/new",
                            fromAppointment: true,
                            informationMessage: registerMessage
                        }
                    });
                    break;

                case "INTERNAL_USER":
                    setMessageType("warning");
                    setServerMessage(
                        result.message || "Este DPI pertenece a un usuario del sistema interno. Por favor conacte a recepción."
                    );
                    break;
                case "INACTIVE":
                    setMessageType("warning");
                    setServerMessage(
                        result.message || "El DPI ingresado está asociado a un usuario inactivo. Por favor, contacte al personal del hospital para asistencia."
                    );
                    break;
                default:
                    setMessageType("error");
                    setServerMessage(
                        result.message || "Ocurrió un error inesperado. Por favor, inténtelo de nuevo más tarde."
                    );
                    break;
            }
        } catch (error) {
            console.error("Error al verificar el DPI:", error);
            setMessageType("error");
            if (error.respone) {
                setServerMessage(getBackendErrorMessage(error));
            } else {
                setServerMessage("No fue posible comunicarse con el servidor. Por favor, inténtelo de nuevo más tarde.");
            }
        } finally {
            setVerifying(false);
        }
    };

    return (
        <div className="home-page">
            <header className="home-header">
                <div className="home-logo">
                    <h1>Hospital HIS</h1>
                </div>
                <nav className="home-nav">
                    <a href="#services">Servicios</a>
                    <a href="#specialties">Especialidades</a>
                    <a href="#contact">Contacto</a>
                    <Link to="/login">Iniciar Sesión</Link>
                    <Link to="/register" className="nav-register">Registrarse</Link>
                </nav>
            </header>

            <section className="hero-section">
                <div className="hero-content">
                    <h1>Sistema Informático Hospitalario</h1>

                    <p>Consulta servicios, especialidades médicas y accede al portal para gestionar tus citas de forma rápida y segura.</p>
                    <div className="hero-actions">
                        <Link to="/login" className="btn-primary">
                            Iniciar Sesión
                        </Link>

                        <Link to="/register" className="btn-secondary">
                            Registrarse
                        </Link>

                        <button type="button" className="btn-outline" onClick={openDpiModal}>
                            Agendar Cita
                        </button>

                    </div>
                </div>
            </section>

            <section id="services" className="section">
                <h2>Servicios del Hospital</h2>
                <div className="cards-container">
                    <div className="info-card">
                        <h3>Consulta Médica</h3>
                        <p>Atención médica general y especializada para pacientes registrados en el sistema.</p>
                    </div>

                    <div className="info-card">
                        <h3>Laboratorio Clínico</h3>
                        <p>Gestión de órdenes, toma de muestras y consulta de  resultados de laboratorio.</p>
                    </div>

                    <div className="info-card">
                        <h3>Farmacia</h3>
                        <p>Despacho de medicamentos recetados por médicos del hospital.</p>
                    </div>
                </div>
            </section>

            <section id="specialties" className="section section-alt">
                <h2>Especialidades Disponibles</h2>

                <div className="cards-container">
                    <div className="info-card">
                        <h3>Medicina General</h3>
                        <p>Atención inicial, diagnóstico general y seguimiento médico.</p>
                    </div>

                    <div className="info-card">
                        <h3>Cardiología</h3>
                        <p>Evaluación y seguimiento de enfermedades del corazón.</p>
                    </div>

                    <div className="info-card">
                        <h3>Pediatría</h3>
                        <p>Atención médica enfocada en pacientes pediátricos.</p>
                    </div>
                </div>
            </section>

            <section id="contact" className="section">
                <h2>Información del Hospital</h2>

                <div className="contact-box">
                    <p><strong>Horario de atención:</strong> Lunes a viernes de 8:00 a 17:00</p>

                    <p><strong>Ubicación:</strong> Sede Central</p>

                    <p><strong>Teléfono:</strong> 0000-0000</p>
                </div>
            </section>

            <footer className="home-footer">
                <p>Sistema Informático Hospitalario HIS</p>
            </footer>

            {/* Modal de verificación de DPI */}
            {showDpiModal && (
                <div
                    className="dpi-modal-overlay"
                    role="presentation"
                    onMouseDown={(event) => {
                        /*
                         * Cierra el modal cuando la persona presiona
                         * directamente sobre el fondo oscuro.
                         */
                        if (event.target === event.currentTarget) {
                            closeDpiModal();
                        }
                    }}
                >
                    <div
                        className="dpi-modal"
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="dpi-modal-title"
                    >
                        {/* Botón para cerrar el modal */}
                        <button
                            type="button"
                            className="dpi-modal-close"
                            onClick={closeDpiModal}
                            disabled={verifying}
                            aria-label="Cerrar ventana"
                        >
                            ×
                        </button>

                        {/* Título */}
                        <h2 id="dpi-modal-title">
                            Verificar Registro
                        </h2>

                        {/* Descripción solicitada por CU-00 */}
                        <p className="dpi-modal-description">
                            Ingrese su número de DPI para verificar si
                            está registrado en el sistema.
                        </p>

                        {/* Formulario de verificación */}
                        <form onSubmit={handleVerifyDpi}>
                            <div className="dpi-form-group">
                                <label htmlFor="appointment-dpi">
                                    Número de DPI
                                </label>

                                <input
                                    id="appointment-dpi"
                                    type="text"
                                    inputMode="numeric"
                                    autoComplete="off"
                                    maxLength={13}
                                    value={dpi}
                                    placeholder="Ingrese los 13 dígitos"
                                    className={
                                        dpiError
                                            ? "dpi-input-error"
                                            : ""
                                    }
                                    onChange={handleDpiChange}
                                    disabled={verifying}
                                    autoFocus
                                />

                                {/* Contador de dígitos */}
                                <div
                                    className={
                                        dpi.length === 13
                                            ? "dpi-counter dpi-counter-valid"
                                            : "dpi-counter"
                                    }
                                >
                                    {dpi.length}/13 dígitos
                                </div>

                                {/* Mensaje de validación local */}
                                {dpiError && (
                                    <div
                                        className="dpi-error-message"
                                        role="alert"
                                    >
                                        {dpiError}
                                    </div>
                                )}
                            </div>

                            {/* Mensaje enviado por el backend */}
                            {serverMessage && (
                                <div
                                    className={`dpi-server-message ${messageType === "warning"
                                        ? "dpi-server-warning"
                                        : messageType === "info"
                                            ? "dpi-server-info"
                                            : "dpi-server-error"
                                        }`}
                                    role="status"
                                    aria-live="polite"
                                >
                                    {serverMessage}

                                    {messageType === "info" && (
                                        <div className="dpi-redirect-message">
                                            Redirigiendo...
                                        </div>
                                    )}
                                </div>
                            )}
                            {/* Botones del modal */}
                            <div className="dpi-modal-actions">
                                <button
                                    type="button"
                                    className="dpi-cancel-button"
                                    onClick={closeDpiModal}
                                    disabled={verifying}
                                >
                                    Cancelar
                                </button>

                                <button
                                    type="submit"
                                    className="dpi-verify-button"
                                    disabled={verifying}
                                >
                                    {verifying
                                        ? "Verificando..."
                                        : "Verificar DPI"}
                                </button>
                            </div>
                        </form>
                    </div>
                </div >
            )
            }

        </div >
    );
}
export default Home;
