import "./PharmacyDispensationReceipt.css";

/**
 * Muestra el comprobante generado después de
 * confirmar un despacho de medicamentos.
 */
function PharmacyDispensationReceipt({
    dispensation,
    onNewDispensation
}) {
    const items =
        Array.isArray(dispensation?.items)
            ? dispensation.items
            : [];

    const alerts =
        Array.isArray(
            dispensation?.stockAlerts
        )
            ? dispensation.stockAlerts
            : [];

    /**
     * Abre el diálogo de impresión del navegador.
     */
    const handlePrint = () => {
        window.print();
    };

    return (
        <section className="pharmacy-receipt">
            <div className="pharmacy-receipt-icon">
                ✓
            </div>

            <h1>
                Despacho confirmado
            </h1>

            <p className="pharmacy-receipt-message">
                {dispensation.message}
            </p>

            <div className="pharmacy-receipt-content">
                <header className="pharmacy-receipt-header">
                    <h2>
                        Hospital HIS
                    </h2>

                    <p>
                        Comprobante de despacho de medicamentos
                    </p>
                </header>

                <section className="pharmacy-receipt-details">
                    <ReceiptRow
                        label="No. despacho"
                        value={
                            dispensation
                                .dispensationNumber
                        }
                    />

                    <ReceiptRow
                        label="No. transacción"
                        value={
                            dispensation
                                .transactionNumber
                        }
                    />

                    <ReceiptRow
                        label="No. receta"
                        value={
                            dispensation
                                .prescriptionNumber
                        }
                    />

                    <ReceiptRow
                        label="Paciente"
                        value={
                            dispensation.patientName
                        }
                    />

                    <ReceiptRow
                        label="DPI"
                        value={maskDpi(
                            dispensation.patientDpi
                        )}
                    />

                    <ReceiptRow
                        label="Médico"
                        value={
                            dispensation.doctorName
                        }
                    />

                    <ReceiptRow
                        label="Sucursal"
                        value={
                            dispensation.branch
                        }
                    />

                    <ReceiptRow
                        label="Estado del despacho"
                        value={formatStatus(
                            dispensation
                                .dispensationStatus
                        )}
                    />

                    <ReceiptRow
                        label="Estado de la receta"
                        value={formatStatus(
                            dispensation
                                .prescriptionStatus
                        )}
                    />
                </section>

                <section className="pharmacy-receipt-items">
                    <h3>
                        Medicamentos entregados
                    </h3>

                    {items.length === 0 ? (
                        <p>
                            El intento no generó entrega
                            de medicamentos.
                        </p>
                    ) : (
                        items.map(item => (
                            <article
                                key={
                                    item
                                        .dispensationItemId
                                }
                                className="pharmacy-receipt-item"
                            >
                                <div className="pharmacy-receipt-item-main">
                                    <div>
                                        <strong>
                                            {
                                                item
                                                    .dispensedMedicationName
                                            }
                                        </strong>

                                        {item.substituted && (
                                            <span className="pharmacy-substitution-label">
                                                Sustitución
                                            </span>
                                        )}
                                    </div>

                                    <b>
                                        {formatAmount(
                                            item.subtotal
                                        )}
                                    </b>
                                </div>

                                {item.substituted && (
                                    <div className="pharmacy-receipt-substitution">
                                        <p>
                                            <strong>
                                                Medicamento original:
                                            </strong>{" "}
                                            {
                                                item
                                                    .originalMedicationName
                                            }
                                        </p>

                                        <p>
                                            <strong>
                                                Justificación:
                                            </strong>{" "}
                                            {
                                                item
                                                    .substitutionReason
                                            }
                                        </p>
                                    </div>
                                )}

                                <div className="pharmacy-receipt-item-data">
                                    <span>
                                        Cantidad:{" "}
                                        {
                                            item
                                                .dispensedQuantity
                                        }
                                    </span>

                                    <span>
                                        Precio unitario:{" "}
                                        {formatAmount(
                                            item.unitPrice
                                        )}
                                    </span>

                                    <span>
                                        Stock restante:{" "}
                                        {item.stockAfter}
                                    </span>

                                    <span>
                                        Estado:{" "}
                                        {formatStatus(
                                            item.status
                                        )}
                                    </span>
                                </div>

                                {item.stockAlert && (
                                    <div className="pharmacy-item-alert">
                                        {item.stockAlert}
                                    </div>
                                )}
                            </article>
                        ))
                    )}
                </section>

                <section className="pharmacy-receipt-details">
                    <ReceiptRow
                        label="Método de pago"
                        value={formatStatus(
                            dispensation.paymentMethod
                        )}
                    />

                    <ReceiptRow
                        label="Estado del pago"
                        value={formatStatus(
                            dispensation.paymentStatus
                        )}
                    />

                    {dispensation.cardLastFour && (
                        <ReceiptRow
                            label="Tarjeta"
                            value={
                                `**** ${dispensation
                                    .cardLastFour}`
                            }
                        />
                    )}

                    <ReceiptRow
                        label="Total"
                        value={formatAmount(
                            dispensation.totalAmount
                        )}
                        important
                    />

                    {dispensation.paymentMethod ===
                        "EFECTIVO" && (
                            <>
                                <ReceiptRow
                                    label="Monto recibido"
                                    value={formatAmount(
                                        dispensation
                                            .receivedAmount
                                    )}
                                />

                                <ReceiptRow
                                    label="Cambio entregado"
                                    value={formatAmount(
                                        dispensation
                                            .changeAmount
                                    )}
                                />
                            </>
                        )}

                    <ReceiptRow
                        label="Fecha"
                        value={formatDateTime(
                            dispensation.confirmedAt
                        )}
                    />

                    <ReceiptRow
                        label="Usuario de Farmacia"
                        value={
                            dispensation
                                .pharmacyUdbeddrname
                        }
                    />
                </section>

                {alerts.length > 0 && (
                    <section className="pharmacy-receipt-alerts">
                        <h3>
                            Alertas de inventario
                        </h3>

                        {alerts.map(
                            (alert, index) => (
                                <div
                                    key={`${index}-${alert}`}
                                >
                                    {alert}
                                </div>
                            )
                        )}
                    </section>
                )}
            </div>

            <div className="pharmacy-receipt-actions">
                <button
                    type="button"
                    className="pharmacy-secondary-button"
                    onClick={handlePrint}
                >
                    Imprimir comprobante
                </button>

                <button
                    type="button"
                    className="pharmacy-primary-button"
                    onClick={onNewDispensation}
                >
                    Nuevo despacho
                </button>
            </div>
        </section>
    );
}

/**
 * Renderiza una fila común del comprobante.
 */
function ReceiptRow({
    label,
    value,
    important = false
}) {
    return (
        <p
            className={
                important
                    ? "pharmacy-receipt-row important"
                    : "pharmacy-receipt-row"
            }
        >
            <span>{label}</span>

            <strong>
                {value || "No disponible"}
            </strong>
        </p>
    );
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
            month: "long",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        }
    );
}

function maskDpi(value) {
    if (!value) {
        return "No registrado";
    }

    const dpi = String(value);

    if (dpi.length <= 4) {
        return dpi;
    }

    return (
        "*".repeat(dpi.length - 4) +
        dpi.slice(-4)
    );
}

export default PharmacyDispensationReceipt;