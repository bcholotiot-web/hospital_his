import { useState } from "react";

import MainLayout
    from "../../layouts/MainLayout";

import PaymentReceipt
    from "./PaymentReceipt";

import {
    registerCashierPayment,
    searchPendingAppointment
} from "../../api/cashierApi";

import "./Cashier.css";

function Cashier() {
    const [searchType, setSearchType] =
        useState("APPOINTMENT_ID");

    const [searchValue, setSearchValue] =
        useState("");

    const [appointment, setAppointment] =
        useState(null);

    const [paymentMethod, setPaymentMethod] =
        useState("EFECTIVO");

    const [receivedAmount, setReceivedAmount] =
        useState("");

    const [cardLastFour, setCardLastFour] =
        useState("");

    const [cardScenario, setCardScenario] =
        useState("approved");

    const [errors, setErrors] =
        useState({});

    const [generalError, setGeneralError] =
        useState("");

    const [successMessage, setSuccessMessage] =
        useState("");

    const [loading, setLoading] =
        useState(false);

    const [processing, setProcessing] =
        useState(false);

    const [paymentReceipt, setPaymentReceipt] =
        useState(null);

    const validateSearch = () => {
        const newErrors = {};
        const cleanValue =
            searchValue.trim();

        if (!cleanValue) {
            newErrors.searchValue =
                searchType === "DPI"
                    ? "Debe ingresar el DPI del paciente."
                    : "Debe ingresar el número de cita.";

        } else if (
            searchType === "APPOINTMENT_ID" &&
            !/^\d+$/.test(cleanValue)
        ) {
            newErrors.searchValue =
                "El número de cita debe contener únicamente números.";

        } else if (
            searchType === "DPI" &&
            !/^\d{13}$/.test(cleanValue)
        ) {
            newErrors.searchValue =
                "El DPI debe contener exactamente 13 dígitos numéricos.";
        }

        setErrors(newErrors);

        return Object.keys(newErrors).length === 0;
    };

    const handleSearch = async (event) => {
        event.preventDefault();

        if (loading) {
            return;
        }

        setAppointment(null);
        setPaymentReceipt(null);
        setGeneralError("");
        setSuccessMessage("");

        if (!validateSearch()) {
            return;
        }

        try {
            setLoading(true);

            const response =
                await searchPendingAppointment(
                    searchType,
                    searchValue.trim()
                );

            setAppointment(response.data);

            resetPaymentFields();

        } catch (error) {
            console.error(
                "Error al buscar cita:",
                error
            );

            setGeneralError(
                error.response?.data?.message ||
                "No se encontraron citas pendientes de pago para el criterio ingresado."
            );

        } finally {
            setLoading(false);
        }
    };

    const validatePayment = () => {
        const newErrors = {};

        if (!paymentMethod) {
            newErrors.paymentMethod =
                "Debe seleccionar un método de pago.";
        }

        if (paymentMethod === "EFECTIVO") {
            if (
                receivedAmount === "" ||
                receivedAmount === null
            ) {
                newErrors.receivedAmount =
                    "Debe ingresar el monto recibido.";

            } else if (
                Number(receivedAmount) <= 0
            ) {
                newErrors.receivedAmount =
                    "El monto recibido debe ser mayor que cero.";

            } else if (
                Number(receivedAmount) <
                Number(appointment.amount)
            ) {
                newErrors.receivedAmount =
                    `El monto recibido (Q${Number(
                        receivedAmount
                    ).toFixed(2)}) es menor al monto a cobrar (Q${Number(
                        appointment.amount
                    ).toFixed(2)}).`;
            }
        }

        if (paymentMethod !== "EFECTIVO") {
            if (!/^\d{4}$/.test(cardLastFour)) {
                newErrors.cardLastFour =
                    "Debe ingresar los últimos 4 dígitos de la tarjeta.";
            }

            if (!cardScenario) {
                newErrors.cardScenario =
                    "Debe seleccionar el resultado de prueba de la tarjeta.";
            }
        }

        setErrors(newErrors);

        return Object.keys(newErrors).length === 0;
    };

    const getPaymentToken = () => {
        const tokens = {
            approved:
                "tok_test_approved",

            declined:
                "tok_test_declined",

            error:
                "tok_test_error",

            communication:
                "tok_test_communication_error"
        };

        return tokens[cardScenario];
    };

    const calculateChange = () => {
        if (
            paymentMethod !== "EFECTIVO" ||
            !receivedAmount ||
            !appointment
        ) {
            return 0;
        }

        return Math.max(
            Number(receivedAmount) -
            Number(appointment.amount),
            0
        );
    };

    const handlePayment = async (event) => {
        event.preventDefault();

        if (
            processing ||
            !appointment
        ) {
            return;
        }

        setGeneralError("");
        setSuccessMessage("");

        if (!validatePayment()) {
            return;
        }

        const request = {
            appointmentId:
                Number(appointment.appointmentId),

            paymentMethod,

            receivedAmount:
                paymentMethod === "EFECTIVO"
                    ? Number(receivedAmount)
                    : null,

            cardLastFour:
                paymentMethod === "EFECTIVO"
                    ? null
                    : cardLastFour,

            paymentToken:
                paymentMethod === "EFECTIVO"
                    ? null
                    : getPaymentToken(),

            idempotencyKey:
                crypto.randomUUID()
        };

        try {
            setProcessing(true);

            const response =
                await registerCashierPayment(
                    request
                );

            if (
                response.data.status ===
                "APROBADO"
            ) {
                setPaymentReceipt(
                    response.data
                );

                setSuccessMessage(
                    response.data.message
                );

                return;
            }

            setGeneralError(
                response.data.message ||
                "El pago no fue aprobado."
            );

        } catch (error) {
            console.error(
                "Error al registrar pago:",
                error
            );

            setGeneralError(
                error.response?.data?.message ||
                "No fue posible registrar el pago."
            );

        } finally {
            setProcessing(false);
        }
    };

    const resetPaymentFields = () => {
        setPaymentMethod("EFECTIVO");
        setReceivedAmount("");
        setCardLastFour("");
        setCardScenario("approved");
        setErrors({});
    };

    const handleNewPayment = () => {
        setSearchType("APPOINTMENT_ID");
        setSearchValue("");
        setAppointment(null);
        setPaymentReceipt(null);
        setGeneralError("");
        setSuccessMessage("");
        resetPaymentFields();
    };

    const maskDpi = (dpi) => {
        if (!dpi) {
            return "No registrado";
        }

        const cleanDpi =
            String(dpi).trim();

        if (cleanDpi.length <= 4) {
            return cleanDpi;
        }

        return `${"*".repeat(
            cleanDpi.length - 4
        )}${cleanDpi.slice(-4)}`;
    };

    const formatDateTime = (value) => {
        if (!value) {
            return "No disponible";
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

    if (paymentReceipt) {
        return (
            <MainLayout>
                <PaymentReceipt
                    payment={paymentReceipt}
                    onNewPayment={
                        handleNewPayment
                    }
                />
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="cashier-page">
                <header className="cashier-header">
                    <h1>
                        Cobro de Consulta en Caja
                    </h1>

                    <p>
                        Busque una cita pendiente y registre
                        el pago del paciente.
                    </p>
                </header>

                <section className="cashier-search-card">
                    <div className="cashier-search-types">
                        <button
                            type="button"
                            className={
                                searchType ===
                                    "APPOINTMENT_ID"
                                    ? "cashier-search-type active"
                                    : "cashier-search-type"
                            }
                            onClick={() => {
                                setSearchType(
                                    "APPOINTMENT_ID"
                                );

                                setSearchValue("");
                                setAppointment(null);
                                setErrors({});
                                setGeneralError("");
                            }}
                        >
                            Por No. Cita
                        </button>

                        <button
                            type="button"
                            className={
                                searchType === "DPI"
                                    ? "cashier-search-type active"
                                    : "cashier-search-type"
                            }
                            onClick={() => {
                                setSearchType("DPI");
                                setSearchValue("");
                                setAppointment(null);
                                setErrors({});
                                setGeneralError("");
                            }}
                        >
                            Por DPI
                        </button>
                    </div>

                    <form
                        className="cashier-search-form"
                        onSubmit={handleSearch}
                    >
                        <div>
                            <input
                                type="text"
                                inputMode="numeric"
                                maxLength={
                                    searchType === "DPI"
                                        ? 13
                                        : 25
                                }
                                placeholder={
                                    searchType === "DPI"
                                        ? "Ingrese DPI de 13 dígitos"
                                        : "Ingrese número de cita"
                                }
                                value={searchValue}
                                className={
                                    errors.searchValue
                                        ? "input-error"
                                        : ""
                                }
                                onChange={(event) => {
                                    const value =
                                        event.target.value
                                            .replace(
                                                /\D/g,
                                                ""
                                            );

                                    setSearchValue(value);

                                    setErrors(previous => ({
                                        ...previous,
                                        searchValue: ""
                                    }));

                                    setGeneralError("");
                                }}
                            />

                            {errors.searchValue && (
                                <div className="error-message">
                                    {errors.searchValue}
                                </div>
                            )}
                        </div>

                        <button
                            type="submit"
                            className="btn btn-primary"
                            disabled={loading}
                        >
                            {loading
                                ? "Buscando..."
                                : "Buscar"}
                        </button>
                    </form>
                </section>

                {generalError && (
                    <div className="cashier-message error">
                        {generalError}
                    </div>
                )}

                {successMessage && (
                    <div className="cashier-message success">
                        {successMessage}
                    </div>
                )}

                {appointment && (
                    <section className="cashier-content">
                        <div className="cashier-appointment-card">
                            <h2>
                                Detalle de la cita
                            </h2>

                            <div className="cashier-details">
                                <p>
                                    <span>No. Cita</span>

                                    <strong>
                                        {appointment.appointmentId}
                                    </strong>
                                </p>

                                <p>
                                    <span>Paciente</span>

                                    <strong>
                                        {appointment.patientName}
                                    </strong>
                                </p>

                                <p>
                                    <span>DPI</span>

                                    <strong>
                                        {maskDpi(
                                            appointment.patientDpi
                                        )}
                                    </strong>
                                </p>

                                <p>
                                    <span>Especialidad</span>

                                    <strong>
                                        {appointment.specialty}
                                    </strong>
                                </p>

                                <p>
                                    <span>Médico</span>

                                    <strong>
                                        {appointment.doctorName}
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

                                <p className="cashier-total">
                                    <span>Total a cobrar</span>

                                    <strong>
                                        Q{Number(
                                            appointment.amount
                                        ).toFixed(2)}
                                    </strong>
                                </p>
                            </div>
                        </div>

                        <div className="cashier-payment-card">
                            <h2>
                                Registrar Pago
                            </h2>

                            <form onSubmit={handlePayment}>
                                <div className="cashier-form-group">
                                    <label>
                                        Método de pago
                                    </label>

                                    <select
                                        value={paymentMethod}
                                        className={
                                            errors.paymentMethod
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={(event) => {
                                            setPaymentMethod(
                                                event.target.value
                                            );

                                            setReceivedAmount("");
                                            setCardLastFour("");
                                            setErrors({});
                                            setGeneralError("");
                                        }}
                                    >
                                        <option value="EFECTIVO">
                                            Efectivo
                                        </option>

                                        <option value="VISA">
                                            Visa
                                        </option>

                                        <option value="MASTERCARD">
                                            Mastercard
                                        </option>

                                        <option value="DEBITO">
                                            Débito
                                        </option>
                                    </select>
                                </div>

                                {paymentMethod ===
                                    "EFECTIVO" ? (
                                    <>
                                        <div className="cashier-form-group">
                                            <label>
                                                Monto recibido
                                            </label>

                                            <input
                                                type="number"
                                                min="0"
                                                step="0.01"
                                                value={receivedAmount}
                                                className={
                                                    errors.receivedAmount
                                                        ? "input-error"
                                                        : ""
                                                }
                                                onChange={(event) => {
                                                    setReceivedAmount(
                                                        event.target.value
                                                    );

                                                    setErrors(
                                                        previous => ({
                                                            ...previous,
                                                            receivedAmount:
                                                                ""
                                                        })
                                                    );
                                                }}
                                            />

                                            {errors.receivedAmount && (
                                                <div className="error-message">
                                                    {
                                                        errors.receivedAmount
                                                    }
                                                </div>
                                            )}
                                        </div>

                                        <div className="cashier-change">
                                            <span>
                                                Cambio a devolver
                                            </span>

                                            <strong>
                                                Q{calculateChange()
                                                    .toFixed(2)}
                                            </strong>
                                        </div>
                                    </>
                                ) : (
                                    <>
                                        <div className="cashier-test-notice">
                                            Entorno simulado. No ingrese
                                            información real de tarjeta.
                                        </div>

                                        <div className="cashier-form-group">
                                            <label>
                                                Últimos 4 dígitos
                                            </label>

                                            <input
                                                type="text"
                                                inputMode="numeric"
                                                maxLength="4"
                                                value={cardLastFour}
                                                className={
                                                    errors.cardLastFour
                                                        ? "input-error"
                                                        : ""
                                                }
                                                onChange={(event) => {
                                                    const value =
                                                        event.target.value
                                                            .replace(
                                                                /\D/g,
                                                                ""
                                                            );

                                                    setCardLastFour(value);

                                                    setErrors(
                                                        previous => ({
                                                            ...previous,
                                                            cardLastFour:
                                                                ""
                                                        })
                                                    );
                                                }}
                                            />

                                            {errors.cardLastFour && (
                                                <div className="error-message">
                                                    {
                                                        errors.cardLastFour
                                                    }
                                                </div>
                                            )}
                                        </div>

                                        <div className="cashier-form-group">
                                            <label>
                                                Resultado de prueba
                                            </label>

                                            <select
                                                value={cardScenario}
                                                onChange={(event) =>
                                                    setCardScenario(
                                                        event.target.value
                                                    )
                                                }
                                            >
                                                <option value="approved">
                                                    Aprobado
                                                </option>

                                                <option value="declined">
                                                    Rechazado
                                                </option>

                                                <option value="error">
                                                    Error de procesamiento
                                                </option>

                                                <option value="communication">
                                                    Error de comunicación
                                                </option>
                                            </select>
                                        </div>
                                    </>
                                )}

                                <button
                                    type="submit"
                                    className="cashier-pay-button"
                                    disabled={processing}
                                >
                                    {processing
                                        ? "Registrando pago..."
                                        : `Registrar Pago Q${Number(
                                            appointment.amount
                                        ).toFixed(2)}`}
                                </button>
                            </form>
                        </div>
                    </section>
                )}
            </div>
        </MainLayout>
    );
}

export default Cashier;