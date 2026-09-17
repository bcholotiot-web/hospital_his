import "./LaboratoryPaymentReceipt.css";

function LaboratoryPaymentReceipt({
    payment,
    onNewPayment
}) {
    /**
     * Presenta la fecha y hora en formato local.
     */
    const formatDateTime = (value) => {
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
    };

    /**
     * Presenta montos en quetzales con dos decimales.
     */
    const formatAmount = (amount) => {
        return `Q${Number(
            amount || 0
        ).toFixed(2)}`;
    };

    /**
     * Convierte valores de enum en texto legible.
     *
     * PENDIENTE_DE_PAGO
     * se convierte en:
     * PENDIENTE DE PAGO
     */
    const formatStatus = (status) => {
        if (!status) {
            return "No disponible";
        }

        return String(status)
            .replaceAll("_", " ");
    };

    /**
     * Abre el diálogo de impresión del navegador.
     */
    const handlePrint = () => {
        window.print();
    };

    const items =
        Array.isArray(payment?.items)
            ? payment.items
            : [];

    return (
        <section className="lab-receipt">
            <div className="lab-receipt-success-icon">
                ✓
            </div>

            <h2>
                Pago de laboratorio registrado
            </h2>

            <p className="lab-receipt-message">
                {payment.message}
            </p>

            <div className="lab-receipt-content">
                <header className="lab-receipt-hospital">
                    <h3>Hospital HIS</h3>

                    <p>
                        Comprobante de pago de laboratorio
                    </p>
                </header>

                <div className="lab-receipt-details">
                    <p>
                        <span>No. transacción</span>

                        <strong>
                            {payment.transactionNumber}
                        </strong>
                    </p>

                    <p>
                        <span>No. orden</span>

                        <strong>
                            {payment.orderNumber}
                        </strong>
                    </p>

                    <p>
                        <span>Paciente</span>

                        <strong>
                            {payment.patientName}
                        </strong>
                    </p>

                    <p>
                        <span>DPI</span>

                        <strong>
                            {maskDpi(payment.patientDpi)}
                        </strong>
                    </p>

                    <p>
                        <span>Médico</span>

                        <strong>
                            {payment.doctorName}
                        </strong>
                    </p>

                    <p>
                        <span>Sucursal</span>

                        <strong>
                            {payment.branch}
                        </strong>
                    </p>

                    <p>
                        <span>Estado del pago</span>

                        <strong>
                            {formatStatus(
                                payment.paymentStatus
                            )}
                        </strong>
                    </p>

                    <p>
                        <span>Estado de la orden</span>

                        <strong>
                            {formatStatus(
                                payment
                                    .laboratoryOrderStatus
                            )}
                        </strong>
                    </p>
                </div>

                <section className="lab-receipt-tests">
                    <h4>
                        Exámenes cobrados
                    </h4>

                    {items.length === 0 ? (
                        <p>
                            No se recibió el detalle de
                            exámenes.
                        </p>
                    ) : (
                        <div className="lab-receipt-test-list">
                            {items.map((item) => (
                                <div
                                    key={
                                        item.itemId ||
                                        item.laboratoryTestId
                                    }
                                    className="lab-receipt-test"
                                >
                                    <div>
                                        <strong>
                                            {item.testName}
                                        </strong>

                                        <span>
                                            {item.testCode}
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
                    )}
                </section>

                <div className="lab-receipt-details">
                    <p>
                        <span>Método de pago</span>

                        <strong>
                            {formatStatus(
                                payment.paymentMethod
                            )}
                        </strong>
                    </p>

                    {payment.cardLastFour && (
                        <p>
                            <span>Tarjeta</span>

                            <strong>
                                **** {payment.cardLastFour}
                            </strong>
                        </p>
                    )}

                    <p className="lab-receipt-total">
                        <span>Total pagado</span>

                        <strong>
                            {formatAmount(
                                payment.amount
                            )}
                        </strong>
                    </p>

                    {payment.paymentMethod ===
                        "EFECTIVO" && (
                            <>
                                <p>
                                    <span>
                                        Monto recibido
                                    </span>

                                    <strong>
                                        {formatAmount(
                                            payment.receivedAmount
                                        )}
                                    </strong>
                                </p>

                                <p>
                                    <span>
                                        Cambio entregado
                                    </span>

                                    <strong>
                                        {formatAmount(
                                            payment.changeAmount
                                        )}
                                    </strong>
                                </p>
                            </>
                        )}

                    <p>
                        <span>Fecha de pago</span>

                        <strong>
                            {formatDateTime(
                                payment.processedAt
                            )}
                        </strong>
                    </p>

                    <p>
                        <span>Cajero</span>

                        <strong>
                            {payment.cashierUsername}
                        </strong>
                    </p>
                </div>
            </div>

            <div className="lab-receipt-actions">
                <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={handlePrint}
                >
                    Imprimir comprobante
                </button>

                <button
                    type="button"
                    className="btn btn-primary"
                    onClick={onNewPayment}
                >
                    Nuevo cobro
                </button>
            </div>
        </section>
    );
}

/**
 * Oculta todos los dígitos del DPI excepto
 * los últimos cuatro.
 */
function maskDpi(dpi) {
    if (!dpi) {
        return "No registrado";
    }

    const cleanDpi =
        String(dpi).trim();

    if (cleanDpi.length <= 4) {
        return cleanDpi;
    }

    return (
        "*".repeat(cleanDpi.length - 4) +
        cleanDpi.slice(-4)
    );
}

export default LaboratoryPaymentReceipt;