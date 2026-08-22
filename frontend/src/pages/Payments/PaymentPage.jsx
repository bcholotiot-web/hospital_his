import {
    useCallback,
    useEffect,
    useState
} from "react";

import {
    useNavigate,
    useParams
} from "react-router-dom";

import MainLayout
    from "../../layouts/MainLayout";

import ReservationTimer
    from "../../components/ReservationTimer";

import {
    getPaymentSummary,
    processPayment
} from "../../api/paymentApi";

import "./PaymentPage.css";

function PaymentPage() {
    const { appointmentId } = useParams();

    const navigate = useNavigate();

    const [paymentSummary, setPaymentSummary] =
        useState(null);

    const [paymentScenario, setPaymentScenario] =
        useState("approved");

    const [cardholderName, setCardholderName] =
        useState("");

    const [cardLastFour, setCardLastFour] =
        useState("4242");

    const [errors, setErrors] =
        useState({});

    const [loading, setLoading] =
        useState(true);

    const [processing, setProcessing] =
        useState(false);

    const [expired, setExpired] =
        useState(false);

    const [generalError, setGeneralError] =
        useState("");

    useEffect(() => {
        loadPaymentReservation();
    }, [appointmentId]);

    const loadPaymentSummary = async () => {
        try {
            setLoading(true);
            setGeneralError("");

            const response =
                await getPaymentSummary(
                    appointmentId
                );

            setPaymentSummary(
                response.data
            );

        } catch (error) {
            const message =
                error.response?.data?.message ||
                "No fue posible cargar la información del pago.";

            setGeneralError(message);

            const normalizedMessage =
                message.toLowerCase();

            if (
                normalizedMessage.includes("expirado") ||
                normalizedMessage.includes("expirada")
            ) {
                setExpired(true);

                setTimeout(() => {
                    navigate(
                        "/appointments/new",
                        {
                            replace: true
                        }
                    );
                }, 4000);
            }

        } finally {
            setLoading(false);
        }
    };

    const handleExpired = useCallback(() => {
        setExpired(true);

        setGeneralError(
            "El tiempo para confirmar su cita ha expirado. " +
            "El horario seleccionado ha sido liberado. " +
            "Por favor, seleccione un nuevo horario. " +
            "Será redirigido en unos segundos..."
        );

        setTimeout(() => {
            sessionStorage.removeItem(
                "paymentConfirmation"
            );

            sessionStorage.removeItem(
                "pendingAppointment"
            );

            navigate(
                "/appointments/new",
                {
                    replace: true
                }
            );
        }, 4000);
    }, [navigate]);

    const validateForm = () => {
        const newErrors = {};

        const cleanName =
            cardholderName.trim();

        if (!cleanName) {
            newErrors.cardholderName =
                "El nombre del titular es obligatorio.";
        } else if (
            cleanName.length < 5 ||
            cleanName.length > 100
        ) {
            newErrors.cardholderName =
                "El nombre del titular debe contener entre 5 y 100 caracteres.";
        }

        if (!/^\d{4}$/.test(cardLastFour)) {
            newErrors.cardLastFour =
                "Debe ingresar exactamente los últimos cuatro dígitos.";
        }

        if (!paymentScenario) {
            newErrors.paymentScenario =
                "Debe seleccionar un escenario de prueba.";
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

        return tokens[paymentScenario];
    };

    const handlePayment = async (event) => {
        event.preventDefault();

        if (
            processing ||
            expired
        ) {
            return;
        }

        setGeneralError("");

        if (!validateForm()) {
            return;
        }

        const [idempotencyKey, setIdempotencyKey] =
            useState(() => crypto.randomUUID());

        try {
            setProcessing(true);

            const response =
                await processPayment({
                    appointmentId:
                        Number(appointmentId),

                    idempotencyKey,

                    paymentToken:
                        getPaymentToken(),

                    cardholderName:
                        cardholderName
                            .trim()
                            .toUpperCase(),

                    cardLastFour:
                        cardLastFour.trim()
                });

            if (
                response.data.status
                === "APROBADO"
            ) {
                sessionStorage.setItem(
                    "paymentConfirmation",
                    JSON.stringify(
                        response.data
                    )
                );

                sessionStorage.removeItem(
                    "pendingAppointment"
                );

                navigate(
                    "/payments/success",
                    {
                        replace: true
                    }
                );

                return;
            }

            setGeneralError(
                response.data.message ||
                "El pago no fue aprobado."
            );

            /*
             * Un reintento deberá usar un UUID nuevo.
             * handlePayment genera uno en cada envío.
             */

        } catch (error) {
            setGeneralError(
                error.response?.data?.message ||
                "No fue posible procesar el pago."
            );

            setIdempotencyKey(
                crypto.randomUUID()
            );

        } finally {
            setProcessing(false);
        }
    };

    if (loading) {
        return (
            <MainLayout>
                <p>
                    Cargando información del pago...
                </p>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="payment-page">
                <div className="payment-header">
                    <div>
                        <h1>Pago de Consulta</h1>

                        <p>
                            Complete el pago antes de que
                            termine el tiempo de reserva.
                        </p>
                    </div>

                    {paymentSummary?.reservationExpiresAt && (
                        <ReservationTimer
                            expirationDate={
                                paymentSummary.reservationExpiresAt
                            }
                            onExpired={handleExpired}
                        />
                    )}
                </div>

                {generalError && (
                    <div className="payment-error">
                        {generalError}
                    </div>
                )}

                <div className="payment-layout">
                    <section className="payment-card">
                        <h2>Resumen de la cita</h2>

                        <div className="payment-summary">
                            <div>
                                <span>Médico</span>

                                <strong>
                                    {paymentSummary?.doctorName}
                                </strong>
                            </div>

                            <div>
                                <span>Especialidad</span>

                                <strong>
                                    {paymentSummary?.specialty}
                                </strong>
                            </div>

                            <div>
                                <span>Sucursal</span>

                                <strong>
                                    {paymentSummary?.branch}
                                </strong>
                            </div>

                            <div>
                                <span>Fecha y hora</span>

                                <strong>
                                    {paymentSummary?.appointmentDateTime
                                        ? new Date(
                                            paymentSummary.appointmentDateTime
                                        ).toLocaleString(
                                            "es-GT",
                                            {
                                                year: "numeric",
                                                month: "long",
                                                day: "numeric",
                                                hour: "2-digit",
                                                minute: "2-digit"
                                            }
                                        )
                                        : "No disponible"}
                                </strong>
                            </div>

                            <div>
                                <span>Total</span>

                                <strong>
                                    Q{Number(
                                        paymentSummary?.amount || 0
                                    ).toFixed(2)}
                                </strong>
                            </div>
                        </div>
                    </section>

                    <section className="payment-card">
                        <h2>Datos de pago de prueba</h2>

                        <p className="payment-security-note">
                            Entorno simulado. No ingrese
                            datos de tarjetas reales.
                        </p>

                        <form onSubmit={handlePayment}>
                            <div className="payment-form-group">
                                <label>
                                    Escenario de prueba
                                </label>

                                <select
                                    value={
                                        paymentScenario
                                    }
                                    className={
                                        errors.paymentScenario
                                            ? "input-error"
                                            : ""
                                    }
                                    onChange={(event) => {
                                        setPaymentScenario(
                                            event.target.value
                                        );

                                        setGeneralError("");
                                    }}
                                >
                                    <option value="approved">
                                        Pago aprobado
                                    </option>

                                    <option value="declined">
                                        Rechazo bancario
                                    </option>

                                    <option value="error">
                                        Error de procesamiento
                                    </option>

                                    <option value="communication">
                                        Error de comunicación
                                    </option>
                                </select>

                                {errors.paymentScenario && (
                                    <div className="error-message">
                                        {errors.paymentScenario}
                                    </div>
                                )}
                            </div>

                            <div className="payment-form-group">
                                <label>
                                    Nombre del titular
                                </label>

                                <input
                                    type="text"
                                    maxLength="100"
                                    value={cardholderName}
                                    className={
                                        errors.cardholderName
                                            ? "input-error"
                                            : ""
                                    }
                                    onChange={(event) => {
                                        setCardholderName(
                                            event.target.value
                                        );

                                        if (
                                            errors.cardholderName
                                        ) {
                                            setErrors({
                                                ...errors,
                                                cardholderName: ""
                                            });
                                        }
                                    }}
                                />

                                {errors.cardholderName && (
                                    <div className="error-message">
                                        {errors.cardholderName}
                                    </div>
                                )}
                            </div>

                            <div className="payment-form-group">
                                <label>
                                    Últimos cuatro dígitos
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

                                        setCardLastFour(
                                            value
                                        );
                                    }}
                                />

                                {errors.cardLastFour && (
                                    <div className="error-message">
                                        {errors.cardLastFour}
                                    </div>
                                )}
                            </div>

                            <button
                                type="submit"
                                className="payment-button"
                                disabled={
                                    processing ||
                                    expired
                                }
                            >
                                {processing
                                    ? "Procesando pago..."
                                    : `Pagar Q${Number(
                                        paymentSummary?.amount || 0
                                    ).toFixed(2)}`}
                            </button>
                        </form>
                    </section>
                </div>
            </div>
        </MainLayout>
    );
}

export default PaymentPage;