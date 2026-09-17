import { useState } from "react";
import { useNavigate } from "react-router-dom";

import MainLayout
    from "../../layouts/MainLayout";

import LaboratoryPaymentReceipt
    from "./LaboratoryPaymentReceipt";

import {
    getLaboratoryOrderPaymentDetail,
    registerLaboratoryPayment,
    searchPendingLaboratoryOrders
} from "../../api/laboratoryPaymentApi";

import "./LaboratoryPaymentPage.css";

function LaboratoryPaymentPage() {
    const navigate = useNavigate();

    /*
     * Criterio utilizado en la búsqueda:
     * DPI u ORDER_NUMBER.
     */
    const [searchCriteria, setSearchCriteria] =
        useState("ORDER_NUMBER");

    const [searchValue, setSearchValue] =
        useState("");

    /*
     * Lista de órdenes encontradas.
     */
    const [orders, setOrders] =
        useState([]);

    /*
     * Orden seleccionada con su detalle de exámenes.
     */
    const [selectedOrder, setSelectedOrder] =
        useState(null);

    const [paymentMethod, setPaymentMethod] =
        useState("EFECTIVO");

    const [receivedAmount, setReceivedAmount] =
        useState("");

    const [cardLastFour, setCardLastFour] =
        useState("");

    /*
     * Escenario utilizado por la pasarela simulada.
     */
    const [cardScenario, setCardScenario] =
        useState("approved");

    /*
     * La llave se conserva durante un intento.
     *
     * Si el intento es rechazado o produce error,
     * se genera una nueva para el siguiente intento.
     */
    const [idempotencyKey, setIdempotencyKey] =
        useState(() => crypto.randomUUID());

    const [errors, setErrors] =
        useState({});

    const [generalError, setGeneralError] =
        useState("");

    const [loading, setLoading] =
        useState(false);

    const [loadingDetail, setLoadingDetail] =
        useState(false);

    const [processing, setProcessing] =
        useState(false);

    const [paymentReceipt, setPaymentReceipt] =
        useState(null);

    /**
     * Valida el criterio de búsqueda antes de
     * consultar Spring Boot.
     */
    const validateSearch = () => {
        const newErrors = {};
        const cleanValue =
            searchValue.trim();

        if (!cleanValue) {
            newErrors.searchValue =
                searchCriteria === "DPI"
                    ? "Debe ingresar el DPI del paciente."
                    : "Debe ingresar el número de orden.";
        } else if (
            searchCriteria === "DPI" &&
            !/^\d{13}$/.test(cleanValue)
        ) {
            newErrors.searchValue =
                "El DPI debe contener exactamente " +
                "13 dígitos numéricos.";
        } else if (
            searchCriteria === "ORDER_NUMBER" &&
            cleanValue.length > 100
        ) {
            newErrors.searchValue =
                "El número de orden no puede exceder " +
                "los 100 caracteres.";
        }

        setErrors(newErrors);

        return (
            Object.keys(newErrors).length === 0
        );
    };

    /**
     * Busca órdenes pendientes.
     *
     * El backend solo devuelve:
     * - Órdenes activas.
     * - PENDIENTE_DE_PAGO.
     * - No externas.
     * - De la sucursal del cajero.
     */
    const handleSearch = async (event) => {
        event.preventDefault();

        if (loading || processing) {
            return;
        }

        setOrders([]);
        setSelectedOrder(null);
        setPaymentReceipt(null);
        setGeneralError("");

        if (!validateSearch()) {
            return;
        }

        try {
            setLoading(true);

            const response =
                await searchPendingLaboratoryOrders(
                    searchCriteria,
                    searchValue.trim()
                );

            const foundOrders =
                Array.isArray(response.data)
                    ? response.data
                    : [];

            setOrders(foundOrders);

            if (foundOrders.length === 0) {
                setGeneralError(
                    "No se encontraron órdenes de laboratorio " +
                    "pendientes de pago para el criterio ingresado."
                );
            }

        } catch (error) {
            console.error(
                "Error buscando órdenes:",
                error
            );

            setOrders([]);

            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible buscar las órdenes " +
                    "de laboratorio."
                )
            );
        } finally {
            setLoading(false);
        }
    };

    /**
     * Recupera el detalle de la orden seleccionada.
     *
     * En este punto Spring Boot vuelve a verificar:
     * - Estado.
     * - Sucursal.
     * - Usuario cajero.
     */
    const handleSelectOrder = async (
        orderId
    ) => {
        if (loadingDetail || processing) {
            return;
        }

        setGeneralError("");
        setSelectedOrder(null);

        try {
            setLoadingDetail(true);

            const response =
                await getLaboratoryOrderPaymentDetail(
                    orderId
                );

            setSelectedOrder(response.data);
            resetPaymentFields();

        } catch (error) {
            console.error(
                "Error cargando orden:",
                error
            );

            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible cargar el detalle " +
                    "de la orden seleccionada."
                )
            );
        } finally {
            setLoadingDetail(false);
        }
    };

    /**
     * Valida la información del pago.
     *
     * Estas validaciones mejoran la experiencia,
     * pero Spring Boot vuelve a validarlas.
     */
    const validatePayment = () => {
        const newErrors = {};

        if (!selectedOrder) {
            newErrors.order =
                "Debe seleccionar una orden.";
        }

        if (!paymentMethod) {
            newErrors.paymentMethod =
                "Debe seleccionar un método de pago.";
        }

        if (paymentMethod === "EFECTIVO") {
            const received =
                Number(receivedAmount);

            const total =
                Number(
                    selectedOrder?.totalAmount || 0
                );

            if (
                receivedAmount === "" ||
                receivedAmount === null
            ) {
                newErrors.receivedAmount =
                    "Debe ingresar el monto recibido.";
            } else if (
                !Number.isFinite(received) ||
                received <= 0
            ) {
                newErrors.receivedAmount =
                    "El monto recibido debe ser " +
                    "mayor que cero.";
            } else if (received < total) {
                newErrors.receivedAmount =
                    `El monto recibido (${formatAmount(
                        received
                    )}) es menor al monto a cobrar (${formatAmount(
                        total
                    )}).`;
            }
        } else {
            if (
                !/^\d{4}$/.test(
                    cardLastFour
                )
            ) {
                newErrors.cardLastFour =
                    "Debe ingresar exactamente los " +
                    "últimos 4 dígitos de la tarjeta.";
            }

            if (!cardScenario) {
                newErrors.cardScenario =
                    "Debe seleccionar un escenario " +
                    "de prueba.";
            }
        }

        setErrors(newErrors);

        return (
            Object.keys(newErrors).length === 0
        );
    };

    /**
     * Devuelve el token reconocido por la
     * pasarela simulada del backend.
     */
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

    /**
     * Calcula el cambio de manera informativa.
     *
     * La cifra definitiva es calculada nuevamente
     * por Spring Boot.
     */
    const calculateChange = () => {
        if (
            paymentMethod !== "EFECTIVO" ||
            !receivedAmount ||
            !selectedOrder
        ) {
            return 0;
        }

        return Math.max(
            Number(receivedAmount) -
            Number(selectedOrder.totalAmount),
            0
        );
    };

    /**
     * Registra el cobro.
     *
     * Nunca envía el usuario del cajero.
     * Spring Boot lo obtiene desde el JWT.
     */
    const handlePayment = async (event) => {
        event.preventDefault();

        if (
            processing ||
            !selectedOrder
        ) {
            return;
        }

        setGeneralError("");

        if (!validatePayment()) {
            return;
        }

        const request = {
            laboratoryOrderId:
                Number(
                    selectedOrder
                        .laboratoryOrderId
                ),

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

            idempotencyKey
        };

        try {
            setProcessing(true);

            const response =
                await registerLaboratoryPayment(
                    request
                );

            const payment =
                response.data;

            if (
                payment.paymentStatus ===
                "APROBADO"
            ) {
                setPaymentReceipt(payment);
                setGeneralError("");

                return;
            }

            /*
             * Ante rechazo o error, la orden sigue
             * PENDIENTE_DE_PAGO y puede reintentarse.
             *
             * El nuevo intento necesita otra UUID.
             */
            setIdempotencyKey(
                crypto.randomUUID()
            );

            setCardLastFour("");

            setGeneralError(
                payment.message ||
                "El pago no fue aprobado."
            );

        } catch (error) {
            console.error(
                "Error registrando pago:",
                error
            );

            setIdempotencyKey(
                crypto.randomUUID()
            );

            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible registrar " +
                    "el pago de laboratorio."
                )
            );
        } finally {
            setProcessing(false);
        }
    };

    /**
     * Limpia únicamente los datos del formulario
     * de pago.
     */
    const resetPaymentFields = () => {
        setPaymentMethod("EFECTIVO");
        setReceivedAmount("");
        setCardLastFour("");
        setCardScenario("approved");
        setIdempotencyKey(
            crypto.randomUUID()
        );
        setErrors({});
        setGeneralError("");
    };

    /**
     * Restablece toda la pantalla para iniciar
     * un cobro nuevo.
     */
    const handleNewPayment = () => {
        setSearchCriteria("ORDER_NUMBER");
        setSearchValue("");
        setOrders([]);
        setSelectedOrder(null);
        setPaymentReceipt(null);
        setGeneralError("");

        resetPaymentFields();
    };

    if (paymentReceipt) {
        return (
            <MainLayout>
                <LaboratoryPaymentReceipt
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
            <div className="lab-payment-page">
                <header className="lab-payment-header">
                    <div>
                        <h1>
                            Cobro de Laboratorio
                        </h1>

                        <p>
                            Busque una orden pendiente y
                            registre el pago del paciente.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={() =>
                            navigate("/cashier")
                        }
                    >
                        Volver a Caja
                    </button>
                </header>

                <section className="lab-payment-search-card">
                    <h2>
                        Buscar orden pendiente
                    </h2>

                    <div className="lab-payment-search-types">
                        <button
                            type="button"
                            className={
                                searchCriteria ===
                                    "ORDER_NUMBER"
                                    ? "lab-search-type active"
                                    : "lab-search-type"
                            }
                            onClick={() => {
                                setSearchCriteria(
                                    "ORDER_NUMBER"
                                );
                                setSearchValue("");
                                setOrders([]);
                                setSelectedOrder(null);
                                setErrors({});
                                setGeneralError("");
                            }}
                            disabled={
                                loading || processing
                            }
                        >
                            Por No. Orden
                        </button>

                        <button
                            type="button"
                            className={
                                searchCriteria === "DPI"
                                    ? "lab-search-type active"
                                    : "lab-search-type"
                            }
                            onClick={() => {
                                setSearchCriteria("DPI");
                                setSearchValue("");
                                setOrders([]);
                                setSelectedOrder(null);
                                setErrors({});
                                setGeneralError("");
                            }}
                            disabled={
                                loading || processing
                            }
                        >
                            Por DPI
                        </button>
                    </div>

                    <form
                        className="lab-payment-search-form"
                        onSubmit={handleSearch}
                    >
                        <div>
                            <input
                                type="text"
                                inputMode={
                                    searchCriteria === "DPI"
                                        ? "numeric"
                                        : "text"
                                }
                                maxLength={
                                    searchCriteria === "DPI"
                                        ? 13
                                        : 100
                                }
                                placeholder={
                                    searchCriteria === "DPI"
                                        ? "Ingrese DPI de 13 dígitos"
                                        : "Ejemplo: LAB-4CA13D8FDC3E"
                                }
                                value={searchValue}
                                className={
                                    errors.searchValue
                                        ? "input-error"
                                        : ""
                                }
                                onChange={(event) => {
                                    let value =
                                        event.target.value;

                                    if (
                                        searchCriteria ===
                                        "DPI"
                                    ) {
                                        value = value
                                            .replace(/\D/g, "")
                                            .slice(0, 13);
                                    } else {
                                        value = value
                                            .toUpperCase();
                                    }

                                    setSearchValue(value);

                                    setErrors(
                                        previous => ({
                                            ...previous,
                                            searchValue: ""
                                        })
                                    );

                                    setGeneralError("");
                                }}
                                disabled={
                                    loading || processing
                                }
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
                            disabled={
                                loading || processing
                            }
                        >
                            {loading
                                ? "Buscando..."
                                : "Buscar"}
                        </button>
                    </form>
                </section>

                {generalError && (
                    <div
                        className="lab-payment-message error"
                        role="alert"
                    >
                        {generalError}
                    </div>
                )}

                {orders.length > 0 && (
                    <section className="lab-payment-results">
                        <h2>
                            Órdenes pendientes
                        </h2>

                        <div className="lab-payment-table-wrapper">
                            <table className="lab-payment-table">
                                <thead>
                                    <tr>
                                        <th>Orden</th>
                                        <th>Paciente</th>
                                        <th>Exámenes</th>
                                        <th>Fecha</th>
                                        <th>Total</th>
                                        <th>Acción</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    {orders.map((order) => (
                                        <tr
                                            key={
                                                order
                                                    .laboratoryOrderId
                                            }
                                        >
                                            <td>
                                                {order.orderNumber}
                                            </td>

                                            <td>
                                                {order.patientName}
                                            </td>

                                            <td>
                                                {order.totalTests}
                                            </td>

                                            <td>
                                                {formatDateTime(
                                                    order.createdAt
                                                )}
                                            </td>

                                            <td>
                                                {formatAmount(
                                                    order.totalAmount
                                                )}
                                            </td>

                                            <td>
                                                <button
                                                    type="button"
                                                    className="btn btn-primary"
                                                    onClick={() =>
                                                        handleSelectOrder(
                                                            order
                                                                .laboratoryOrderId
                                                        )
                                                    }
                                                    disabled={
                                                        loadingDetail ||
                                                        processing
                                                    }
                                                >
                                                    {loadingDetail
                                                        ? "Cargando..."
                                                        : "Seleccionar"}
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </section>
                )}

                {selectedOrder && (
                    <section className="lab-payment-content">
                        <div className="lab-order-card">
                            <h2>
                                Detalle de la orden
                            </h2>

                            <div className="lab-order-details">
                                <p>
                                    <span>No. orden</span>
                                    <strong>
                                        {
                                            selectedOrder
                                                .orderNumber
                                        }
                                    </strong>
                                </p>

                                <p>
                                    <span>Paciente</span>
                                    <strong>
                                        {
                                            selectedOrder
                                                .patientName
                                        }
                                    </strong>
                                </p>

                                <p>
                                    <span>DPI</span>
                                    <strong>
                                        {maskDpi(
                                            selectedOrder
                                                .patientDpi
                                        )}
                                    </strong>
                                </p>

                                <p>
                                    <span>Médico</span>
                                    <strong>
                                        {
                                            selectedOrder
                                                .doctorName
                                        }
                                    </strong>
                                </p>

                                <p>
                                    <span>Sucursal</span>
                                    <strong>
                                        {
                                            selectedOrder
                                                .branch
                                        }
                                    </strong>
                                </p>
                            </div>

                            <div className="lab-order-tests">
                                <h3>
                                    Exámenes
                                </h3>

                                {(selectedOrder.items || [])
                                    .map((item) => (
                                        <div
                                            key={
                                                item.itemId ||
                                                item.laboratoryTestId
                                            }
                                            className="lab-order-test"
                                        >
                                            <div>
                                                <strong>
                                                    {
                                                        item
                                                            .testName
                                                    }
                                                </strong>

                                                <span>
                                                    {
                                                        item
                                                            .testCode
                                                    }
                                                </span>
                                            </div>

                                            <b>
                                                {formatAmount(
                                                    item.unitPrice
                                                )}
                                            </b>
                                        </div>
                                    ))}
                            </div>

                            <div className="lab-order-total">
                                <span>
                                    Total a cobrar
                                </span>

                                <strong>
                                    {formatAmount(
                                        selectedOrder
                                            .totalAmount
                                    )}
                                </strong>
                            </div>
                        </div>

                        <div className="lab-payment-form-card">
                            <h2>
                                Registrar pago
                            </h2>

                            <form onSubmit={handlePayment}>
                                <div className="lab-payment-form-group">
                                    <label htmlFor="lab-payment-method">
                                        Método de pago
                                    </label>

                                    <select
                                        id="lab-payment-method"
                                        value={paymentMethod}
                                        onChange={(event) => {
                                            setPaymentMethod(
                                                event.target.value
                                            );

                                            setReceivedAmount("");
                                            setCardLastFour("");
                                            setErrors({});
                                            setGeneralError("");
                                            setIdempotencyKey(
                                                crypto.randomUUID()
                                            );
                                        }}
                                        disabled={processing}
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
                                        <div className="lab-payment-form-group">
                                            <label htmlFor="lab-received-amount">
                                                Monto recibido
                                            </label>

                                            <input
                                                id="lab-received-amount"
                                                type="number"
                                                min="0"
                                                step="0.01"
                                                value={
                                                    receivedAmount
                                                }
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

                                                    setGeneralError("");
                                                }}
                                                disabled={processing}
                                            />

                                            {errors.receivedAmount && (
                                                <div className="error-message">
                                                    {
                                                        errors
                                                            .receivedAmount
                                                    }
                                                </div>
                                            )}
                                        </div>

                                        <div className="lab-payment-change">
                                            <span>
                                                Cambio a devolver
                                            </span>

                                            <strong>
                                                {formatAmount(
                                                    calculateChange()
                                                )}
                                            </strong>
                                        </div>
                                    </>
                                ) : (
                                    <>
                                        <div className="lab-payment-test-notice">
                                            Entorno simulado. No
                                            ingrese información real
                                            de tarjetas.
                                        </div>

                                        <div className="lab-payment-form-group">
                                            <label htmlFor="lab-card-last-four">
                                                Últimos 4 dígitos
                                            </label>

                                            <input
                                                id="lab-card-last-four"
                                                type="text"
                                                inputMode="numeric"
                                                maxLength={4}
                                                value={
                                                    cardLastFour
                                                }
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
                                                            )
                                                            .slice(
                                                                0,
                                                                4
                                                            );

                                                    setCardLastFour(
                                                        value
                                                    );

                                                    setErrors(
                                                        previous => ({
                                                            ...previous,
                                                            cardLastFour:
                                                                ""
                                                        })
                                                    );

                                                    setGeneralError("");
                                                }}
                                                disabled={processing}
                                            />

                                            {errors.cardLastFour && (
                                                <div className="error-message">
                                                    {
                                                        errors
                                                            .cardLastFour
                                                    }
                                                </div>
                                            )}
                                        </div>

                                        <div className="lab-payment-form-group">
                                            <label htmlFor="lab-card-scenario">
                                                Resultado de prueba
                                            </label>

                                            <select
                                                id="lab-card-scenario"
                                                value={cardScenario}
                                                onChange={(event) => {
                                                    setCardScenario(
                                                        event.target.value
                                                    );

                                                    setGeneralError("");
                                                }}
                                                disabled={processing}
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
                                    className="lab-pay-button"
                                    disabled={processing}
                                >
                                    {processing
                                        ? "Registrando pago..."
                                        : `Registrar pago ${formatAmount(
                                            selectedOrder
                                                .totalAmount
                                        )}`}
                                </button>
                            </form>
                        </div>
                    </section>
                )}
            </div>
        </MainLayout>
    );
}

/**
 * Presenta cantidades monetarias.
 */
function formatAmount(value) {
    return `Q${Number(
        value || 0
    ).toFixed(2)}`;
}

/**
 * Presenta fechas en formato local.
 */
function formatDateTime(value) {
    if (!value) {
        return "No disponible";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString(
        "es-GT",
        {
            year: "numeric",
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        }
    );
}

/**
 * Oculta el DPI excepto los últimos cuatro dígitos.
 */
function maskDpi(value) {
    if (!value) {
        return "No registrado";
    }

    const dpi =
        String(value).trim();

    if (dpi.length <= 4) {
        return dpi;
    }

    return (
        "*".repeat(dpi.length - 4) +
        dpi.slice(-4)
    );
}

/**
 * Obtiene el mensaje de error enviado por Spring Boot.
 */
function getBackendMessage(
    error,
    fallback
) {
    const data =
        error.response?.data;

    if (typeof data === "string") {
        return data;
    }

    const validationErrors =
        data?.validationErrors;

    if (validationErrors) {
        const messages =
            Object.values(validationErrors);

        if (messages.length > 0) {
            return messages[0];
        }
    }

    return data?.message || fallback;
}

export default LaboratoryPaymentPage;