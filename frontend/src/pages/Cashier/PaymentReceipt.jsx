import "./PaymentReceipt.css";

function PaymentReceipt({
    payment,
    onNewPayment
}) {
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

    const formatAmount = (amount) => {
        return `Q${Number(
            amount || 0
        ).toFixed(2)}`;
    };

    const handlePrint = () => {
        window.print();
    };

    return (
        <section className="cashier-receipt">
            <div className="receipt-success-icon">
                ✓
            </div>

            <h2>
                Pago registrado exitosamente
            </h2>

            <p className="receipt-message">
                {payment.message}
            </p>

            <div className="receipt-content">
                <div className="receipt-hospital">
                    <h3>Hospital HIS</h3>

                    <p>Comprobante de pago</p>
                </div>

                <div className="receipt-details">
                    <p>
                        <span>No. transacción</span>

                        <strong>
                            {payment.transactionNumber}
                        </strong>
                    </p>

                    <p>
                        <span>No. cita</span>

                        <strong>
                            {payment.appointmentId}
                        </strong>
                    </p>

                    <p>
                        <span>Médico</span>

                        <strong>
                            {payment.doctorName}
                        </strong>
                    </p>

                    <p>
                        <span>Especialidad</span>

                        <strong>
                            {payment.specialty}
                        </strong>
                    </p>

                    <p>
                        <span>Sucursal</span>

                        <strong>
                            {payment.branch}
                        </strong>
                    </p>

                    <p>
                        <span>Fecha de cita</span>

                        <strong>
                            {formatDateTime(
                                payment.appointmentDateTime
                            )}
                        </strong>
                    </p>

                    <p>
                        <span>Método de pago</span>

                        <strong>
                            {payment.paymentMethod}
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

                    <p>
                        <span>Total pagado</span>

                        <strong>
                            {formatAmount(payment.amount)}
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

            <div className="receipt-actions">
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
                    Nuevo Cobro
                </button>
            </div>
        </section>
    );
}

export default PaymentReceipt;