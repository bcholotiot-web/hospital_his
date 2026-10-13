/**
 * Presenta visualmente el estado de una cita.
 *
 * El código técnico continúa disponible en:
 *
 * status = "PENDIENTE_DE_PAGO"
 *
 * La presentación visual puede venir del backend:
 *
 * presentation.displayName
 * presentation.description
 * presentation.color
 * presentation.configured
 */
function AppointmentStatusBadge({
    status,
    presentation,
    showDescription = false
}) {
    /**
     * El backend devuelve una presentación
     * predeterminada cuando la configuración:
     *
     * - No existe.
     * - Está inactiva.
     * - No es visible para el contexto actual.
     *
     * Aun así, mantenemos valores de respaldo
     * en React para respuestas antiguas.
     */
    const displayName =
        presentation?.displayName ||
        formatTechnicalStatus(
            status
        );

    const description =
        presentation?.description ||
        "";

    const color =
        isValidHexColor(
            presentation?.color
        )
            ? presentation.color
                .toUpperCase()
            : "#64748B";

    const backgroundColor =
        hexToRgba(
            color,
            0.12
        );

    return (
        <div
            style={{
                display:
                    "inline-flex",
                flexDirection:
                    "column",
                alignItems:
                    "flex-start",
                gap:
                    showDescription &&
                        description
                        ? "6px"
                        : "0"
            }}
        >
            <span
                title={
                    description ||
                    displayName
                }
                style={{
                    display:
                        "inline-flex",
                    alignItems:
                        "center",
                    gap:
                        "7px",
                    padding:
                        "5px 10px",
                    borderRadius:
                        "999px",
                    backgroundColor,
                    border:
                        `1px solid ${color}`,
                    color,
                    fontWeight:
                        700,
                    fontSize:
                        "0.82rem",
                    whiteSpace:
                        "nowrap"
                }}
            >
                <span
                    aria-hidden="true"
                    style={{
                        width:
                            "8px",
                        height:
                            "8px",
                        borderRadius:
                            "50%",
                        backgroundColor:
                            color,
                        flexShrink:
                            0
                    }}
                />

                {displayName}
            </span>

            {showDescription &&
                description && (
                    <small
                        style={{
                            maxWidth:
                                "360px",
                            color:
                                "#64748b",
                            lineHeight:
                                1.4
                        }}
                    >
                        {description}
                    </small>
                )}
        </div>
    );
}

/**
 * Convierte un código técnico en texto visible.
 *
 * PENDIENTE_DE_PAGO
 * ->
 * Pendiente de pago
 */
function formatTechnicalStatus(value) {
    if (!value) {
        return "Sin estado";
    }

    const formattedText =
        String(value)
            .trim()
            .toLowerCase()
            .replace(
                /_/g,
                " "
            );

    return (
        formattedText
            .charAt(0)
            .toUpperCase() +
        formattedText.slice(1)
    );
}

/**
 * Comprueba el formato hexadecimal #RRGGBB.
 */
function isValidHexColor(value) {
    return /^#[0-9A-Fa-f]{6}$/
        .test(value || "");
}

/**
 * Convierte un color hexadecimal a rgba.
 *
 * #F59E0B
 * ->
 * rgba(245, 158, 11, 0.12)
 */
function hexToRgba(
    hexColor,
    alpha
) {
    if (
        !isValidHexColor(
            hexColor
        )
    ) {
        return "rgba(100, 116, 139, 0.12)";
    }

    const red =
        parseInt(
            hexColor.slice(
                1,
                3
            ),
            16
        );

    const green =
        parseInt(
            hexColor.slice(
                3,
                5
            ),
            16
        );

    const blue =
        parseInt(
            hexColor.slice(
                5,
                7
            ),
            16
        );

    return `rgba(${red}, ${green}, ${blue}, ${alpha})`;
}

export default AppointmentStatusBadge;