import axios from "axios";

const API_URL =
    "http://localhost:8080/api/pharmacy/inventory-movements";

/**
 * Construye la configuración del JWT.
 */
const getAuthConfig = () => {
    const token =
        localStorage.getItem("token");

    return {
        headers: {
            Authorization:
                `Bearer ${token}`
        }
    };
};

/**
 * Consulta la bitácora paginada.
 *
 * FARMACIA:
 * branchId se omite y el backend utiliza
 * la sucursal asignada.
 *
 * ADMIN:
 * debe enviar branchId.
 */
export const getInventoryMovements = ({
    branchId = "",
    search = "",
    movementType = "",
    referenceNumber = "",
    createdBy = "",
    active = "",
    startDate = "",
    endDate = "",
    page = 0,
    size = 20
} = {}) => {
    const params = {
        page,
        size
    };

    if (branchId !== "") {
        params.branchId =
            Number(branchId);
    }

    if (search.trim()) {
        params.search =
            search.trim();
    }

    if (movementType) {
        params.movementType =
            movementType;
    }

    if (referenceNumber.trim()) {
        params.referenceNumber =
            referenceNumber.trim();
    }

    if (createdBy.trim()) {
        params.createdBy =
            createdBy.trim();
    }

    if (
        active !== "" &&
        active !== null &&
        active !== undefined
    ) {
        params.active =
            active;
    }

    if (startDate) {
        params.startDate =
            startDate;
    }

    if (endDate) {
        params.endDate =
            endDate;
    }

    return axios.get(
        API_URL,
        {
            ...getAuthConfig(),
            params
        }
    );
};

/**
 * Consulta el resumen mensual.
 *
 * ADMIN debe enviar branchId.
 * FARMACIA lo omite.
 */
export const getInventoryMonthlySummary = ({
    branchId = "",
    year,
    month
}) => {
    const params = {
        year,
        month
    };

    if (branchId !== "") {
        params.branchId =
            Number(branchId);
    }

    return axios.get(
        `${API_URL}/monthly-summary`,
        {
            ...getAuthConfig(),
            params
        }
    );
};

/**
 * Calcula el efecto proyectado de un
 * movimiento sin guardar cambios.
 */
export const previewInventoryMovement = ({
    branchId = "",
    medicationId,
    movementType,
    quantity,
    unitCost = ""
}) => {
    const params = {
        medicationId:
            Number(medicationId),
        movementType,
        quantity:
            Number(quantity)
    };

    if (branchId !== "") {
        params.branchId =
            Number(branchId);
    }

    if (
        unitCost !== "" &&
        unitCost !== null &&
        unitCost !== undefined
    ) {
        params.unitCost =
            Number(unitCost);
    }

    return axios.get(
        `${API_URL}/preview`,
        {
            ...getAuthConfig(),
            params
        }
    );
};

/**
 * Registra un movimiento manual.
 */
export const createInventoryMovement =
    data => {
        const payload = {
            medicationId:
                Number(
                    data.medicationId
                ),
            movementType:
                data.movementType,
            quantity:
                Number(
                    data.quantity
                ),
            unitCost:
                data.unitCost !== "" &&
                    data.unitCost !== null &&
                    data.unitCost !== undefined
                    ? Number(
                        data.unitCost
                    )
                    : null,
            referenceNumber:
                data.referenceNumber
                    ?.trim() ||
                null,
            reason:
                data.reason
                    ?.trim() ||
                null,
            inventoryVersion:
                Number(
                    data.inventoryVersion
                )
        };

        if (
            data.branchId !== "" &&
            data.branchId !== null &&
            data.branchId !== undefined
        ) {
            payload.branchId =
                Number(
                    data.branchId
                );
        }

        return axios.post(
            API_URL,
            payload,
            getAuthConfig()
        );
    };

/**
 * Obtiene el detalle de un movimiento.
 */
export const getInventoryMovementDetail =
    movementId => {
        return axios.get(
            `${API_URL}/${movementId}`,
            getAuthConfig()
        );
    };

/**
 * Activa o desactiva administrativamente
 * un movimiento.
 *
 * No modifica el stock.
 */
export const changeInventoryMovementStatus = (
    movementId,
    active,
    version
) => {
    return axios.patch(
        `${API_URL}/${movementId}/status`,
        null,
        {
            ...getAuthConfig(),
            params: {
                active,
                version
            }
        }
    );
};