import {
    useCallback,
    useEffect,
    useMemo,
    useState
} from "react";

import MainLayout from "../../layouts/MainLayout";

import {
    getActiveRoles
} from "../../api/roleApi";

import {
    getRolePermissions,
    updateRolePermissions
} from "../../api/permissionApi";

import {
    useAuth
} from "../../context/AuthContext";

import "../../styles/ui.css";

/**
 * Permisos que el rol ADMIN no puede perder.
 *
 * El backend aplica la misma protección.
 */
const REQUIRED_ADMIN_PERMISSIONS =
    new Set([
        "PERMISSION_READ",
        "ROLE_PERMISSION_UPDATE",
        "ROLE_READ",
        "ROLE_UPDATE"
    ]);

/**
 * Etiquetas visibles para cada módulo.
 */
const MODULE_LABELS = {
    APPOINTMENT_STATUS:
        "Estados de Cita",

    BRANCH:
        "Sucursales",

    BRANCH_SPECIALTY:
        "Especialidades por Sede",

    INVENTORY:
        "Inventario",

    LABORATORY_TEST:
        "Exámenes de Laboratorio",

    MEDICATION:
        "Medicamentos",

    ROLE:
        "Roles",

    SECURITY:
        "Seguridad",

    SPECIALTY:
        "Especialidades"
};

function RolePermissions() {
    const {
        user,
        hasPermission,
        refreshSession
    } = useAuth();

    /**
     * Permisos de la pantalla.
     */
    const canRead =
        hasPermission(
            "PERMISSION_READ"
        );

    const canUpdate =
        hasPermission(
            "ROLE_PERMISSION_UPDATE"
        );

    /**
     * Roles disponibles.
     */
    const [
        roles,
        setRoles
    ] = useState([]);

    const [
        selectedRoleId,
        setSelectedRoleId
    ] = useState("");

    /**
     * Respuesta completa del backend.
     */
    const [
        rolePermissionsData,
        setRolePermissionsData
    ] = useState(null);

    /**
     * IDs seleccionados actualmente.
     */
    const [
        selectedPermissionIds,
        setSelectedPermissionIds
    ] = useState(
        new Set()
    );

    /**
     * Módulos expandidos.
     */
    const [
        expandedModules,
        setExpandedModules
    ] = useState(
        new Set()
    );

    /**
     * Estados de carga.
     */
    const [
        loadingRoles,
        setLoadingRoles
    ] = useState(false);

    const [
        loadingPermissions,
        setLoadingPermissions
    ] = useState(false);

    const [
        saving,
        setSaving
    ] = useState(false);

    /**
     * Detecta si existen cambios no guardados.
     */
    const [
        initialPermissionIds,
        setInitialPermissionIds
    ] = useState(
        new Set()
    );

    /**
     * Cargar roles activos.
     */
    const loadRoles =
        useCallback(async () => {
            try {
                setLoadingRoles(true);

                const response =
                    await getActiveRoles();

                const roleList =
                    Array.isArray(
                        response.data
                    )
                        ? response.data
                        : [];

                setRoles(roleList);

            } catch (error) {
                console.error(
                    "Error al cargar roles:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible cargar los roles."
                );

            } finally {
                setLoadingRoles(false);
            }
        }, []);

    useEffect(() => {
        if (canRead) {
            loadRoles();
        }
    }, [
        canRead,
        loadRoles
    ]);

    /**
     * Carga los permisos asignados al rol.
     */
    const loadRolePermissions =
        useCallback(
            async roleId => {
                if (!roleId) {
                    setRolePermissionsData(
                        null
                    );

                    setSelectedPermissionIds(
                        new Set()
                    );

                    setInitialPermissionIds(
                        new Set()
                    );

                    return;
                }

                try {
                    setLoadingPermissions(
                        true
                    );

                    const response =
                        await getRolePermissions(
                            roleId
                        );

                    const responseData =
                        response.data;

                    const permissions =
                        Array.isArray(
                            responseData
                                ?.permissions
                        )
                            ? responseData
                                .permissions
                            : [];

                    const assignedIds =
                        new Set(
                            permissions
                                .filter(
                                    permission =>
                                        Boolean(
                                            permission
                                                .assigned
                                        )
                                )
                                .map(
                                    permission =>
                                        permission
                                            .permissionId
                                )
                        );

                    setRolePermissionsData(
                        responseData
                    );

                    setSelectedPermissionIds(
                        new Set(
                            assignedIds
                        )
                    );

                    setInitialPermissionIds(
                        new Set(
                            assignedIds
                        )
                    );

                    /**
                     * Expandir todos los módulos
                     * al cargar.
                     */
                    setExpandedModules(
                        new Set(
                            permissions.map(
                                permission =>
                                    permission.module
                            )
                        )
                    );

                } catch (error) {
                    console.error(
                        "Error al cargar permisos del rol:",
                        error
                    );

                    alert(
                        error.response
                            ?.data
                            ?.message ||
                        "No fue posible cargar los permisos del rol."
                    );

                    setRolePermissionsData(
                        null
                    );

                } finally {
                    setLoadingPermissions(
                        false
                    );
                }
            },
            []
        );

    /**
     * Cuando cambia el rol seleccionado,
     * consultar sus permisos.
     */
    useEffect(() => {
        if (
            selectedRoleId &&
            canRead
        ) {
            loadRolePermissions(
                Number(
                    selectedRoleId
                )
            );

        } else {
            setRolePermissionsData(
                null
            );

            setSelectedPermissionIds(
                new Set()
            );

            setInitialPermissionIds(
                new Set()
            );
        }
    }, [
        selectedRoleId,
        canRead,
        loadRolePermissions
    ]);

    /**
     * Permisos agrupados por módulo.
     */
    const permissionsByModule =
        useMemo(() => {
            const permissions =
                rolePermissionsData
                    ?.permissions;

            if (
                !Array.isArray(
                    permissions
                )
            ) {
                return {};
            }

            return permissions.reduce(
                (
                    groupedPermissions,
                    permission
                ) => {
                    const moduleName =
                        permission.module ||
                        "OTHER";

                    if (
                        !groupedPermissions[
                        moduleName
                        ]
                    ) {
                        groupedPermissions[
                            moduleName
                        ] = [];
                    }

                    groupedPermissions[
                        moduleName
                    ].push(
                        permission
                    );

                    return groupedPermissions;
                },
                {}
            );
        }, [
            rolePermissionsData
        ]);

    /**
     * Módulos ordenados alfabéticamente
     * por su etiqueta visible.
     */
    const orderedModules =
        useMemo(() => {
            return Object.keys(
                permissionsByModule
            ).sort(
                (
                    firstModule,
                    secondModule
                ) => {
                    const firstLabel =
                        MODULE_LABELS[
                        firstModule
                        ] ||
                        firstModule;

                    const secondLabel =
                        MODULE_LABELS[
                        secondModule
                        ] ||
                        secondModule;

                    return firstLabel
                        .localeCompare(
                            secondLabel,
                            "es"
                        );
                }
            );
        }, [
            permissionsByModule
        ]);

    /**
     * Determina si el rol seleccionado
     * es ADMIN.
     */
    const selectedRoleIsAdmin =
        normalizeRoleName(
            rolePermissionsData
                ?.roleName
        ) === "ADMIN";

    /**
     * Determina si un permiso está protegido
     * para el rol ADMIN.
     */
    const isProtectedAdminPermission =
        permission => {
            return (
                selectedRoleIsAdmin &&
                REQUIRED_ADMIN_PERMISSIONS
                    .has(
                        permission
                            .permissionCode
                    )
            );
        };

    /**
     * Marca o desmarca un permiso individual.
     */
    const togglePermission =
        permission => {
            if (!canUpdate) {
                return;
            }

            if (
                isProtectedAdminPermission(
                    permission
                )
            ) {
                alert(
                    `El permiso ${permission.permissionCode} es obligatorio para el rol ADMIN.`
                );

                return;
            }

            setSelectedPermissionIds(
                currentSelection => {
                    const nextSelection =
                        new Set(
                            currentSelection
                        );

                    if (
                        nextSelection.has(
                            permission
                                .permissionId
                        )
                    ) {
                        nextSelection.delete(
                            permission
                                .permissionId
                        );

                    } else {
                        nextSelection.add(
                            permission
                                .permissionId
                        );
                    }

                    return nextSelection;
                }
            );
        };

    /**
     * Selecciona o deselecciona todos
     * los permisos de un módulo.
     */
    const toggleModule =
        moduleName => {
            if (!canUpdate) {
                return;
            }

            const modulePermissions =
                permissionsByModule[
                moduleName
                ] || [];

            const selectablePermissions =
                modulePermissions.filter(
                    permission =>
                        !isProtectedAdminPermission(
                            permission
                        )
                );

            const allSelectableAssigned =
                selectablePermissions.length > 0 &&
                selectablePermissions.every(
                    permission =>
                        selectedPermissionIds
                            .has(
                                permission
                                    .permissionId
                            )
                );

            setSelectedPermissionIds(
                currentSelection => {
                    const nextSelection =
                        new Set(
                            currentSelection
                        );

                    if (
                        allSelectableAssigned
                    ) {
                        selectablePermissions
                            .forEach(
                                permission =>
                                    nextSelection
                                        .delete(
                                            permission
                                                .permissionId
                                        )
                            );

                    } else {
                        selectablePermissions
                            .forEach(
                                permission =>
                                    nextSelection
                                        .add(
                                            permission
                                                .permissionId
                                        )
                            );
                    }

                    /**
                     * Mantener los permisos obligatorios
                     * del rol ADMIN.
                     */
                    if (
                        selectedRoleIsAdmin
                    ) {
                        modulePermissions
                            .filter(
                                permission =>
                                    REQUIRED_ADMIN_PERMISSIONS
                                        .has(
                                            permission
                                                .permissionCode
                                        )
                            )
                            .forEach(
                                permission =>
                                    nextSelection
                                        .add(
                                            permission
                                                .permissionId
                                        )
                            );
                    }

                    return nextSelection;
                }
            );
        };

    /**
     * Expande o contrae un módulo.
     */
    const toggleModuleExpansion =
        moduleName => {
            setExpandedModules(
                currentModules => {
                    const nextModules =
                        new Set(
                            currentModules
                        );

                    if (
                        nextModules.has(
                            moduleName
                        )
                    ) {
                        nextModules.delete(
                            moduleName
                        );

                    } else {
                        nextModules.add(
                            moduleName
                        );
                    }

                    return nextModules;
                }
            );
        };

    /**
     * Seleccionar todos los permisos.
     */
    const selectAllPermissions = () => {
        if (
            !canUpdate ||
            !rolePermissionsData
        ) {
            return;
        }

        const allPermissionIds =
            new Set(
                rolePermissionsData
                    .permissions
                    .map(
                        permission =>
                            permission
                                .permissionId
                    )
            );

        setSelectedPermissionIds(
            allPermissionIds
        );
    };

    /**
     * Deseleccionar todos los permisos
     * no protegidos.
     */
    const clearAllPermissions = () => {
        if (
            !canUpdate ||
            !rolePermissionsData
        ) {
            return;
        }

        const requiredIds =
            new Set();

        if (selectedRoleIsAdmin) {
            rolePermissionsData
                .permissions
                .filter(
                    permission =>
                        REQUIRED_ADMIN_PERMISSIONS
                            .has(
                                permission
                                    .permissionCode
                            )
                )
                .forEach(
                    permission =>
                        requiredIds.add(
                            permission
                                .permissionId
                        )
                );
        }

        setSelectedPermissionIds(
            requiredIds
        );
    };

    /**
     * Restablece la selección sin guardar.
     */
    const resetSelection = () => {
        setSelectedPermissionIds(
            new Set(
                initialPermissionIds
            )
        );
    };

    /**
     * Detectar cambios.
     */
    const hasUnsavedChanges =
        useMemo(() => {
            if (
                selectedPermissionIds
                    .size !==
                initialPermissionIds
                    .size
            ) {
                return true;
            }

            for (
                const permissionId
                of selectedPermissionIds
            ) {
                if (
                    !initialPermissionIds
                        .has(permissionId)
                ) {
                    return true;
                }
            }

            return false;
        }, [
            selectedPermissionIds,
            initialPermissionIds
        ]);

    /**
     * Guardar asignaciones.
     */
    const handleSave =
        async () => {
            if (!canUpdate) {
                alert(
                    "No tiene permiso para modificar los permisos de los roles."
                );

                return;
            }

            if (!selectedRoleId) {
                alert(
                    "Debe seleccionar un rol."
                );

                return;
            }

            if (!hasUnsavedChanges) {
                alert(
                    "No existen cambios pendientes."
                );

                return;
            }

            const confirmed =
                window.confirm(
                    `¿Desea guardar ${selectedPermissionIds.size} permiso(s) para el rol "${rolePermissionsData?.roleName}"?`
                );

            if (!confirmed) {
                return;
            }

            try {
                setSaving(true);

                const permissionIds =
                    Array.from(
                        selectedPermissionIds
                    ).sort(
                        (
                            firstId,
                            secondId
                        ) =>
                            firstId -
                            secondId
                    );

                const response =
                    await updateRolePermissions(
                        Number(
                            selectedRoleId
                        ),
                        permissionIds
                    );

                alert(
                    response.data
                        ?.message ||
                    "Permisos del rol actualizados correctamente."
                );

                await loadRolePermissions(
                    Number(
                        selectedRoleId
                    )
                );

                /**
                 * Si se modificó el rol del usuario
                 * conectado, actualizar inmediatamente
                 * el menú y las rutas de React.
                 */
                if (
                    Number(
                        user?.roleId
                    ) ===
                    Number(
                        selectedRoleId
                    )
                ) {
                    await refreshSession();
                }

            } catch (error) {
                console.error(
                    "Error al actualizar permisos:",
                    error
                );

                alert(
                    error.response
                        ?.data
                        ?.message ||
                    "No fue posible actualizar los permisos del rol."
                );

                await loadRolePermissions(
                    Number(
                        selectedRoleId
                    )
                );

            } finally {
                setSaving(false);
            }
        };

    /**
     * Controla el cambio del rol seleccionado.
     */
    const handleRoleChange =
        event => {
            const newRoleId =
                event.target.value;

            if (
                hasUnsavedChanges
            ) {
                const confirmed =
                    window.confirm(
                        "Existen cambios sin guardar. ¿Desea descartarlos y cambiar de rol?"
                    );

                if (!confirmed) {
                    return;
                }
            }

            setSelectedRoleId(
                newRoleId
            );

            setRolePermissionsData(
                null
            );
        };

    if (!canRead) {
        return (
            <MainLayout>
                <h1 className="page-title">
                    Permisos por Rol
                </h1>

                <div className="card">
                    <p>
                        No tiene permiso para consultar la configuración de permisos.
                    </p>
                </div>
            </MainLayout>
        );
    }

    return (
        <MainLayout>
            <h1 className="page-title">
                Permisos por Rol
            </h1>

            <div className="card">
                <h2 className="section-title">
                    Seleccionar rol
                </h2>

                <div className="form-grid">
                    <div className="form-group">
                        <label>
                            Rol
                        </label>

                        <select
                            value={
                                selectedRoleId
                            }
                            onChange={
                                handleRoleChange
                            }
                            disabled={
                                loadingRoles ||
                                saving
                            }
                        >
                            <option value="">
                                Seleccione un rol
                            </option>

                            {roles.map(
                                role => (
                                    <option
                                        key={
                                            role.id
                                        }
                                        value={
                                            role.id
                                        }
                                    >
                                        {role.name}
                                    </option>
                                )
                            )}
                        </select>

                        {loadingRoles && (
                            <small>
                                Cargando roles...
                            </small>
                        )}
                    </div>
                </div>
            </div>

            <br />

            {loadingPermissions && (
                <div className="card">
                    <p>
                        Cargando permisos del rol...
                    </p>
                </div>
            )}

            {!loadingPermissions &&
                !rolePermissionsData &&
                selectedRoleId && (
                    <div className="card">
                        <p>
                            No fue posible cargar la información del rol.
                        </p>
                    </div>
                )}

            {!loadingPermissions &&
                rolePermissionsData && (
                    <>
                        <div className="card">
                            <h2 className="section-title">
                                Información del rol
                            </h2>

                            <div className="form-grid">
                                <div className="form-group">
                                    <label>
                                        Nombre
                                    </label>

                                    <input
                                        type="text"
                                        value={
                                            rolePermissionsData
                                                .roleName ||
                                            ""
                                        }
                                        disabled
                                    />
                                </div>

                                <div className="form-group">
                                    <label>
                                        Descripción
                                    </label>

                                    <input
                                        type="text"
                                        value={
                                            rolePermissionsData
                                                .roleDescription ||
                                            ""
                                        }
                                        disabled
                                    />
                                </div>

                                <div className="form-group">
                                    <label>
                                        Tipo
                                    </label>

                                    <input
                                        type="text"
                                        value={
                                            rolePermissionsData
                                                .systemRole
                                                ? "Rol técnico del sistema"
                                                : "Rol personalizado"
                                        }
                                        disabled
                                    />
                                </div>

                                <div className="form-group">
                                    <label>
                                        Permisos seleccionados
                                    </label>

                                    <input
                                        type="text"
                                        value={
                                            `${selectedPermissionIds.size} de ${rolePermissionsData.availablePermissionCount || 0}`
                                        }
                                        disabled
                                    />
                                </div>
                            </div>

                            {selectedRoleIsAdmin && (
                                <div
                                    style={{
                                        marginTop:
                                            "12px",
                                        padding:
                                            "12px",
                                        borderRadius:
                                            "6px",
                                        backgroundColor:
                                            "#fff7ed",
                                        border:
                                            "1px solid #fdba74",
                                        color:
                                            "#9a3412"
                                    }}
                                >
                                    El rol ADMIN debe conservar los permisos de seguridad necesarios para administrar roles y permisos.
                                </div>
                            )}

                            {hasUnsavedChanges && (
                                <div
                                    style={{
                                        marginTop:
                                            "12px",
                                        padding:
                                            "12px",
                                        borderRadius:
                                            "6px",
                                        backgroundColor:
                                            "#fefce8",
                                        border:
                                            "1px solid #fde047",
                                        color:
                                            "#854d0e"
                                    }}
                                >
                                    Existen cambios pendientes de guardar.
                                </div>
                            )}
                        </div>

                        <br />

                        <div className="table-card">
                            <div
                                style={{
                                    display:
                                        "flex",
                                    alignItems:
                                        "center",
                                    justifyContent:
                                        "space-between",
                                    gap:
                                        "12px",
                                    flexWrap:
                                        "wrap"
                                }}
                            >
                                <h2 className="section-title">
                                    Permisos disponibles
                                </h2>

                                {canUpdate && (
                                    <div className="button-row">
                                        <button
                                            type="button"
                                            className="btn btn-secondary"
                                            onClick={
                                                selectAllPermissions
                                            }
                                            disabled={
                                                saving
                                            }
                                        >
                                            Seleccionar todos
                                        </button>

                                        <button
                                            type="button"
                                            className="btn btn-secondary"
                                            onClick={
                                                clearAllPermissions
                                            }
                                            disabled={
                                                saving
                                            }
                                        >
                                            Limpiar selección
                                        </button>
                                    </div>
                                )}
                            </div>

                            {orderedModules.map(
                                moduleName => {
                                    const modulePermissions =
                                        permissionsByModule[
                                        moduleName
                                        ] || [];

                                    const moduleExpanded =
                                        expandedModules
                                            .has(
                                                moduleName
                                            );

                                    const selectedCount =
                                        modulePermissions
                                            .filter(
                                                permission =>
                                                    selectedPermissionIds
                                                        .has(
                                                            permission
                                                                .permissionId
                                                        )
                                            )
                                            .length;

                                    const allSelected =
                                        modulePermissions
                                            .length > 0 &&
                                        modulePermissions
                                            .every(
                                                permission =>
                                                    selectedPermissionIds
                                                        .has(
                                                            permission
                                                                .permissionId
                                                        )
                                            );

                                    return (
                                        <div
                                            key={
                                                moduleName
                                            }
                                            style={{
                                                marginBottom:
                                                    "16px",
                                                border:
                                                    "1px solid #e2e8f0",
                                                borderRadius:
                                                    "8px",
                                                overflow:
                                                    "hidden"
                                            }}
                                        >
                                            <div
                                                style={{
                                                    padding:
                                                        "14px",
                                                    display:
                                                        "flex",
                                                    alignItems:
                                                        "center",
                                                    justifyContent:
                                                        "space-between",
                                                    gap:
                                                        "12px",
                                                    flexWrap:
                                                        "wrap",
                                                    backgroundColor:
                                                        "#f8fafc"
                                                }}
                                            >
                                                <button
                                                    type="button"
                                                    onClick={() =>
                                                        toggleModuleExpansion(
                                                            moduleName
                                                        )
                                                    }
                                                    style={{
                                                        border:
                                                            "none",
                                                        background:
                                                            "transparent",
                                                        cursor:
                                                            "pointer",
                                                        fontWeight:
                                                            700,
                                                        fontSize:
                                                            "1rem",
                                                        color:
                                                            "#1e293b"
                                                    }}
                                                >
                                                    {moduleExpanded
                                                        ? "▼"
                                                        : "▶"}
                                                    {" "}
                                                    {MODULE_LABELS[
                                                        moduleName
                                                    ] ||
                                                        moduleName}

                                                    {" "}

                                                    ({selectedCount}/{
                                                        modulePermissions.length
                                                    })
                                                </button>

                                                {canUpdate && (
                                                    <label
                                                        style={{
                                                            display:
                                                                "flex",
                                                            alignItems:
                                                                "center",
                                                            gap:
                                                                "8px",
                                                            cursor:
                                                                "pointer"
                                                        }}
                                                    >
                                                        <input
                                                            type="checkbox"
                                                            checked={
                                                                allSelected
                                                            }
                                                            onChange={() =>
                                                                toggleModule(
                                                                    moduleName
                                                                )
                                                            }
                                                            disabled={
                                                                saving
                                                            }
                                                        />

                                                        Seleccionar módulo
                                                    </label>
                                                )}
                                            </div>

                                            {moduleExpanded && (
                                                <div
                                                    style={{
                                                        padding:
                                                            "14px"
                                                    }}
                                                >
                                                    {modulePermissions.map(
                                                        permission => {
                                                            const selected =
                                                                selectedPermissionIds
                                                                    .has(
                                                                        permission
                                                                            .permissionId
                                                                    );

                                                            const protectedPermission =
                                                                isProtectedAdminPermission(
                                                                    permission
                                                                );

                                                            return (
                                                                <label
                                                                    key={
                                                                        permission
                                                                            .permissionId
                                                                    }
                                                                    style={{
                                                                        display:
                                                                            "grid",
                                                                        gridTemplateColumns:
                                                                            "24px minmax(180px, 260px) 1fr",
                                                                        gap:
                                                                            "10px",
                                                                        alignItems:
                                                                            "start",
                                                                        padding:
                                                                            "12px 4px",
                                                                        borderBottom:
                                                                            "1px solid #e2e8f0",
                                                                        cursor:
                                                                            canUpdate &&
                                                                                !protectedPermission
                                                                                ? "pointer"
                                                                                : "default"
                                                                    }}
                                                                >
                                                                    <input
                                                                        type="checkbox"
                                                                        checked={
                                                                            selected
                                                                        }
                                                                        disabled={
                                                                            !canUpdate ||
                                                                            saving ||
                                                                            protectedPermission
                                                                        }
                                                                        onChange={() =>
                                                                            togglePermission(
                                                                                permission
                                                                            )
                                                                        }
                                                                    />

                                                                    <div>
                                                                        <strong>
                                                                            {
                                                                                permission
                                                                                    .permissionName
                                                                            }
                                                                        </strong>

                                                                        <div
                                                                            style={{
                                                                                marginTop:
                                                                                    "4px",
                                                                                color:
                                                                                    "#64748b",
                                                                                fontSize:
                                                                                    "0.82rem",
                                                                                fontFamily:
                                                                                    "monospace"
                                                                            }}
                                                                        >
                                                                            {
                                                                                permission
                                                                                    .permissionCode
                                                                            }
                                                                        </div>
                                                                    </div>

                                                                    <div
                                                                        style={{
                                                                            color:
                                                                                "#475569"
                                                                        }}
                                                                    >
                                                                        {
                                                                            permission
                                                                                .description
                                                                        }

                                                                        {protectedPermission && (
                                                                            <div
                                                                                style={{
                                                                                    marginTop:
                                                                                        "5px",
                                                                                    color:
                                                                                        "#b45309",
                                                                                    fontSize:
                                                                                        "0.82rem",
                                                                                    fontWeight:
                                                                                        600
                                                                                }}
                                                                            >
                                                                                Obligatorio para ADMIN
                                                                            </div>
                                                                        )}
                                                                    </div>
                                                                </label>
                                                            );
                                                        }
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    );
                                }
                            )}

                            {canUpdate && (
                                <div className="button-row">
                                    <button
                                        type="button"
                                        className="btn btn-primary"
                                        onClick={
                                            handleSave
                                        }
                                        disabled={
                                            saving ||
                                            !hasUnsavedChanges
                                        }
                                    >
                                        {saving
                                            ? "Guardando..."
                                            : "Guardar permisos"}
                                    </button>

                                    <button
                                        type="button"
                                        className="btn btn-secondary"
                                        onClick={
                                            resetSelection
                                        }
                                        disabled={
                                            saving ||
                                            !hasUnsavedChanges
                                        }
                                    >
                                        Descartar cambios
                                    </button>
                                </div>
                            )}
                        </div>
                    </>
                )}
        </MainLayout>
    );
}

/**
 * Normaliza el nombre de un rol.
 */
function normalizeRoleName(value) {
    if (!value) {
        return "";
    }

    return String(value)
        .normalize("NFD")
        .replace(
            /[\u0300-\u036f]/g,
            ""
        )
        .trim()
        .replace(
            /\s+/g,
            "_"
        )
        .toUpperCase();
}

export default RolePermissions;