import { useNavigate } from "react-router-dom";
import MainLayout from "../../layouts/MainLayout";
import "./PaymentConfirmation.css";

function PaymentConfirmation() {
    const navigate = useNavigate();

    const storedPayment =
        sessionStorage.getItem(
            "paymentConfirmation"
        );

    const payment =
        storedPayment
            ? JSON.parse(storedPayment)
            : null;

    if (!payment) {
        return (
            <MainLayout>
                <div className="confirmation-card">
                    <h1>
                        No hay un comprobante disponible
                    </h1>

                    <button
                        type="button"
                        onClick={() =>
                            navigate("/dashboard")
                        }
                    >
                        Volver al Portal
                    </button>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <div className="confirmation-card">
                <div className="confirmation-icon">
                    ✓
                </div>

                <h1>¡Pago Exitoso!</h1>

                <p className="confirmation-message">
                    {payment.message}
                </p>

                <div className="confirmation-details">
                    <p>
                        <span>Transacción:</span>
                        <strong>
                            {payment.transactionNumber}
                        </strong>
                    </p>

                    <p>
                        <span>Médico:</span>
                        <strong>
                            {payment.doctorName}
                        </strong>
                    </p>

                    <p>
                        <span>Especialidad:</span>
                        <strong>
                            {payment.specialty}
                        </strong>
                    </p>

                    <p>
                        <span>Sucursal:</span>
                        <strong>
                            {payment.branch}
                        </strong>
                    </p>

                    <p>
                        <span>Fecha y hora:</span>
                        <strong>
                            {payment.appointmentDateTime}
                        </strong>
                    </p>

                    <p>
                        <span>Monto:</span>
                        <strong>
                            Q{Number(payment.amount).toFixed(2)}
                        </strong>
                    </p>

                    <p>
                        <span>Tarjeta:</span>
                        <strong>
                            **** {payment.cardLastFour}
                        </strong>
                    </p>
                </div>

                <div className="confirmation-notice">
                    Se ha generado el comprobante del pago.
                    El envío de correo será integrado en el
                    módulo de notificaciones.
                </div>

                <div className="confirmation-actions">
                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={() => {
                            sessionStorage.removeItem(
                                "paymentConfirmation"
                            );

                            navigate("/dashboard");
                        }}
                    >
                        Volver al Portal
                    </button>

                    <button
                        type="button"
                        className="btn btn-primary"
                        onClick={() => {
                            /*
                             * Crearemos esta página cuando
                             * implementemos Mis Citas.
                             */
                            navigate("/appointments");
                        }}
                    >
                        Ver Mis Citas
                    </button>
                </div>
            </div>
        </MainLayout>
    );
}

export default PaymentConfirmation;