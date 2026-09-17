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

/*
 * RN-CU04:
 * Número de tarjeta entre 13 y 19 dígitos.
 */
const MIN_CARD_DIGITS = 13;
const MAX_CARD_DIGITS = 19;

/*
 * RN-CU04-02:
 * Nombre del titular entre 5 y 100 caracteres.
 */
const MIN_CARDHOLDER_LENGTH = 5;
const MAX_CARDHOLDER_LENGTH = 100;

function PaymentPage() {
    const { appointmentId } = useParams();
    const navigate = useNavigate();

    /*
     * Información de la cita obtenida desde Spring Boot.
     */
    const [
        paymentSummary,
        setPaymentSummary
    ] = useState(null);

    /*
     * Escenario utilizado únicamente por la pasarela
     * simulada del proyecto.
     */
    const [
        paymentScenario,
        setPaymentScenario
    ] = useState("approved");

    /*
     * Datos temporales del formulario.
     *
     * cardNumber, expirationDate y cvv solamente viven
     * en memoria mientras este componente está abierto.
     */
    const [
        cardNumber,
        setCardNumber
    ] = useState("");

    const [
        cardNumberFocused,
        setCardNumberFocused
    ] = useState(false);

    const [
        cardholderName,
        setCardholderName
    ] = useState("");

    const [
        expirationDate,
        setExpirationDate
    ] = useState("");

    const [
        cvv,
        setCvv
    ] = useState("");

    /*
     * UUID que protege contra solicitudes duplicadas.
     */
    const [
        idempotencyKey,
        setIdempotencyKey
    ] = useState(
        () => crypto.randomUUID()
    );

    const [errors, setErrors] =
        useState({});

    const [loading, setLoading] =
        useState(true);

    const [processing, setProcessing] =
        useState(false);

    const [expired, setExpired] =
        useState(false);

    const [
        generalError,
        setGeneralError
    ] = useState("");

    /*
     * Cargar el resumen de la cita al abrir la página.
     */
    useEffect(() => {
        loadPaymentSummary();
    }, [appointmentId]);

    /**
     * Obtiene el resumen que se presenta antes del pago.
     */
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

    /**
     * Procesa la expiración reportada por
     * ReservationTimer.
     */
    const handleExpired = useCallback(() => {
        setExpired(true);

        /*
         * Limpiar inmediatamente los datos sensibles.
         */
        clearSensitivePaymentData();

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

    /**
     * Elimina el error de un campo cuando la persona
     * comienza a corregirlo.
     */
    const clearFieldError = (fieldName) => {
        setErrors(currentErrors => {
            if (!currentErrors[fieldName]) {
                return currentErrors;
            }

            const updatedErrors = {
                ...currentErrors
            };

            delete updatedErrors[fieldName];

            return updatedErrors;
        });
    };

    /**
     * Conserva solamente dígitos.
     */
    const onlyDigits = (value) => {
        return String(value || "")
            .replace(/\D/g, "");
    };

    /**
     * Valida el número mediante el algoritmo de Luhn.
     *
     * Luhn detecta errores comunes de digitación, pero
     * no confirma que una tarjeta exista o tenga fondos.
     */
    const isValidLuhn = (value) => {
        const digits = onlyDigits(value);

        if (
            digits.length < MIN_CARD_DIGITS ||
            digits.length > MAX_CARD_DIGITS
        ) {
            return false;
        }

        /*
         * Evitar números formados por un único dígito
         * repetido, por ejemplo 0000000000000000.
         */
        if (/^(\d)\1+$/.test(digits)) {
            return false;
        }

        let sum = 0;
        let mustDouble = false;

        for (
            let index = digits.length - 1;
            index >= 0;
            index -= 1
        ) {
            let digit =
                Number(digits.charAt(index));

            if (mustDouble) {
                digit *= 2;

                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            mustDouble = !mustDouble;
        }

        return sum % 10 === 0;
    };

    /**
     * Enmascara el número y conserva visibles únicamente
     * los últimos cuatro dígitos.
     *
     * Ejemplo:
     * ••••••••••••1111
     */
    const maskCardNumber = (value) => {
        const digits = onlyDigits(value);

        if (!digits) {
            return "";
        }

        if (digits.length <= 4) {
            return digits;
        }

        return (
            "•".repeat(digits.length - 4) +
            digits.slice(-4)
        );
    };

    /**
     * Obtiene el valor que será visible dentro del campo.
     *
     * Mientras el campo tiene foco se muestra el número
     * para permitir edición.
     *
     * Al perder el foco se enmascara.
     */
    const getDisplayedCardNumber = () => {
        if (cardNumberFocused) {
            return cardNumber;
        }

        return maskCardNumber(cardNumber);
    };

    /**
     * Controla el número de tarjeta.
     */
    const handleCardNumberChange = (event) => {
        const numericValue =
            onlyDigits(event.target.value)
                .slice(0, MAX_CARD_DIGITS);

        setCardNumber(numericValue);
        clearFieldError("cardNumber");
        setGeneralError("");
    };

    /**
     * Convierte automáticamente el titular a mayúsculas.
     */
    const handleCardholderNameChange = (event) => {
        const value =
            event.target.value
                .toUpperCase()
                .slice(
                    0,
                    MAX_CARDHOLDER_LENGTH
                );

        setCardholderName(value);
        clearFieldError("cardholderName");
        setGeneralError("");
    };

    /**
     * Autoformatea la fecha como MM/AA.
     *
     * Ejemplos:
     * 1    -> 1
     * 12   -> 12
     * 122  -> 12/2
     * 1229 -> 12/29
     */
    const handleExpirationDateChange = (event) => {
        const digits =
            onlyDigits(event.target.value)
                .slice(0, 4);

        let formattedValue = digits;

        if (digits.length > 2) {
            formattedValue =
                `${digits.slice(0, 2)}/` +
                digits.slice(2);
        }

        setExpirationDate(formattedValue);
        clearFieldError("expirationDate");
        setGeneralError("");
    };

    /**
     * Verifica que MM/AA tenga un mes válido y que
     * la tarjeta no esté vencida.
     *
     * Se considera válida durante todo el mes indicado.
     */
    const isValidExpirationDate = (value) => {
        const format =
            /^(0[1-9]|1[0-2])\/\d{2}$/;

        if (!format.test(value)) {
            return false;
        }

        const [monthText, yearText] =
            value.split("/");

        const expirationMonth =
            Number(monthText);

        const expirationYear =
            2000 + Number(yearText);

        const currentDate =
            new Date();

        const currentMonth =
            currentDate.getMonth() + 1;

        const currentYear =
            currentDate.getFullYear();

        if (expirationYear < currentYear) {
            return false;
        }

        if (
            expirationYear === currentYear &&
            expirationMonth < currentMonth
        ) {
            return false;
        }

        return true;
    };

    /**
     * Conserva únicamente 3 o 4 dígitos del CVV.
     */
    const handleCvvChange = (event) => {
        const numericValue =
            onlyDigits(event.target.value)
                .slice(0, 4);

        setCvv(numericValue);
        clearFieldError("cvv");
        setGeneralError("");
    };

    /**
     * Valida el formulario antes de simular
     * la tokenización.
     */
    const validateForm = () => {
        const newErrors = {};

        const cleanCardNumber =
            onlyDigits(cardNumber);

        const cleanCardholderName =
            cardholderName.trim();

        if (
            cleanCardNumber.length <
            MIN_CARD_DIGITS ||
            cleanCardNumber.length >
            MAX_CARD_DIGITS ||
            !isValidLuhn(cleanCardNumber)
        ) {
            newErrors.cardNumber =
                "El número de tarjeta no es válido. " +
                "Debe contener entre 13 y 19 dígitos " +
                "y cumplir la validación Luhn.";
        }

        if (!cleanCardholderName) {
            newErrors.cardholderName =
                "El nombre del titular es obligatorio.";
        } else if (
            cleanCardholderName.length <
            MIN_CARDHOLDER_LENGTH ||
            cleanCardholderName.length >
            MAX_CARDHOLDER_LENGTH
        ) {
            newErrors.cardholderName =
                "El nombre del titular debe contener " +
                "entre 5 y 100 caracteres.";
        }

        if (
            !isValidExpirationDate(
                expirationDate
            )
        ) {
            newErrors.expirationDate =
                "La fecha de vencimiento debe tener " +
                "formato MM/AA y corresponder a una " +
                "fecha vigente.";
        }

        if (!/^\d{3,4}$/.test(cvv)) {
            newErrors.cvv =
                "El código de seguridad CVV debe " +
                "contener 3 o 4 dígitos.";
        }

        if (!paymentScenario) {
            newErrors.paymentScenario =
                "Debe seleccionar un escenario de prueba.";
        }

        setErrors(newErrors);

        return (
            Object.keys(newErrors).length === 0
        );
    };

    /**
     * Devuelve el token reconocido por
     * SimulatedPaymentGateway.
     *
     * En producción, esta función debe sustituirse por
     * el SDK de la pasarela real.
     */
    const tokenizeCardWithSimulatedGateway = async () => {
        /*
         * Esta validación simula que la pasarela
         * recibió temporalmente los datos.
         *
         * No se imprimen ni se almacenan.
         */
        if (
            !cardNumber ||
            !cardholderName ||
            !expirationDate ||
            !cvv
        ) {
            throw new Error(
                "No fue posible generar el token de pago."
            );
        }

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

        const token =
            tokens[paymentScenario];

        if (!token) {
            throw new Error(
                "El escenario de pago no es válido."
            );
        }

        return token;
    };

    /**
     * Limpia los datos sensibles del componente.
     */
    const clearSensitivePaymentData = () => {
        setCardNumber("");
        setExpirationDate("");
        setCvv("");
        setCardNumberFocused(false);
    };

    /**
     * Procesa el pago.
     */
    const handlePayment = async (event) => {
        event.preventDefault();

        if (processing || expired) {
            return;
        }

        setGeneralError("");

        if (!validateForm()) {
            return;
        }

        try {
            setProcessing(true);

            /*
             * En el entorno simulado se genera un token
             * local reconocido por el backend.
             *
             * En producción, este paso será ejecutado
             * por el SDK seguro de la pasarela.
             */
            const paymentToken =
                await tokenizeCardWithSimulatedGateway();

            /*
             * Solo se envían datos no sensibles a Spring Boot.
             */
            const paymentRequest = {
                appointmentId:
                    Number(appointmentId),

                idempotencyKey,

                paymentToken,

                cardholderName:
                    cardholderName
                        .trim()
                        .toUpperCase(),

                cardLastFour:
                    onlyDigits(cardNumber)
                        .slice(-4)
            };

            /*
             * No imprimir paymentRequest durante una
             * integración real, porque contiene un token.
             */
            const response =
                await processPayment(
                    paymentRequest
                );

            /*
             * El CVV debe eliminarse de memoria después
             * de cada intento, incluso si fue rechazado.
             */
            setCvv("");

            if (
                response.data.status ===
                "APROBADO"
            ) {
                /*
                 * El comprobante contiene únicamente
                 * información no sensible.
                 */
                sessionStorage.setItem(
                    "paymentConfirmation",
                    JSON.stringify(
                        response.data
                    )
                );

                sessionStorage.removeItem(
                    "pendingAppointment"
                );

                clearSensitivePaymentData();

                navigate(
                    "/payments/success",
                    {
                        replace: true
                    }
                );

                return;
            }

            /*
             * Si el pago no fue aprobado, un reintento
             * debe utilizar una nueva llave.
             */
            setIdempotencyKey(
                crypto.randomUUID()
            );

            setGeneralError(
                response.data.message ||
                "El pago no fue aprobado."
            );

        } catch (error) {
            /*
             * El CVV se limpia después del intento.
             */
            setCvv("");

            setGeneralError(
                error.response?.data?.message ||
                error.message ||
                "No fue posible procesar el pago."
            );

            /*
             * Un nuevo intento requiere una UUID distinta.
             */
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
                        <h1>
                            Pago de Consulta
                        </h1>

                        <p>
                            Complete el pago antes de que
                            termine el tiempo de reserva.
                        </p>
                    </div>

                    {paymentSummary
                        ?.reservationExpiresAt && (
                            <ReservationTimer
                                expirationDate={
                                    paymentSummary
                                        .reservationExpiresAt
                                }
                                onExpired={
                                    handleExpired
                                }
                            />
                        )}
                </div>

                {generalError && (
                    <div
                        className="payment-error"
                        role="alert"
                    >
                        {generalError}
                    </div>
                )}

                <div className="payment-layout">
                    {/* Resumen */}
                    <section className="payment-card">
                        <h2>
                            Resumen de la cita
                        </h2>

                        <div className="payment-summary">
                            <div>
                                <span>Médico</span>
                                <strong>
                                    {
                                        paymentSummary
                                            ?.doctorName
                                    }
                                </strong>
                            </div>

                            <div>
                                <span>
                                    Especialidad
                                </span>
                                <strong>
                                    {
                                        paymentSummary
                                            ?.specialty
                                    }
                                </strong>
                            </div>

                            <div>
                                <span>Sucursal</span>
                                <strong>
                                    {
                                        paymentSummary
                                            ?.branch
                                    }
                                </strong>
                            </div>

                            <div>
                                <span>
                                    Fecha y hora
                                </span>

                                <strong>
                                    {paymentSummary
                                        ?.appointmentDateTime
                                        ? new Date(
                                            paymentSummary
                                                .appointmentDateTime
                                        ).toLocaleString(
                                            "es-GT",
                                            {
                                                year:
                                                    "numeric",
                                                month:
                                                    "long",
                                                day:
                                                    "numeric",
                                                hour:
                                                    "2-digit",
                                                minute:
                                                    "2-digit"
                                            }
                                        )
                                        : "No disponible"}
                                </strong>
                            </div>

                            <div>
                                <span>Total</span>
                                <strong>
                                    Q
                                    {Number(
                                        paymentSummary
                                            ?.amount || 0
                                    ).toFixed(2)}
                                </strong>
                            </div>
                        </div>
                    </section>

                    {/* Formulario */}
                    <section className="payment-card">
                        <h2>
                            Datos de pago
                        </h2>

                        <p className="payment-security-note">
                            Entorno simulado. No ingrese
                            datos de tarjetas reales.
                        </p>

                        <form onSubmit={handlePayment}>
                            {/* Escenario de prueba */}
                            <div className="payment-form-group">
                                <label htmlFor="payment-scenario">
                                    Escenario de prueba
                                </label>

                                <select
                                    id="payment-scenario"
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

                                        clearFieldError(
                                            "paymentScenario"
                                        );

                                        setGeneralError("");
                                    }}
                                    disabled={
                                        processing || expired
                                    }
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
                                        {
                                            errors
                                                .paymentScenario
                                        }
                                    </div>
                                )}
                            </div>

                            {/* Número de tarjeta */}
                            <div className="payment-form-group">
                                <label htmlFor="card-number">
                                    Número de tarjeta
                                </label>

                                <input
                                    id="card-number"
                                    type="text"
                                    inputMode="numeric"
                                    autoComplete="cc-number"
                                    value={
                                        getDisplayedCardNumber()
                                    }
                                    placeholder="13 a 19 dígitos"
                                    className={
                                        errors.cardNumber
                                            ? "input-error"
                                            : ""
                                    }
                                    onFocus={() => {
                                        setCardNumberFocused(
                                            true
                                        );
                                    }}
                                    onBlur={() => {
                                        setCardNumberFocused(
                                            false
                                        );
                                    }}
                                    onChange={
                                        handleCardNumberChange
                                    }
                                    disabled={
                                        processing || expired
                                    }
                                    aria-describedby={
                                        errors.cardNumber
                                            ? "card-number-error"
                                            : undefined
                                    }
                                />

                                {!cardNumberFocused &&
                                    cardNumber.length >= 4 && (
                                        <small className="masked-card-help">
                                            Tarjeta terminada en{" "}
                                            <strong>
                                                {
                                                    cardNumber
                                                        .slice(-4)
                                                }
                                            </strong>
                                        </small>
                                    )}

                                {errors.cardNumber && (
                                    <div
                                        id="card-number-error"
                                        className="error-message"
                                    >
                                        {
                                            errors.cardNumber
                                        }
                                    </div>
                                )}
                            </div>

                            {/* Titular */}
                            <div className="payment-form-group">
                                <label htmlFor="cardholder-name">
                                    Nombre del titular
                                </label>

                                <input
                                    id="cardholder-name"
                                    type="text"
                                    autoComplete="cc-name"
                                    maxLength={
                                        MAX_CARDHOLDER_LENGTH
                                    }
                                    value={
                                        cardholderName
                                    }
                                    placeholder="NOMBRE COMO APARECE EN LA TARJETA"
                                    className={
                                        errors.cardholderName
                                            ? "input-error"
                                            : ""
                                    }
                                    onChange={
                                        handleCardholderNameChange
                                    }
                                    disabled={
                                        processing || expired
                                    }
                                />

                                <div className="payment-field-counter">
                                    {
                                        cardholderName.length
                                    }
                                    /100 caracteres
                                </div>

                                {errors.cardholderName && (
                                    <div className="error-message">
                                        {
                                            errors
                                                .cardholderName
                                        }
                                    </div>
                                )}
                            </div>

                            <div className="payment-inline-fields">
                                {/* Vencimiento */}
                                <div className="payment-form-group">
                                    <label htmlFor="expiration-date">
                                        Vencimiento
                                    </label>

                                    <input
                                        id="expiration-date"
                                        type="text"
                                        inputMode="numeric"
                                        autoComplete="cc-exp"
                                        maxLength={5}
                                        value={
                                            expirationDate
                                        }
                                        placeholder="MM/AA"
                                        className={
                                            errors.expirationDate
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            handleExpirationDateChange
                                        }
                                        disabled={
                                            processing ||
                                            expired
                                        }
                                    />

                                    {errors.expirationDate && (
                                        <div className="error-message">
                                            {
                                                errors
                                                    .expirationDate
                                            }
                                        </div>
                                    )}
                                </div>

                                {/* CVV */}
                                <div className="payment-form-group">
                                    <label htmlFor="card-cvv">
                                        CVV
                                    </label>

                                    <input
                                        id="card-cvv"
                                        type="password"
                                        inputMode="numeric"
                                        autoComplete="cc-csc"
                                        maxLength={4}
                                        value={cvv}
                                        placeholder="3 o 4 dígitos"
                                        className={
                                            errors.cvv
                                                ? "input-error"
                                                : ""
                                        }
                                        onChange={
                                            handleCvvChange
                                        }
                                        disabled={
                                            processing ||
                                            expired
                                        }
                                    />

                                    {errors.cvv && (
                                        <div className="error-message">
                                            {errors.cvv}
                                        </div>
                                    )}
                                </div>
                            </div>

                            <div className="sensitive-data-notice">
                                <strong>
                                    Información protegida
                                </strong>

                                <span>
                                    El número completo, el
                                    vencimiento y el CVV no se
                                    almacenan ni se envían al
                                    servidor de Hospital HIS.
                                </span>
                            </div>

                            <button
                                type="submit"
                                className="payment-button"
                                disabled={
                                    processing || expired
                                }
                            >
                                {processing
                                    ? "Procesando pago..."
                                    : `Pagar Q${Number(
                                        paymentSummary
                                            ?.amount || 0
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