import { useEffect, useState } from "react";
import MainLayout from "../../layouts/MainLayout";

import {
    getAppointmentBranches,
    getSpecialtiesByBranch,
    getDoctors,
    getAvailability,
    createAppointment
} from "../../api/appointmentApi";

import "./AppointmentWizard.css";

function AppointmentWizard() {
    const [currentStep, setCurrentStep] = useState(1);

    const [branches, setBranches] = useState([]);
    const [specialties, setSpecialties] = useState([]);
    const [doctors, setDoctors] = useState([]);
    const [availableSlots, setAvailableSlots] = useState([]);

    const [branchId, setBranchId] = useState("");
    const [specialtyId, setSpecialtyId] = useState("");
    const [doctorId, setDoctorId] = useState("");
    const [selectedDate, setSelectedDate] = useState("");
    const [selectedDateTime, setSelectedDateTime] = useState("");

    const [reason, setReason] = useState("");
    const [errorMessage, setErrorMessage] = useState("");
    const [loading, setLoading] = useState(false);
    const [reasonError, setReasonError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    useEffect(() => {
        loadBranches();
    }, []);

    const loadBranches = async () => {
        try {
            setLoading(true);
            setErrorMessage("");

            const response =
                await getAppointmentBranches();

            const activeBranches =
                response.data.filter(
                    branch => branch.active
                );

            setBranches(activeBranches);

        } catch (error) {
            console.error(error);

            setErrorMessage(
                error.response?.data?.message ||
                "No fue posible cargar las sucursales."
            );

        } finally {
            setLoading(false);
        }
    };

    const loadSpecialties = async () => {
        if (!branchId) {
            setErrorMessage(
                "Debe seleccionar una sucursal."
            );

            return;
        }

        try {
            setLoading(true);
            setErrorMessage("");

            const response =
                await getSpecialtiesByBranch(
                    Number(branchId)
                );

            setSpecialties(response.data);

            setSpecialtyId("");
            setDoctorId("");
            setSelectedDate("");
            setSelectedDateTime("");

            setDoctors([]);
            setAvailableSlots([]);

            setCurrentStep(2);

        } catch (error) {
            console.error(error);

            setSpecialties([]);

            setErrorMessage(
                error.response?.data?.message ||
                "No fue posible cargar las especialidades de la sucursal seleccionada."
            );

        } finally {
            setLoading(false);
        }
    };

    const loadDoctors = async () => {
        if (!branchId) {
            setErrorMessage(
                "Debe seleccionar una sucursal."
            );

            setCurrentStep(1);
            return;
        }

        if (!specialtyId) {
            setErrorMessage(
                "Debe seleccionar una especialidad."
            );

            return;
        }

        try {
            setLoading(true);
            setErrorMessage("");

            const response = await getDoctors(
                Number(branchId),
                Number(specialtyId)
            );

            setDoctors(response.data);

            setDoctorId("");
            setSelectedDate("");
            setSelectedDateTime("");
            setAvailableSlots([]);

            setCurrentStep(3);

        } catch (error) {
            console.error(error);

            setDoctors([]);
            setDoctorId("");

            setErrorMessage(
                error.response?.data?.message ||
                "No fue posible cargar los médicos disponibles."
            );

        } finally {
            setLoading(false);
        }
    };

    const loadAvailability = async () => {
        if (!doctorId) {
            setErrorMessage(
                "Debe seleccionar un médico."
            );
            return;
        }

        if (!selectedDate) {
            setErrorMessage(
                "Debe seleccionar una fecha."
            );
            return;
        }

        try {
            setLoading(true);
            setErrorMessage("");

            const response = await getAvailability(
                Number(doctorId),
                selectedDate
            );

            setAvailableSlots(response.data);
            setSelectedDateTime("");

        } catch (error) {
            console.error(error);

            setAvailableSlots([]);
            setSelectedDateTime("");

            setErrorMessage(
                error.response?.data?.message ||
                "No fue posible cargar los horarios disponibles."
            );

        } finally {
            setLoading(false);
        }
    };
    const today = new Date().toISOString().split("T")[0];

    const selectedBranch = branches.find(
        branch => branch.id === Number(branchId)
    );

    const selectedSpecialty = specialties.find(
        specialty => specialty.id === Number(specialtyId)
    );

    const selectedDoctor = doctors.find(
        doctor => doctor.id === Number(doctorId)
    );

    const selectedSlot = availableSlots.find(
        slot => slot.dateTime === selectedDateTime
    );

    const validateReason = () => {
        const cleanReason = reason.trim();

        if (!cleanReason) {
            setReasonError(
                "El motivo de consulta es obligatorio."
            );

            return false;
        }

        if (cleanReason.length < 10) {
            setReasonError(
                `El motivo debe contener al menos 10 caracteres. Usted ingresó ${cleanReason.length} caracteres.`
            );

            return false;
        }

        if (cleanReason.length > 2000) {
            setReasonError(
                `El motivo no puede exceder los 2000 caracteres. Usted ingresó ${cleanReason.length} caracteres.`
            );

            return false;
        }

        setReasonError("");
        return true;
    };

    const handleConfirmAppointment = async () => {
        setErrorMessage("");
        setSuccessMessage("");

        if (!validateReason()) {
            return;
        }

        if (!branchId) {
            setErrorMessage(
                "Debe seleccionar una sucursal."
            );
            return;
        }

        if (!specialtyId) {
            setErrorMessage(
                "Debe seleccionar una especialidad."
            );
            return;
        }

        if (!doctorId) {
            setErrorMessage(
                "Debe seleccionar un médico."
            );
            return;
        }

        if (!selectedDateTime) {
            setErrorMessage(
                "Debe seleccionar una fecha y hora."
            );
            return;
        }

        const patientUserId =
            localStorage.getItem("userId");

        if (!patientUserId) {
            setErrorMessage(
                "No se pudo identificar al paciente autenticado. Inicie sesión nuevamente."
            );
            return;
        }

        try {
            setLoading(true);

            const response = await createAppointment({
                patientUserId: Number(patientUserId),
                doctorUserId: Number(doctorId),
                branchId: Number(branchId),
                specialtyId: Number(specialtyId),
                appointmentDateTime: selectedDateTime,
                reason: reason.trim()
            });

            setSuccessMessage(
                "Su cita ha sido registrada exitosamente. Será redirigido al proceso de pago para confirmar la reserva."
            );

            console.log(
                "Cita registrada:",
                response.data
            );

            /*
             * El CU-04 todavía no está implementado.
             * De momento, dejamos preparada la redirección.
             */
            setTimeout(() => {
                // navigate(`/payments/${response.data.id}`);
                console.log(
                    "Pendiente redirección al CU-04. Cita:",
                    response.data.id
                );
            }, 1500);

        } catch (error) {
            console.error(error);

            setErrorMessage(
                error.response?.data?.message ||
                "No fue posible registrar la cita."
            );

        } finally {
            setLoading(false);
        }
    };

    return (
        <MainLayout>
            <div className="appointment-wizard">
                <h1>Agendar Cita Médica</h1>

                <p>
                    Paso {currentStep} de 5
                </p>

                {errorMessage && (
                    <div className="wizard-error">
                        {errorMessage}
                    </div>
                )}

                {loading && (
                    <p>Cargando información...</p>
                )}

                <div className="wizard-progress">
                    <div className={currentStep >= 1 ? "step active" : "step"}>
                        1. Sucursal
                    </div>

                    <div className={currentStep >= 2 ? "step active" : "step"}>
                        2. Especialidad
                    </div>

                    <div className={currentStep >= 3 ? "step active" : "step"}>
                        3. Médico
                    </div>

                    <div className={currentStep >= 4 ? "step active" : "step"}>
                        4. Fecha y hora
                    </div>

                    <div className={currentStep >= 5 ? "step active" : "step"}>
                        5. Confirmar
                    </div>
                </div>

                {currentStep === 1 && (
                    <section className="wizard-card">
                        <h2>Seleccione una sucursal</h2>

                        <select
                            value={branchId}
                            onChange={(event) => {
                                setBranchId(event.target.value);

                                setSpecialtyId("");
                                setDoctorId("");
                                setSelectedDate("");
                                setSelectedDateTime("");

                                setSpecialties([]);
                                setDoctors([]);
                                setAvailableSlots([]);
                            }}
                        >
                            <option value="">
                                Seleccione una sucursal
                            </option>

                            {branches.map(branch => (
                                <option
                                    key={branch.id}
                                    value={branch.id}
                                >
                                    {branch.name}
                                </option>
                            ))}
                        </select>

                        <div className="wizard-actions">
                            <button
                                type="button"
                                className="btn btn-primary"
                                disabled={!branchId || loading}
                                onClick={loadSpecialties}
                            >
                                {loading
                                    ? "Cargando..."
                                    : "Continuar"}
                            </button>
                        </div>
                    </section>
                )}

                {currentStep === 2 && (
                    <section className="wizard-card">
                        <h2>
                            Seleccione una especialidad
                        </h2>

                        <p className="wizard-description">
                            Elija una especialidad disponible en la sucursal seleccionada.
                        </p>

                        {specialties.length === 0 ? (
                            <div className="wizard-empty">
                                No hay especialidades disponibles para la sucursal seleccionada.
                            </div>
                        ) : (
                            <div className="wizard-options">
                                {specialties.map(specialty => (
                                    <button
                                        type="button"
                                        key={specialty.id}
                                        className={
                                            Number(specialtyId) === specialty.id
                                                ? "wizard-option selected"
                                                : "wizard-option"
                                        }
                                        onClick={() => {
                                            setSpecialtyId(
                                                String(specialty.id)
                                            );

                                            setDoctorId("");
                                            setSelectedDate("");
                                            setSelectedDateTime("");

                                            setDoctors([]);
                                            setAvailableSlots([]);
                                            setErrorMessage("");
                                        }}
                                    >
                                        <strong>
                                            {specialty.name}
                                        </strong>

                                        {specialty.description && (
                                            <span>
                                                {specialty.description}
                                            </span>
                                        )}
                                    </button>
                                ))}
                            </div>
                        )}

                        <div className="wizard-actions">
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => {
                                    setCurrentStep(1);

                                    setSpecialtyId("");
                                    setDoctorId("");
                                    setSelectedDate("");
                                    setSelectedDateTime("");

                                    setDoctors([]);
                                    setAvailableSlots([]);
                                    setErrorMessage("");
                                }}
                            >
                                Volver
                            </button>

                            <button
                                type="button"
                                className="btn btn-primary"
                                disabled={!specialtyId || loading}
                                onClick={loadDoctors}
                            >
                                {loading
                                    ? "Cargando médicos..."
                                    : "Continuar"}
                            </button>
                        </div>
                    </section>
                )}

                {currentStep === 3 && (
                    <section className="wizard-card">
                        <h2>Seleccione un médico</h2>

                        <p className="wizard-description">
                            Elija un médico disponible para la sucursal y especialidad seleccionadas.
                        </p>

                        {doctors.length === 0 ? (
                            <div className="wizard-empty">
                                No se encontraron médicos disponibles para la selección realizada.
                            </div>
                        ) : (
                            <div className="wizard-options">
                                {doctors.map(doctor => (
                                    <button
                                        type="button"
                                        key={doctor.id}
                                        className={
                                            Number(doctorId) === doctor.id
                                                ? "wizard-option selected"
                                                : "wizard-option"
                                        }
                                        onClick={() => {
                                            setDoctorId(
                                                String(doctor.id)
                                            );

                                            setSelectedDate("");
                                            setSelectedDateTime("");
                                            setAvailableSlots([]);
                                            setErrorMessage("");
                                        }}
                                    >
                                        <strong>
                                            {doctor.fullName}
                                        </strong>

                                        {doctor.specialty && (
                                            <span>
                                                Especialidad: {doctor.specialty}
                                            </span>
                                        )}

                                        {doctor.branch && (
                                            <span>
                                                Sucursal: {doctor.branch}
                                            </span>
                                        )}
                                    </button>
                                ))}
                            </div>
                        )}

                        <div className="wizard-actions">
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => {
                                    setCurrentStep(2);

                                    setDoctorId("");
                                    setSelectedDate("");
                                    setSelectedDateTime("");
                                    setDoctors([]);
                                    setAvailableSlots([]);
                                    setErrorMessage("");
                                }}
                            >
                                Volver
                            </button>

                            <button
                                type="button"
                                className="btn btn-primary"
                                disabled={!doctorId}
                                onClick={() => {
                                    setCurrentStep(4);
                                    setSelectedDate("");
                                    setSelectedDateTime("");
                                    setAvailableSlots([]);
                                    setErrorMessage("");
                                }}
                            >
                                Continuar
                            </button>
                        </div>
                    </section>
                )}

                {currentStep === 4 && (
                    <section className="wizard-card">
                        <h2>
                            Seleccione la fecha y hora
                        </h2>

                        <p className="wizard-description">
                            Seleccione una fecha para consultar los horarios disponibles
                            del médico.
                        </p>

                        <div className="wizard-date-section">
                            <label htmlFor="appointment-date">
                                Fecha de la cita
                            </label>

                            <input
                                id="appointment-date"
                                type="date"
                                min={today}
                                value={selectedDate}
                                onChange={(event) => {
                                    setSelectedDate(
                                        event.target.value
                                    );

                                    setSelectedDateTime("");
                                    setAvailableSlots([]);
                                    setErrorMessage("");
                                }}
                            />

                            <button
                                type="button"
                                className="btn btn-primary"
                                disabled={
                                    !selectedDate ||
                                    loading
                                }
                                onClick={loadAvailability}
                            >
                                {loading
                                    ? "Consultando..."
                                    : "Consultar horarios"}
                            </button>
                        </div>

                        {availableSlots.length > 0 && (
                            <div className="slots-section">
                                <h3>
                                    Horarios del día
                                </h3>

                                <div className="slots-grid">
                                    {availableSlots.map(slot => (
                                        <button
                                            type="button"
                                            key={slot.dateTime}
                                            disabled={!slot.available}
                                            className={
                                                selectedDateTime === slot.dateTime
                                                    ? "slot-button selected"
                                                    : slot.available
                                                        ? "slot-button"
                                                        : "slot-button occupied"
                                            }
                                            onClick={() => {
                                                if (slot.available) {
                                                    setSelectedDateTime(
                                                        slot.dateTime
                                                    );

                                                    setErrorMessage("");
                                                }
                                            }}
                                        >
                                            <span>
                                                {slot.time}
                                            </span>

                                            <small>
                                                {slot.available
                                                    ? "Disponible"
                                                    : "Ocupado"}
                                            </small>
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}

                        <div className="wizard-actions">
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => {
                                    setCurrentStep(3);
                                    setSelectedDate("");
                                    setSelectedDateTime("");
                                    setAvailableSlots([]);
                                    setErrorMessage("");
                                }}
                            >
                                Volver
                            </button>

                            <button
                                type="button"
                                className="btn btn-primary"
                                disabled={!selectedDateTime}
                                onClick={() => {
                                    setCurrentStep(5);
                                    setErrorMessage("");
                                }}
                            >
                                Continuar
                            </button>
                        </div>
                    </section>
                )}

                {currentStep === 5 && (
                    <section className="wizard-card">
                        <h2>
                            Confirmar Cita
                        </h2>

                        <p className="wizard-description">
                            Revise la información seleccionada e ingrese el motivo
                            de la consulta.
                        </p>

                        {successMessage && (
                            <div className="wizard-success">
                                {successMessage}
                            </div>
                        )}

                        <div className="appointment-summary">
                            <div className="summary-item">
                                <span>Sucursal</span>

                                <strong>
                                    {selectedBranch?.name || "No seleccionada"}
                                </strong>
                            </div>

                            <div className="summary-item">
                                <span>Especialidad</span>

                                <strong>
                                    {selectedSpecialty?.name || "No seleccionada"}
                                </strong>
                            </div>

                            <div className="summary-item">
                                <span>Médico</span>

                                <strong>
                                    {selectedDoctor?.fullName || "No seleccionado"}
                                </strong>
                            </div>

                            <div className="summary-item">
                                <span>Fecha</span>

                                <strong>
                                    {selectedDate || "No seleccionada"}
                                </strong>
                            </div>

                            <div className="summary-item">
                                <span>Hora</span>

                                <strong>
                                    {selectedSlot?.time || "No seleccionada"}
                                </strong>
                            </div>

                            <div className="summary-item">
                                <span>Estado inicial</span>

                                <strong>
                                    Pendiente de pago
                                </strong>
                            </div>
                        </div>

                        <div className="reason-section">
                            <label htmlFor="appointment-reason">
                                Motivo de consulta
                            </label>

                            <textarea
                                id="appointment-reason"
                                rows="6"
                                maxLength="2000"
                                placeholder="Describa el motivo de la consulta..."
                                value={reason}
                                className={
                                    reasonError
                                        ? "input-error"
                                        : ""
                                }
                                onChange={(event) => {
                                    setReason(
                                        event.target.value
                                    );

                                    if (reasonError) {
                                        setReasonError("");
                                    }
                                }}
                            />

                            <div className="reason-counter">
                                {reason.length} / 2000 caracteres
                            </div>

                            {reasonError && (
                                <div className="error-message">
                                    {reasonError}
                                </div>
                            )}
                        </div>

                        <div className="wizard-actions">
                            <button
                                type="button"
                                className="btn btn-secondary"
                                disabled={loading}
                                onClick={() => {
                                    setCurrentStep(4);
                                    setSuccessMessage("");
                                    setErrorMessage("");
                                    setReasonError("");
                                }}
                            >
                                Volver
                            </button>

                            <button
                                type="button"
                                className="btn btn-primary"
                                disabled={
                                    loading ||
                                    Boolean(successMessage)
                                }
                                onClick={
                                    handleConfirmAppointment
                                }
                            >
                                {loading
                                    ? "Registrando cita..."
                                    : "Confirmar Cita"}
                            </button>
                        </div>
                    </section>
                )}
            </div>
        </MainLayout>
    );
}

export default AppointmentWizard;