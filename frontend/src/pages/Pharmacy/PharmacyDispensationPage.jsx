import {
    useMemo,
    useState
} from "react";

import {
    Link
} from "react-router-dom";

import MainLayout
    from "../../layouts/MainLayout";

import PharmacyDispensationReceipt
    from "./PharmacyDispensationReceipt";

import {
    confirmMedicationDispensation,
    declinePrescriptionDispensation,
    getPrescriptionDispensationDetail,
    searchPrescriptionsForDispensation
} from "../../api/pharmacyDispensationApi";

import "./PharmacyDispensationPage.css";

function PharmacyDispensationPage() {
    const [searchCriteria, setSearchCriteria] =
        useState("PRESCRIPTION_NUMBER");

    const [searchValue, setSearchValue] =
        useState("");

    const [prescriptions, setPrescriptions] =
        useState([]);

    const [
        selectedPrescription,
        setSelectedPrescription
    ] = useState(null);

    /*
     * Conserva las decisiones de despacho
     * para cada medication item.
     */
    const [itemForms, setItemForms] =
        useState({});

    const [paymentMethod, setPaymentMethod] =
        useState("EFECTIVO");

    const [receivedAmount, setReceivedAmount] =
        useState("");

    const [cardLastFour, setCardLastFour] =
        useState("");

    const [paymentScenario, setPaymentScenario] =
        useState("approved");

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

    const [receipt, setReceipt] =
        useState(null);

    /*
* Controla el formulario del flujo alterno:
* el paciente no desea adquirir medicamentos.
*/
    const [
        showDeclineForm,
        setShowDeclineForm
    ] = useState(false);

    const [
        declineReason,
        setDeclineReason
    ] = useState("");

    const [
        declining,
        setDeclining
    ] = useState(false);

    const [
        successMessage,
        setSuccessMessage
    ] = useState("");

    /**
     * Busca una receta activa por número
     * o consulta médica.
     */
    const handleSearch = async (event) => {
        event.preventDefault();

        if (loading || processing) {
            return;
        }

        const cleanValue =
            searchValue.trim();

        const newErrors = {};

        if (!cleanValue) {
            newErrors.searchValue =
                searchCriteria ===
                    "CONSULTATION_ID"
                    ? "Debe ingresar el ID de la consulta."
                    : "Debe ingresar el número de receta.";
        }

        if (
            searchCriteria ===
            "CONSULTATION_ID" &&
            cleanValue &&
            !/^\d+$/.test(cleanValue)
        ) {
            newErrors.searchValue =
                "El ID de consulta debe ser numérico.";
        }

        setErrors(newErrors);

        if (
            Object.keys(newErrors).length > 0
        ) {
            return;
        }

        try {
            setLoading(true);
            setGeneralError("");
            setPrescriptions([]);
            setSelectedPrescription(null);

            const response =
                await searchPrescriptionsForDispensation(
                    searchCriteria,
                    cleanValue
                );

            const found =
                Array.isArray(response.data)
                    ? response.data
                    : [];

            setPrescriptions(found);

            if (found.length === 0) {
                setGeneralError(
                    "No se encontró una receta activa " +
                    "para el criterio ingresado."
                );
            }

        } catch (error) {
            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible buscar la receta."
                )
            );
        } finally {
            setLoading(false);
        }
    };

    /**
     * Obtiene vigencia, cantidades pendientes,
     * stock y posibles alternativas.
     */
    const handleSelectPrescription = async (
        prescriptionId
    ) => {
        try {
            setLoadingDetail(true);
            setGeneralError("");
            setSelectedPrescription(null);

            const response =
                await getPrescriptionDispensationDetail(
                    prescriptionId
                );

            const detail =
                response.data;

            setSelectedPrescription(detail);

            setShowDeclineForm(false);
            setDeclineReason("");
            setSuccessMessage("");

            /*
             * Inicializar el formulario de cada ítem.
             *
             * selected:
             * indica si se incluirá en este despacho.
             *
             * quantity:
             * utiliza la menor cantidad entre stock
             * disponible y cantidad pendiente.
             */
            const initialForms = {};

            (detail.items || []).forEach(item => {
                const defaultQuantity =
                    Math.min(
                        Number(
                            item.pendingQuantity || 0
                        ),
                        Number(
                            item.availableStock || 0
                        )
                    );

                initialForms[
                    item.prescriptionItemId
                ] = {
                    selected:
                        item.pendingQuantity > 0 &&
                        item.availableStock > 0,

                    quantity:
                        defaultQuantity > 0
                            ? String(
                                defaultQuantity
                            )
                            : "",

                    substituted: false,

                    alternativeMedicationId: "",

                    substitutionReason: "",

                    controlAuthorization: ""
                };
            });

            setItemForms(initialForms);
            resetPaymentFields();

        } catch (error) {
            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible cargar la receta."
                )
            );
        } finally {
            setLoadingDetail(false);
        }
    };

    /**
     * Actualiza un campo de un medicamento
     * dentro del formulario.
     */
    const updateItemForm = (
        prescriptionItemId,
        field,
        value
    ) => {
        setItemForms(previous => ({
            ...previous,
            [prescriptionItemId]:
            {
                ...previous[prescriptionItemId],
                [field]: value
            }
        }));

        setErrors(previous => ({
            ...previous,
            [`item-${prescriptionItemId}`]:
                ""
        }));

        setGeneralError("");
    };

    /**
     * Calcula el total informativo del frontend.
     *
     * Spring Boot calculará nuevamente el total
     * usando precios vigentes del backend.
     */
    const calculatedTotal =
        useMemo(() => {
            if (!selectedPrescription) {
                return 0;
            }

            return (
                selectedPrescription.items || []
            ).reduce((total, item) => {
                const form =
                    itemForms[
                    item.prescriptionItemId
                    ];

                if (
                    !form ||
                    !form.selected
                ) {
                    return total;
                }

                const quantity =
                    Number(form.quantity || 0);

                let unitPrice =
                    Number(
                        item.currentUnitPrice || 0
                    );

                if (
                    form.substituted &&
                    form.alternativeMedicationId
                ) {
                    const alternative =
                        (item.alternatives || [])
                            .find(candidate =>
                                String(
                                    candidate.medicationId
                                ) ===
                                String(
                                    form
                                        .alternativeMedicationId
                                )
                            );

                    unitPrice =
                        Number(
                            alternative?.unitPrice || 0
                        );
                }

                return (
                    total +
                    quantity * unitPrice
                );
            }, 0);
        }, [
            selectedPrescription,
            itemForms
        ]);

    /**
     * Calcula el cambio de forma informativa.
     */
    const calculatedChange =
        useMemo(() => {
            if (
                paymentMethod !== "EFECTIVO"
            ) {
                return 0;
            }

            return Math.max(
                Number(receivedAmount || 0) -
                calculatedTotal,
                0
            );
        }, [
            paymentMethod,
            receivedAmount,
            calculatedTotal
        ]);

    /**
     * Valida el formulario antes de enviarlo.
     */
    const validateDispensation = () => {
        const newErrors = {};

        if (!selectedPrescription) {
            newErrors.prescription =
                "Debe seleccionar una receta.";
        }

        if (
            selectedPrescription &&
            !selectedPrescription.canDispense
        ) {
            newErrors.prescription =
                "La receta no está habilitada " +
                "para despacho.";
        }

        const selectedItems =
            (selectedPrescription?.items || [])
                .filter(item =>
                    itemForms[
                        item.prescriptionItemId
                    ]?.selected
                );

        if (selectedItems.length === 0) {
            newErrors.items =
                "Debe seleccionar al menos un medicamento.";
        }

        selectedItems.forEach(item => {
            const form =
                itemForms[
                item.prescriptionItemId
                ];

            const quantity =
                Number(form.quantity);

            const maximumAvailable =
                form.substituted
                    ? getAlternativeStock(
                        item,
                        form
                            .alternativeMedicationId
                    )
                    : Number(
                        item.availableStock || 0
                    );

            if (
                !Number.isInteger(quantity) ||
                quantity <= 0
            ) {
                newErrors[
                    `item-${item.prescriptionItemId}`
                ] =
                    "La cantidad debe ser un número entero mayor que cero.";

            } else if (
                quantity >
                Number(item.pendingQuantity)
            ) {
                newErrors[
                    `item-${item.prescriptionItemId}`
                ] =
                    "La cantidad no puede exceder " +
                    "la cantidad pendiente.";

            } else if (
                quantity >
                maximumAvailable
            ) {
                newErrors[
                    `item-${item.prescriptionItemId}`
                ] =
                    "La cantidad excede el stock disponible.";
            }

            if (form.substituted) {
                if (
                    !form
                        .alternativeMedicationId
                ) {
                    newErrors[
                        `item-${item.prescriptionItemId}`
                    ] =
                        "Debe seleccionar un medicamento alternativo.";
                }

                if (
                    !form
                        .substitutionReason
                        .trim()
                ) {
                    newErrors[
                        `item-${item.prescriptionItemId}`
                    ] =
                        "Debe justificar la sustitución.";
                }
            }

            const selectedAlternative =
                form.substituted
                    ? getAlternative(
                        item,
                        form
                            .alternativeMedicationId
                    )
                    : null;

            const requiresAuthorization =
                Boolean(
                    item.controlledMedication
                ) ||
                Boolean(
                    selectedAlternative
                        ?.controlled
                );

            if (
                requiresAuthorization &&
                !form
                    .controlAuthorization
                    .trim()
            ) {
                newErrors[
                    `item-${item.prescriptionItemId}`
                ] =
                    "Debe registrar la autorización " +
                    "del medicamento controlado.";
            }
        });

        if (paymentMethod === "EFECTIVO") {
            const received =
                Number(receivedAmount);

            if (
                receivedAmount === "" ||
                !Number.isFinite(received) ||
                received <= 0
            ) {
                newErrors.receivedAmount =
                    "Debe ingresar un monto recibido válido.";

            } else if (
                received <
                calculatedTotal
            ) {
                newErrors.receivedAmount =
                    "El monto recibido es menor " +
                    "al total informativo.";
            }

        } else {
            if (
                !/^\d{4}$/.test(
                    cardLastFour
                )
            ) {
                newErrors.cardLastFour =
                    "Debe ingresar exactamente " +
                    "los últimos 4 dígitos.";
            }
        }

        setErrors(newErrors);

        return (
            Object.keys(newErrors).length === 0
        );
    };

    /**
     * Confirma el despacho mediante Spring Boot.
     */
    const handleConfirmDispensation = async (event) => {
        event.preventDefault();

        if (processing) {
            return;
        }

        setGeneralError("");

        if (!validateDispensation()) {
            return;
        }

        const items =
            selectedPrescription.items
                .filter(item =>
                    itemForms[
                        item
                            .prescriptionItemId
                    ].selected
                )
                .map(item => {
                    const form =
                        itemForms[
                        item
                            .prescriptionItemId
                        ];

                    return {
                        prescriptionItemId:
                            item
                                .prescriptionItemId,

                        dispensedQuantity:
                            Number(
                                form.quantity
                            ),

                        substituted:
                            Boolean(
                                form.substituted
                            ),

                        alternativeMedicationId:
                            form.substituted
                                ? Number(
                                    form
                                        .alternativeMedicationId
                                )
                                : null,

                        substitutionReason:
                            form.substituted
                                ? form
                                    .substitutionReason
                                    .trim()
                                : null,

                        controlAuthorization:
                            form
                                .controlAuthorization
                                .trim() ||
                            null
                    };
                });

        const request = {
            prescriptionId:
                selectedPrescription
                    .prescriptionId,

            idempotencyKey,

            paymentMethod,

            receivedAmount:
                paymentMethod ===
                    "EFECTIVO"
                    ? Number(
                        receivedAmount
                    )
                    : null,

            cardLastFour:
                paymentMethod ===
                    "EFECTIVO"
                    ? null
                    : cardLastFour,

            paymentToken:
                paymentMethod ===
                    "EFECTIVO"
                    ? null
                    : getPaymentToken(
                        paymentScenario
                    ),

            items
        };

        try {
            setProcessing(true);

            const response =
                await confirmMedicationDispensation(
                    request
                );

            const result =
                response.data;

            if (
                result.paymentStatus ===
                "APROBADO"
            ) {
                setReceipt(result);
                return;
            }

            /*
             * Después de rechazo o error,
             * el siguiente intento necesita
             * una nueva llave.
             */
            setIdempotencyKey(
                crypto.randomUUID()
            );

            setCardLastFour("");

            setGeneralError(
                result.message ||
                "El pago no fue aprobado."
            );

        } catch (error) {
            setIdempotencyKey(
                crypto.randomUUID()
            );

            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible confirmar el despacho."
                )
            );
        } finally {
            setProcessing(false);
        }
    };

    /**
* Registra que el paciente decidió no adquirir
* los medicamentos.
*
* Spring Boot no modifica inventario, cantidades
* ni estado de la receta.
*/
    const handleDeclinePrescription = async () => {
        if (
            declining ||
            !selectedPrescription
        ) {
            return;
        }

        const cleanReason =
            declineReason.trim();

        if (!cleanReason) {
            setErrors(previous => ({
                ...previous,
                declineReason:
                    "Debe indicar el motivo."
            }));

            return;
        }

        if (cleanReason.length > 500) {
            setErrors(previous => ({
                ...previous,
                declineReason:
                    "El motivo no puede exceder " +
                    "los 500 caracteres."
            }));

            return;
        }

        try {
            setDeclining(true);
            setGeneralError("");
            setSuccessMessage("");

            const response =
                await declinePrescriptionDispensation(
                    selectedPrescription
                        .prescriptionId,
                    cleanReason
                );

            setSuccessMessage(
                response.data.message
            );

            setShowDeclineForm(false);
            setDeclineReason("");

        } catch (error) {
            setGeneralError(
                getBackendMessage(
                    error,
                    "No fue posible registrar "
                    + "la decisión del paciente."
                )
            );
        } finally {
            setDeclining(false);
        }
    };

    /**
     * Restablece toda la pantalla.
     */
    const handleNewDispensation = () => {
        setSearchCriteria(
            "PRESCRIPTION_NUMBER"
        );

        setSearchValue("");
        setPrescriptions([]);
        setSelectedPrescription(null);
        setItemForms({});
        setReceipt(null);
        setGeneralError("");
        setErrors({});
        setShowDeclineForm(false);
        setDeclineReason("");
        setSuccessMessage("");

        resetPaymentFields();
    };

    const resetPaymentFields = () => {
        setPaymentMethod("EFECTIVO");
        setReceivedAmount("");
        setCardLastFour("");
        setPaymentScenario("approved");
        setIdempotencyKey(
            crypto.randomUUID()
        );
    };

    if (receipt) {
        return (
            <MainLayout>
                <PharmacyDispensationReceipt
                    dispensation={receipt}
                    onNewDispensation={
                        handleNewDispensation
                    }
                />
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="pharmacy-dispensation-page">
                <header className="pharmacy-dispensation-header">
                    <div>
                        <h1>
                            Despacho de Medicamentos
                        </h1>

                        <p>
                            Busque una receta vigente,
                            seleccione las cantidades y
                            confirme el despacho.
                        </p>
                    </div>

                    <Link
                        to="/pharmacy"
                        className="pharmacy-back-link"
                    >
                        Volver a Farmacia
                    </Link>
                </header>

                <section className="pharmacy-search-card">
                    <h2>
                        Buscar receta
                    </h2>

                    <div className="pharmacy-search-types">
                        <button
                            type="button"
                            className={
                                searchCriteria ===
                                    "PRESCRIPTION_NUMBER"
                                    ? "active"
                                    : ""
                            }
                            onClick={() => {
                                setSearchCriteria(
                                    "PRESCRIPTION_NUMBER"
                                );

                                setSearchValue("");
                                setErrors({});
                                setGeneralError("");
                            }}
                        >
                            Por número de receta
                        </button>

                        <button
                            type="button"
                            className={
                                searchCriteria ===
                                    "CONSULTATION_ID"
                                    ? "active"
                                    : ""
                            }
                            onClick={() => {
                                setSearchCriteria(
                                    "CONSULTATION_ID"
                                );

                                setSearchValue("");
                                setErrors({});
                                setGeneralError("");
                            }}
                        >
                            Por ID de consulta
                        </button>
                    </div>

                    <form
                        className="pharmacy-search-form"
                        onSubmit={handleSearch}
                    >
                        <div>
                            <input
                                type="text"
                                inputMode={
                                    searchCriteria ===
                                        "CONSULTATION_ID"
                                        ? "numeric"
                                        : "text"
                                }
                                placeholder={
                                    searchCriteria ===
                                        "CONSULTATION_ID"
                                        ? "Ejemplo: 25"
                                        : "Ejemplo: REC-A1B2C3D4E5F6"
                                }
                                value={searchValue}
                                onChange={(event) => {
                                    let value =
                                        event.target.value;

                                    if (
                                        searchCriteria ===
                                        "CONSULTATION_ID"
                                    ) {
                                        value =
                                            value.replace(
                                                /\D/g,
                                                ""
                                            );
                                    } else {
                                        value =
                                            value.toUpperCase();
                                    }

                                    setSearchValue(value);
                                    setErrors({});
                                    setGeneralError("");
                                }}
                                disabled={
                                    loading ||
                                    processing
                                }
                            />

                            {errors.searchValue && (
                                <div className="pharmacy-field-error">
                                    {
                                        errors
                                            .searchValue
                                    }
                                </div>
                            )}
                        </div>

                        <button
                            type="submit"
                            className="pharmacy-primary-button"
                            disabled={
                                loading ||
                                processing
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
                        className="pharmacy-page-message error"
                        role="alert"
                    >
                        {generalError}
                    </div>
                )}

                {successMessage && (
                    <div
                        className="pharmacy-page-message success"
                        role="status"
                    >
                        {successMessage}
                    </div>
                )}

                {prescriptions.length > 0 && (
                    <section className="pharmacy-results-card">
                        <h2>
                            Resultado
                        </h2>

                        {prescriptions.map(
                            prescription => (
                                <article
                                    key={
                                        prescription
                                            .prescriptionId
                                    }
                                    className="pharmacy-prescription-result"
                                >
                                    <div>
                                        <strong>
                                            {
                                                prescription
                                                    .prescriptionNumber
                                            }
                                        </strong>

                                        <span>
                                            {
                                                prescription
                                                    .patientName
                                            }
                                        </span>

                                        <small>
                                            Estado:{" "}
                                            {formatStatus(
                                                prescription
                                                    .status
                                            )}
                                            {" · "}
                                            Días:{" "}
                                            {
                                                prescription
                                                    .elapsedDays
                                            }
                                        </small>
                                    </div>

                                    <button
                                        type="button"
                                        className="pharmacy-primary-button"
                                        disabled={
                                            !prescription
                                                .canDispense ||
                                            loadingDetail
                                        }
                                        onClick={() =>
                                            handleSelectPrescription(
                                                prescription
                                                    .prescriptionId
                                            )
                                        }
                                    >
                                        {loadingDetail
                                            ? "Cargando..."
                                            : "Seleccionar"}
                                    </button>
                                </article>
                            )
                        )}
                    </section>
                )}

                {errors.prescription && (
                    <div className="pharmacy-page-message error">
                        {errors.prescription}
                    </div>
                )}

                {selectedPrescription && (
                    <form
                        onSubmit={
                            handleConfirmDispensation
                        }
                    >
                        <section className="pharmacy-prescription-card">
                            <h2>
                                Detalle de la receta
                            </h2>

                            <div className="pharmacy-prescription-summary">
                                <SummaryItem
                                    label="Receta"
                                    value={
                                        selectedPrescription
                                            .prescriptionNumber
                                    }
                                />

                                <SummaryItem
                                    label="Paciente"
                                    value={
                                        selectedPrescription
                                            .patientName
                                    }
                                />

                                <SummaryItem
                                    label="Médico"
                                    value={
                                        selectedPrescription
                                            .doctorName
                                    }
                                />

                                <SummaryItem
                                    label="Sucursal"
                                    value={
                                        selectedPrescription
                                            .pharmacyBranch
                                    }
                                />

                                <SummaryItem
                                    label="Estado"
                                    value={formatStatus(
                                        selectedPrescription
                                            .prescriptionStatus
                                    )}
                                />

                                <SummaryItem
                                    label="Vigencia restante"
                                    value={
                                        `${selectedPrescription
                                            .remainingValidityDays} días`
                                    }
                                />
                            </div>

                            <p className="pharmacy-detail-message">
                                {
                                    selectedPrescription
                                        .message
                                }
                            </p>
                        </section>

                        <section className="pharmacy-medications-card">
                            <h2>
                                Medicamentos
                            </h2>

                            {errors.items && (
                                <div className="pharmacy-field-error">
                                    {errors.items}
                                </div>
                            )}

                            {(selectedPrescription.items || [])
                                .map(item => (
                                    <MedicationForm
                                        key={
                                            item
                                                .prescriptionItemId
                                        }
                                        item={item}
                                        form={
                                            itemForms[
                                            item
                                                .prescriptionItemId
                                            ] || {}
                                        }
                                        error={
                                            errors[
                                            `item-${item
                                                .prescriptionItemId}`
                                            ]
                                        }
                                        processing={
                                            processing
                                        }
                                        onChange={(
                                            field,
                                            value
                                        ) =>
                                            updateItemForm(
                                                item
                                                    .prescriptionItemId,
                                                field,
                                                value
                                            )
                                        }
                                    />
                                ))}
                        </section>

                        <section className="pharmacy-payment-card">
                            <h2>
                                Cobro
                            </h2>

                            <div className="pharmacy-total">
                                <span>
                                    Total informativo
                                </span>

                                <strong>
                                    {formatAmount(
                                        calculatedTotal
                                    )}
                                </strong>
                            </div>

                            <div className="pharmacy-form-group">
                                <label>
                                    Método de pago
                                </label>

                                <select
                                    value={
                                        paymentMethod
                                    }
                                    onChange={(event) => {
                                        setPaymentMethod(
                                            event.target
                                                .value
                                        );

                                        resetPaymentFields();

                                        setPaymentMethod(
                                            event.target
                                                .value
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
                                    <div className="pharmacy-form-group">
                                        <label>
                                            Monto recibido
                                        </label>

                                        <input
                                            type="number"
                                            min="0"
                                            step="0.01"
                                            value={
                                                receivedAmount
                                            }
                                            onChange={event =>
                                                setReceivedAmount(
                                                    event
                                                        .target
                                                        .value
                                                )
                                            }
                                            disabled={
                                                processing
                                            }
                                        />

                                        {errors.receivedAmount && (
                                            <div className="pharmacy-field-error">
                                                {
                                                    errors
                                                        .receivedAmount
                                                }
                                            </div>
                                        )}
                                    </div>

                                    <div className="pharmacy-change">
                                        <span>
                                            Cambio
                                        </span>

                                        <strong>
                                            {formatAmount(
                                                calculatedChange
                                            )}
                                        </strong>
                                    </div>
                                </>
                            ) : (
                                <>
                                    <div className="pharmacy-card-notice">
                                        Entorno simulado.
                                        No ingrese información
                                        real de tarjetas.
                                    </div>

                                    <div className="pharmacy-form-group">
                                        <label>
                                            Últimos 4 dígitos
                                        </label>

                                        <input
                                            type="text"
                                            inputMode="numeric"
                                            maxLength={4}
                                            value={
                                                cardLastFour
                                            }
                                            onChange={event =>
                                                setCardLastFour(
                                                    event
                                                        .target
                                                        .value
                                                        .replace(
                                                            /\D/g,
                                                            ""
                                                        )
                                                        .slice(
                                                            0,
                                                            4
                                                        )
                                                )
                                            }
                                            disabled={
                                                processing
                                            }
                                        />

                                        {errors.cardLastFour && (
                                            <div className="pharmacy-field-error">
                                                {
                                                    errors
                                                        .cardLastFour
                                                }
                                            </div>
                                        )}
                                    </div>

                                    <div className="pharmacy-form-group">
                                        <label>
                                            Resultado de prueba
                                        </label>

                                        <select
                                            value={
                                                paymentScenario
                                            }
                                            onChange={event =>
                                                setPaymentScenario(
                                                    event
                                                        .target
                                                        .value
                                                )
                                            }
                                            disabled={
                                                processing
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
                                className="pharmacy-confirm-button"
                                disabled={
                                    processing ||
                                    calculatedTotal <= 0 ||
                                    !selectedPrescription
                                        .canDispense
                                }
                            >
                                {processing
                                    ? "Confirmando despacho..."
                                    : `Confirmar despacho ${formatAmount(
                                        calculatedTotal
                                    )}`}
                            </button>

                            <div className="pharmacy-decline-section">
                                {!showDeclineForm ? (
                                    <button
                                        type="button"
                                        className="pharmacy-decline-button"
                                        onClick={() => {
                                            setShowDeclineForm(true);
                                            setGeneralError("");
                                            setSuccessMessage("");
                                        }}
                                        disabled={
                                            processing ||
                                            declining
                                        }
                                    >
                                        El paciente no adquirirá los medicamentos
                                    </button>
                                ) : (
                                    <div className="pharmacy-decline-form">
                                        <h3>
                                            Registrar medicamentos no adquiridos
                                        </h3>

                                        <p>
                                            Esta acción no cancela la receta
                                            ni modifica el inventario.
                                        </p>

                                        <div className="pharmacy-form-group">
                                            <label htmlFor="decline-reason">
                                                Motivo
                                            </label>

                                            <textarea
                                                id="decline-reason"
                                                rows={4}
                                                maxLength={500}
                                                value={declineReason}
                                                onChange={(event) => {
                                                    setDeclineReason(
                                                        event.target.value
                                                    );

                                                    setErrors(previous => ({
                                                        ...previous,
                                                        declineReason: ""
                                                    }));
                                                }}
                                                disabled={declining}
                                            />

                                            <small>
                                                {declineReason.length}/500
                                            </small>

                                            {errors.declineReason && (
                                                <div className="pharmacy-field-error">
                                                    {errors.declineReason}
                                                </div>
                                            )}
                                        </div>

                                        <div className="pharmacy-decline-actions">
                                            <button
                                                type="button"
                                                className="pharmacy-secondary-button"
                                                onClick={() => {
                                                    setShowDeclineForm(false);
                                                    setDeclineReason("");
                                                }}
                                                disabled={declining}
                                            >
                                                Cancelar
                                            </button>

                                            <button
                                                type="button"
                                                className="pharmacy-decline-confirm"
                                                onClick={
                                                    handleDeclinePrescription
                                                }
                                                disabled={declining}
                                            >
                                                {declining
                                                    ? "Registrando..."
                                                    : "Confirmar decisión"}
                                            </button>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </section>
                    </form>
                )}
            </div>
        </MainLayout>
    );
}

function MedicationForm({
    item,
    form,
    error,
    processing,
    onChange
}) {
    const selectedAlternative =
        getAlternative(
            item,
            form.alternativeMedicationId
        );

    const availableStock =
        form.substituted
            ? Number(
                selectedAlternative
                    ?.availableStock || 0
            )
            : Number(
                item.availableStock || 0
            );

    const controlled =
        Boolean(item.controlledMedication) ||
        Boolean(
            selectedAlternative?.controlled
        );

    return (
        <article
            className={
                form.selected
                    ? "pharmacy-medication selected"
                    : "pharmacy-medication"
            }
        >
            <label className="pharmacy-medication-select">
                <input
                    type="checkbox"
                    checked={
                        Boolean(form.selected)
                    }
                    onChange={event =>
                        onChange(
                            "selected",
                            event.target.checked
                        )
                    }
                    disabled={
                        processing ||
                        item.pendingQuantity <= 0
                    }
                />

                <div>
                    <strong>
                        {item.medicationName}
                    </strong>

                    <span>
                        {item.medicationCode}
                        {" · "}
                        {item.concentration}
                        {" · "}
                        {item.presentation}
                    </span>
                </div>
            </label>

            <div className="pharmacy-medication-data">
                <span>
                    Prescrito:
                    <strong>
                        {item.prescribedQuantity}
                    </strong>
                </span>

                <span>
                    Entregado:
                    <strong>
                        {
                            item
                                .alreadyDispensedQuantity
                        }
                    </strong>
                </span>

                <span>
                    Pendiente:
                    <strong>
                        {item.pendingQuantity}
                    </strong>
                </span>

                <span>
                    Stock:
                    <strong>
                        {availableStock}
                    </strong>
                </span>
            </div>

            <p className="pharmacy-usage-instructions">
                {item.dosage}
                {" · "}
                {item.frequency}
                {" · "}
                {item.duration}
            </p>

            {item.specialInstructions && (
                <p className="pharmacy-special-instructions">
                    {item.specialInstructions}
                </p>
            )}

            {form.selected && (
                <div className="pharmacy-medication-fields">
                    <div className="pharmacy-form-group">
                        <label>
                            Cantidad a entregar
                        </label>

                        <input
                            type="number"
                            min="1"
                            max={Math.min(
                                item.pendingQuantity,
                                availableStock
                            )}
                            step="1"
                            value={
                                form.quantity || ""
                            }
                            onChange={event =>
                                onChange(
                                    "quantity",
                                    event.target.value
                                )
                            }
                            disabled={processing}
                        />
                    </div>

                    <label className="pharmacy-substitution-check">
                        <input
                            type="checkbox"
                            checked={
                                Boolean(
                                    form.substituted
                                )
                            }
                            onChange={event => {
                                onChange(
                                    "substituted",
                                    event.target.checked
                                );

                                onChange(
                                    "alternativeMedicationId",
                                    ""
                                );

                                onChange(
                                    "substitutionReason",
                                    ""
                                );
                            }}
                            disabled={
                                processing ||
                                (
                                    item.alternatives ||
                                    []
                                ).length === 0
                            }
                        />

                        Sustituir medicamento
                    </label>

                    {form.substituted && (
                        <>
                            <div className="pharmacy-form-group">
                                <label>
                                    Medicamento alternativo
                                </label>

                                <select
                                    value={
                                        form
                                            .alternativeMedicationId ||
                                        ""
                                    }
                                    onChange={event => {
                                        onChange(
                                            "alternativeMedicationId",
                                            event.target.value
                                        );

                                        const alternative =
                                            getAlternative(
                                                item,
                                                event
                                                    .target
                                                    .value
                                            );

                                        const quantity =
                                            Math.min(
                                                Number(
                                                    item
                                                        .pendingQuantity
                                                ),
                                                Number(
                                                    alternative
                                                        ?.availableStock ||
                                                    0
                                                )
                                            );

                                        onChange(
                                            "quantity",
                                            quantity > 0
                                                ? String(
                                                    quantity
                                                )
                                                : ""
                                        );
                                    }}
                                    disabled={processing}
                                >
                                    <option value="">
                                        Seleccione una alternativa
                                    </option>

                                    {(item.alternatives || [])
                                        .map(alternative => (
                                            <option
                                                key={
                                                    alternative
                                                        .medicationId
                                                }
                                                value={
                                                    alternative
                                                        .medicationId
                                                }
                                            >
                                                {
                                                    alternative
                                                        .name
                                                }
                                                {" · "}
                                                {
                                                    alternative
                                                        .concentration
                                                }
                                                {" · Stock: "}
                                                {
                                                    alternative
                                                        .availableStock
                                                }
                                                {" · "}
                                                {formatAmount(
                                                    alternative
                                                        .unitPrice
                                                )}
                                            </option>
                                        ))}
                                </select>
                            </div>

                            <div className="pharmacy-form-group">
                                <label>
                                    Justificación
                                </label>

                                <textarea
                                    rows={3}
                                    maxLength={1000}
                                    value={
                                        form
                                            .substitutionReason ||
                                        ""
                                    }
                                    onChange={event =>
                                        onChange(
                                            "substitutionReason",
                                            event.target.value
                                        )
                                    }
                                    disabled={processing}
                                />
                            </div>
                        </>
                    )}

                    {controlled && (
                        <div className="pharmacy-form-group controlled">
                            <label>
                                Autorización de medicamento controlado
                            </label>

                            <input
                                type="text"
                                maxLength={200}
                                value={
                                    form
                                        .controlAuthorization ||
                                    ""
                                }
                                onChange={event =>
                                    onChange(
                                        "controlAuthorization",
                                        event.target.value
                                    )
                                }
                                disabled={processing}
                            />
                        </div>
                    )}
                </div>
            )}

            <div className="pharmacy-medication-message">
                {item.message}
            </div>

            {error && (
                <div className="pharmacy-field-error">
                    {error}
                </div>
            )}
        </article>
    );
}

function SummaryItem({
    label,
    value
}) {
    return (
        <div>
            <span>{label}</span>
            <strong>
                {value || "No disponible"}
            </strong>
        </div>
    );
}

function getAlternative(
    item,
    alternativeId
) {
    return (item.alternatives || [])
        .find(alternative =>
            String(
                alternative.medicationId
            ) ===
            String(alternativeId)
        );
}

function getAlternativeStock(
    item,
    alternativeId
) {
    return Number(
        getAlternative(
            item,
            alternativeId
        )?.availableStock || 0
    );
}

function getPaymentToken(scenario) {
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

    return tokens[scenario];
}

function formatAmount(value) {
    return `Q${Number(
        value || 0
    ).toFixed(2)}`;
}

function formatStatus(value) {
    if (!value) {
        return "No disponible";
    }

    return String(value)
        .replaceAll("_", " ");
}

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
            Object.values(
                validationErrors
            );

        if (messages.length > 0) {
            return messages[0];
        }
    }

    return data?.message || fallback;
}

export default PharmacyDispensationPage;