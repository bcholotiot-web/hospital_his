import "./ConfirmModal.css";

function ConfirmModal({
    title,
    message,
    onConfirm,
    onCancel
}) {
    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <h2>{title}</h2>

                <p>{message}</p>

                <div className="modal-actions">
                    <button
                        className="btn-cancel"
                        onClick={onCancel}
                    >
                        Cancelar
                    </button>

                    <button
                        className="btn-confirm-delete"
                        onClick={onConfirm}
                    >
                        Eliminar
                    </button>
                </div>
            </div>
        </div>
    );
}

export default ConfirmModal;