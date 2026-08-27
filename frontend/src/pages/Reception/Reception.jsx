import { useState } from "react";
import { useNavigate } from "react-router-dom";

import MainLayout
    from "../../layouts/MainLayout";

import {
    registerPatientArrival,
    searchReceptionAppointment
} from "../../api/receptionApi";

import "./Reception.css";
import ReassignDoctorModal
    from "./ReassignDoctorModal";

import EmergencyReceptionModal from "./EmergencyReceptionModal";

function Reception() {
    const navigate = useNavigate();

    const [searchType, setSearchType] =
        useState("APPOINTMENT_ID");

    const [searchValue, setSearchValue] =
        useState("");

    const [searchResult, setSearchResult] =
        useState(null);

    const [loading, setLoading] =
        useState(false);

    const [registeringArrival, setRegisteringArrival] =
        useState(false);

    const [errorMessage, setErrorMessage] =
        useState("");

    const [successMessage, setSuccessMessage] =
        useState("");

    const [showReassignModal, setShowReassignModal] =
        useState(false);

    const [showEmergencyModal, setShowEmergencyModal] = useState(false);

    const [emergencyResult, setEmergencyResult] = useState(null);

    const maskDpi = (dpi) => {
        if (!dpi) {
            return "No registrado";
        }

        const cleanDpi = String(dpi).trim();

        if (cleanDpi.length <= 4) {
            return cleanDpi;
        }

        const hiddenDigits = "*".repeat(
            cleanDpi.length - 4
        );

        const lastFourDigits = cleanDpi.slice(-4);

        return `${hiddenDigits}${lastFourDigits}`;
    };

    const validateSearch = () => {
        const cleanValue =
            searchValue.trim();

        if (!cleanValue) {
            setErrorMessage(
                "Debe ingresar un criterio de búsqueda."
            );

            return false;
        }

        if (cleanValue.length > 25) {
            setErrorMessage(
                "El criterio de búsqueda no puede exceder los 25 caracteres."
            );

            return false;
        }

        if (
            searchType === "APPOINTMENT_ID" &&
            !/^\d+$/.test(cleanValue)
        ) {
            setErrorMessage(
                "El número de cita debe contener únicamente números."
            );

            return false;
        }

        if (
            searchType === "DPI" &&
            !/^\d{13}$/.test(cleanValue)
        ) {
            setErrorMessage(
                "El DPI debe contener exactamente 13 dígitos numéricos."
            );

            return false;
        }

        setErrorMessage("");
        return true;
    };

    const handleSearch = async (event) => {
        event.preventDefault();

        setSuccessMessage("");
        setSearchResult(null);

        if (!validateSearch()) {
            return;
        }

        try {
            setLoading(true);

            const response =
                await searchReceptionAppointment(
                    searchType,
                    searchValue.trim()
                );

            setSearchResult(response.data);

        } catch (error) {
            console.error(error);

            setErrorMessage(
                error.response?.data?.message ||
                "No fue posible realizar la búsqueda."
            );

        } finally {
            setLoading(false);
        }
    };

    const handleRegisterArrival = async () => {
        const appointment =
            searchResult?.appointment;

        if (!appointment) {
            return;
        }

        try {
            setRegisteringArrival(true);
            setErrorMessage("");
            setSuccessMessage("");

            const response =
                await registerPatientArrival(
                    appointment.appointmentId
                );

            setSuccessMessage(
                response.data.message
            );

            setSearchResult(previous => ({
                ...previous,
                message: response.data.message,
                subText:
                    "Llegada registrada — esperando llamado de enfermería.",
                appointment: response.data
            }));

        } catch (error) {
            console.error(error);

            setErrorMessage(
                error.response?.data?.message ||
                "Error al registrar la llegada."
            );

        } finally {
            setRegisteringArrival(false);
        }
    };

    const clearSearch = () => {
        setSearchValue("");
        setSearchResult(null);
        setErrorMessage("");
        setSuccessMessage("");
    };

    const formatDateTime = (value) => {
        if (!value) {
            return "No registrada";
        }

        return new Date(value)
            .toLocaleString(
                "es-GT",
                {
                    year: "numeric",
                    month: "long",
                    day: "numeric",
                    hour: "2-digit",
                    minute: "2-digit"
                }
            );
    };

    return (
        <MainLayout>
            <div className="reception-page">
                <div className="reception-header">
                    <div>
                        <h1>
                            Recepción y Verificación de Cita
                        </h1>

                        <p>
                            Busque la cita del paciente y registre
                            su llegada a la clínica.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="register-emergency-button"
                        onClick={() => {
                            setEmergencyResult(null);
                            setShowEmergencyModal(true);
                        }}
                    >
                        Registrar Emergencia
                    </button>
                </div>

                <section className="reception-search-card">
                    <div className="search-type-buttons">
                        <button
                            type="button"
                            className={
                                searchType === "APPOINTMENT_ID"
                                    ? "search-type-button active"
                                    : "search-type-button"
                            }
                            onClick={() => {
                                setSearchType(
                                    "APPOINTMENT_ID"
                                );

                                clearSearch();
                            }}
                        >
                            Por No. Cita
                        </button>

                        <button
                            type="button"
                            className={
                                searchType === "DPI"
                                    ? "search-type-button active"
                                    : "search-type-button"
                            }
                            onClick={() => {
                                setSearchType("DPI");
                                clearSearch();
                            }}
                        >
                            Por DPI
                        </button>
                    </div>

                    <form className="reception-search-form" onSubmit={handleSearch}>
                        <input
                            type="text"
                            maxLength="25"
                            placeholder={
                                searchType === "DPI"
                                    ? "Ingrese DPI de 13 dígitos"
                                    : "Ingrese número de cita"
                            }
                            value={searchValue}
                            className={
                                errorMessage && !searchResult
                                    ? "input-error"
                                    : ""
                            }
                            onChange={(event) => {
                                const value =
                                    event.target.value
                                        .replace(/\D/g, "");

                                setSearchValue(value);
                                setErrorMessage("");
                            }}
                        />

                        <button
                            type="submit"
                            className="reception-search-button"
                            disabled={loading}
                        >
                            {loading
                                ? "Buscando..."
                                : "Buscar"}
                        </button>

                        <button
                            type="button"
                            className="reception-clear-button"
                            onClick={clearSearch}
                        >
                            Limpiar
                        </button>
                    </form>
                </section>

                {errorMessage && (
                    <div className="reception-message error">
                        {errorMessage}
                    </div>
                )}

                {successMessage && (
                    <div className="reception-message success">
                        {successMessage}
                    </div>
                )}

                {searchResult && (
                    <ReceptionResult
                        searchResult={searchResult}
                        registeringArrival={registeringArrival}
                        formatDateTime={formatDateTime}
                        onRegisterArrival={
                            handleRegisterArrival
                        }
                        onRegisterPatient={() =>
                            navigate("/register")
                        }
                        onNewAppointment={() =>
                            navigate("/appointments/new")
                        }
                        onOpenReassign={() =>
                            setShowReassignModal(true)
                        }
                    />
                )}

                {showReassignModal && searchResult?.appointment && (
                    <ReassignDoctorModal
                        appointment={
                            searchResult.appointment
                        }
                        onClose={() =>
                            setShowReassignModal(false)
                        }
                        onReassigned={(updatedAppointment) => {
                            setSearchResult(previous => ({
                                ...previous,

                                message:
                                    "Médico reasignado correctamente.",

                                subText:
                                    previous.subText,

                                appointment:
                                    updatedAppointment
                            }));

                            setSuccessMessage(
                                "Médico reasignado correctamente."
                            );

                            setErrorMessage("");

                            setShowReassignModal(false);
                        }}
                    />
                )}

                {showEmergencyModal && (
                    <EmergencyReceptionModal
                        onClose={() =>
                            setShowEmergencyModal(false)
                        }
                        onRegistered={(result) => {
                            setEmergencyResult(result);
                            setShowEmergencyModal(false);

                            setSuccessMessage(
                                result.message
                            );

                            setErrorMessage("");
                            setSearchResult(null);
                        }}
                    />
                )}

                {emergencyResult && (
                    <section className="emergency-result-card">
                        <div className="emergency-result-header">
                            <div>
                                <h2>
                                    {emergencyResult.patientName}
                                </h2>

                                <p>
                                    Registro de emergencia
                                    #{emergencyResult.emergencyReceptionId}
                                </p>
                            </div>

                            <span className="emergency-priority-badge">
                                EMERGENCIA
                            </span>
                        </div>

                        <div className="emergency-result-details">
                            <div>
                                <span>DPI</span>

                                <strong>
                                    {maskDpi(
                                        emergencyResult.patientDpi
                                    )}
                                </strong>
                            </div>

                            <div>
                                <span>Sucursal</span>

                                <strong>
                                    {emergencyResult.branch}
                                </strong>
                            </div>

                            <div>
                                <span>Estado</span>

                                <strong>
                                    {emergencyResult.status
                                        .replaceAll("_", " ")}
                                </strong>
                            </div>

                            <div>
                                <span>Hora de llegada</span>

                                <strong>
                                    {formatDateTime(
                                        emergencyResult.arrivalTime
                                    )}
                                </strong>
                            </div>

                            <div className="emergency-detail-wide">
                                <span>Nota</span>

                                <strong>
                                    {emergencyResult.emergencyNote ||
                                        "Sin nota adicional"}
                                </strong>
                            </div>
                        </div>

                        <div className="emergency-next-step">
                            <strong>
                                Atención inmediata requerida
                            </strong>

                            <p>
                                El paciente debe pasar directamente
                                a toma de signos vitales.
                            </p>
                        </div>

                        <button
                            type="button"
                            className="urgent-button"
                            disabled
                        >
                            Signos Vitales Urgente
                        </button>
                    </section>
                )}
            </div>
        </MainLayout>
    );
}

function ReceptionResult({
    searchResult,
    registeringArrival,
    formatDateTime,
    onRegisterArrival,
    onRegisterPatient,
    onNewAppointment,
    onOpenReassign
}) {
    const appointment =
        searchResult.appointment;

    if (!appointment) {
        return (
            <section className="reception-result-card empty-result">
                <h2>
                    {searchResult.message}
                </h2>

                <p>
                    {searchResult.subText}
                </p>

                {searchResult.showRegisterPatientButton && (
                    <button
                        type="button"
                        className="btn btn-primary"
                        onClick={onRegisterPatient}
                    >
                        Registrar Paciente
                    </button>
                )}

                {searchResult.showNewAppointmentButton && (
                    <button
                        type="button"
                        className="btn btn-primary"
                        onClick={onNewAppointment}
                    >
                        Nueva Cita Walk-in
                    </button>
                )}
            </section>
        );
    }

    return (
        <section className="reception-result-card">
            <div className="reception-result-header">
                <div>
                    <h2>
                        {appointment.patientName}
                    </h2>

                    <p>
                        Cita #{appointment.appointmentId}
                    </p>
                </div>

                <div className="reception-labels">
                    <span
                        className={
                            appointment.priority === "EMERGENCIA"
                                ? "priority-label emergency"
                                : "priority-label normal"
                        }
                    >
                        {appointment.priority}
                    </span>

                    <span
                        className={`reception-status ${appointment.status
                            .toLowerCase()
                            .replaceAll("_", "-")}`}
                    >
                        {appointment.status
                            .replaceAll("_", " ")}
                    </span>
                </div>
            </div>

            <div className="reception-details">
                <div>
                    <span>DPI</span>
                    <strong>
                        {appointment.patientDpi ||
                            "No registrado"}
                    </strong>
                </div>

                <div>
                    <span>Médico</span>
                    <strong>
                        {appointment.doctorName}
                    </strong>
                </div>

                <div>
                    <span>Especialidad</span>
                    <strong>
                        {appointment.specialty}
                    </strong>
                </div>

                <div>
                    <span>Sucursal</span>
                    <strong>
                        {appointment.branch}
                    </strong>
                </div>

                <div>
                    <span>Fecha de cita</span>
                    <strong>
                        {formatDateTime(
                            appointment.appointmentDateTime
                        )}
                    </strong>
                </div>

                <div>
                    <span>Hora de llegada</span>
                    <strong>
                        {formatDateTime(
                            appointment.arrivalTime
                        )}
                    </strong>
                </div>

                <div className="detail-wide">
                    <span>Motivo de consulta</span>
                    <strong>
                        {appointment.reason}
                    </strong>
                </div>
            </div>

            <div className="reception-information">
                <strong>
                    {searchResult.message}
                </strong>

                <p>
                    {searchResult.subText}
                </p>
            </div>

            <div className="reception-actions">
                {appointment.canRegisterArrival && (
                    <button
                        type="button"
                        className="btn btn-primary"
                        disabled={registeringArrival}
                        onClick={onRegisterArrival}
                    >
                        {registeringArrival
                            ? "Registrando..."
                            : "Registrar llegada"}
                    </button>
                )}

                {(
                    appointment.status === "CONFIRMADA" ||
                    appointment.status === "PACIENTE_PRESENTE"
                ) && (
                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={onOpenReassign}
                        >
                            Reasignar Médico
                        </button>
                    )}

                {searchResult.showNewAppointmentButton && (
                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={onNewAppointment}
                    >
                        Nueva Cita
                    </button>
                )}

                {appointment.priority === "EMERGENCIA" &&
                    appointment.arrivalRegistered && (
                        <button
                            type="button"
                            className="urgent-button"
                            disabled
                        >
                            Signos Vitales Urgente
                        </button>
                    )}


            </div>
        </section>
    );
}

export default Reception;