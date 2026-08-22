import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import MainLayout
    from "../../layouts/MainLayout";

import {
    getMyAppointments
} from "../../api/appointmentApi";

import "./MyAppointments.css";

function MyAppointments() {
    const navigate = useNavigate();

    const [appointments, setAppointments] =
        useState([]);

    const [statusFilter, setStatusFilter] =
        useState("TODAS");

    const [loading, setLoading] =
        useState(true);

    const [errorMessage, setErrorMessage] =
        useState("");

    useEffect(() => {
        loadAppointments();
    }, []);

    const loadAppointments = async () => {
        try {
            setLoading(true);
            setErrorMessage("");

            const response =
                await getMyAppointments();

            setAppointments(response.data);

        } catch (error) {
            console.error(error);

            setErrorMessage(
                error.response?.data?.message ||
                "No fue posible cargar sus citas."
            );

        } finally {
            setLoading(false);
        }
    };

    const filteredAppointments =
        statusFilter === "TODAS"
            ? appointments
            : appointments.filter(
                appointment =>
                    appointment.status === statusFilter
            );

    const formatDateTime = (dateTime) => {
        if (!dateTime) {
            return "No disponible";
        }

        return new Date(dateTime)
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

    const getStatusLabel = (status) => {
        const labels = {
            PENDIENTE_DE_PAGO:
                "Pendiente de pago",

            PAGADA:
                "Pagada",

            CONFIRMADA:
                "Confirmada",

            CANCELADA:
                "Cancelada",

            EXPIRADA:
                "Expirada"
        };

        return labels[status] || status;
    };

    const getStatusClass = (status) => {
        const classes = {
            PENDIENTE_DE_PAGO:
                "appointment-status pending",

            PAGADA:
                "appointment-status paid",

            CONFIRMADA:
                "appointment-status confirmed",

            CANCELADA:
                "appointment-status cancelled",

            EXPIRADA:
                "appointment-status expired"
        };

        return classes[status] ||
            "appointment-status";
    };

    return (
        <MainLayout>
            <div className="my-appointments-page">
                <div className="appointments-header">
                    <div>
                        <h1>Mis Citas</h1>

                        <p>
                            Consulte el estado de sus citas médicas.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="btn btn-primary"
                        onClick={() =>
                            navigate(
                                "/appointments/new"
                            )
                        }
                    >
                        Agendar nueva cita
                    </button>
                </div>

                <div className="appointments-toolbar">
                    <label htmlFor="status-filter">
                        Filtrar por estado
                    </label>

                    <select
                        id="status-filter"
                        value={statusFilter}
                        onChange={(event) =>
                            setStatusFilter(
                                event.target.value
                            )
                        }
                    >
                        <option value="TODAS">
                            Todas
                        </option>

                        <option value="PENDIENTE_DE_PAGO">
                            Pendiente de pago
                        </option>

                        <option value="PAGADA">
                            Pagada
                        </option>

                        <option value="CONFIRMADA">
                            Confirmada
                        </option>

                        <option value="CANCELADA">
                            Cancelada
                        </option>

                        <option value="EXPIRADA">
                            Expirada
                        </option>
                    </select>
                </div>

                {errorMessage && (
                    <div className="appointments-error">
                        {errorMessage}
                    </div>
                )}

                {loading ? (
                    <p>Cargando citas...</p>

                ) : filteredAppointments.length === 0 ? (
                    <div className="appointments-empty">
                        <h2>
                            No se encontraron citas
                        </h2>

                        <p>
                            No tiene citas registradas con el
                            estado seleccionado.
                        </p>

                        <button
                            type="button"
                            className="btn btn-primary"
                            onClick={() =>
                                navigate(
                                    "/appointments/new"
                                )
                            }
                        >
                            Agendar una cita
                        </button>
                    </div>

                ) : (
                    <div className="appointments-grid">
                        {filteredAppointments.map(
                            appointment => (
                                <article
                                    key={appointment.id}
                                    className="appointment-card"
                                >
                                    <div className="appointment-card-header">
                                        <span>
                                            Cita #{appointment.id}
                                        </span>

                                        <span
                                            className={
                                                getStatusClass(
                                                    appointment.status
                                                )
                                            }
                                        >
                                            {getStatusLabel(
                                                appointment.status
                                            )}
                                        </span>
                                    </div>

                                    <div className="appointment-information">
                                        <p>
                                            <span>Médico</span>

                                            <strong>
                                                {appointment.doctorName}
                                            </strong>
                                        </p>

                                        <p>
                                            <span>Especialidad</span>

                                            <strong>
                                                {appointment.specialty}
                                            </strong>
                                        </p>

                                        <p>
                                            <span>Sucursal</span>

                                            <strong>
                                                {appointment.branch}
                                            </strong>
                                        </p>

                                        <p>
                                            <span>Fecha y hora</span>

                                            <strong>
                                                {formatDateTime(
                                                    appointment
                                                        .appointmentDateTime
                                                )}
                                            </strong>
                                        </p>

                                        <p>
                                            <span>Motivo</span>

                                            <strong>
                                                {appointment.reason}
                                            </strong>
                                        </p>
                                    </div>

                                    {appointment.status ===
                                        "PENDIENTE_DE_PAGO" && (
                                            <button
                                                type="button"
                                                className="btn btn-primary"
                                                onClick={() => {
                                                    sessionStorage.setItem(
                                                        "pendingAppointment",
                                                        JSON.stringify(
                                                            appointment
                                                        )
                                                    );

                                                    navigate(
                                                        `/payments/${appointment.id}`
                                                    );
                                                }}
                                            >
                                                Continuar con el pago
                                            </button>
                                        )}
                                </article>
                            )
                        )}
                    </div>
                )}
            </div>
        </MainLayout>
    );
}

export default MyAppointments;