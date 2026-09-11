import { useEffect, useState } from "react";

import { updateUser } from "../../api/userApi";
import { getRoles } from "../../api/roleApi";
import { getBranches } from "../../api/branchApi";
import { getSpecialties } from "../../api/specialtyApi";

import "./Users.css";
import "../../styles/forms.css";
import "../../styles/ui.css";

function UserEdit({ user, onUpdated, onCancel }) {
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [username, setUsername] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [dpi, setDpi] = useState("");
    const [phone, setPhone] = useState("");
    const [nit, setNit] = useState("");
    const [insuranceNumber, setInsuranceNumber] = useState("");
    const [roleId, setRoleId] = useState("");
    const [branchId, setBranchId] = useState("");
    const [specialtyId, setSpecialtyId] = useState("");
    const [active, setActive] = useState(true);

    const [roles, setRoles] = useState([]);
    const [branches, setBranches] = useState([]);
    const [specialties, setSpecialties] = useState([]);

    const [errors, setErrors] = useState({});
    const [loadingCatalogs, setLoadingCatalogs] = useState(true);
    const [saving, setSaving] = useState(false);

    /*
     * Cargar los catálogos necesarios para el formulario.
     */
    useEffect(() => {
        loadCatalogs();
    }, []);

    /*
     * Cargar los datos del usuario seleccionado.
    por seguridad la contraseña no se carga, solo se permite cambiarla.
     */
    useEffect(() => {
        if (!user) {
            return;
        }

        setFullName(user.fullName || "");
        setEmail(user.email || "");
        setUsername(user.username || "");
        setNewPassword("");
        setDpi(user.dpi || "");
        setPhone(user.phone || "");
        setNit(user.nit || "");
        setInsuranceNumber(user.insuranceNumber || "");
        setActive(user.active ?? true);

        setRoleId(
            user.roleId !== null && user.roleId !== undefined
                ? String(user.roleId)
                : ""
        );

        setBranchId(
            user.branchId !== null && user.branchId !== undefined
                ? String(user.branchId)
                : ""
        );

        setSpecialtyId(
            user.specialtyId !== null &&
                user.specialtyId !== undefined
                ? String(user.specialtyId)
                : ""
        );

        setErrors({});
    }, [user]);

    /*
     * Obtiene roles, sucursales y especialidades.
     */
    const loadCatalogs = async () => {
        setLoadingCatalogs(true);

        try {
            const [
                rolesResponse,
                branchesResponse,
                specialtiesResponse
            ] = await Promise.all([
                getRoles(),
                getBranches(),
                getSpecialties()
            ]);

            setRoles(rolesResponse.data || []);
            setBranches(branchesResponse.data || []);
            setSpecialties(specialtiesResponse.data || []);
        } catch (error) {
            console.error(
                "Error al cargar los catálogos:",
                error
            );

            alert(
                error.response?.data?.message ||
                "No fue posible cargar los catálogos del formulario."
            );
        } finally {
            setLoadingCatalogs(false);
        }
    };

    /*
     * Buscar el rol seleccionado para determinar si corresponde
     * al rol Médico.
     */
    /*
 * Normaliza un texto:
 *
 * MÉDICO → MEDICO
 * Médico → MEDICO
 * Medico → MEDICO
 * medico → MEDICO
 */
    const normalizeText = (value) => {
        return String(value || "")
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .trim()
            .toUpperCase();
    };

    const selectedRole = roles.find(
        role => role.id === Number(roleId)
    );

    const isDoctorRole =
        normalizeText(selectedRole?.name) === "MEDICO";

    /*
     * Elimina el error de un campo cuando el usuario comienza
     * a corregirlo.
     */
    const clearFieldError = fieldName => {
        setErrors(currentErrors => {
            if (!currentErrors[fieldName]) {
                return currentErrors;
            }

            const updatedErrors = {
                ...currentErrors
            };

            delete updatedErrors[fieldName];

            return updatedErrors;
        });
    };

    /*
     * Validaciones del formulario.
     */
    const validateForm = () => {
        const newErrors = {};

        /*
         * Validar nombre completo.
         */
        if (!fullName.trim()) {
            newErrors.fullName =
                "El campo Nombre es obligatorio.";
        } else if (
            fullName.trim().length < 10 ||
            fullName.trim().length > 100
        ) {
            newErrors.fullName =
                `El nombre debe contener entre 10 y 100 caracteres. ` +
                `Usted ingresó ${fullName.trim().length} caracteres.`;
        }

        /*
         * Validar nombre de usuario.
         */
        if (!username.trim()) {
            newErrors.username =
                "El campo Usuario es obligatorio.";
        } else if (username.trim().length < 8) {
            newErrors.username =
                "El usuario debe contener al menos 8 caracteres.";
        } else if (username.trim().length > 9) {
            newErrors.username =
                "El usuario no puede exceder los 9 caracteres.";
        } else if (!/^[a-zA-Z0-9]+$/.test(username.trim())) {
            newErrors.username =
                "El usuario debe contener únicamente caracteres alfanuméricos.";
        }

        //el cambio de contraseña es opcional 

        if (newPassword && newPassword.length < 12) {
            newErrors.newPassword = "La nueva contraseña debe contener al menos 12 caracteres.";
        }
        /*
         * Validar DPI únicamente si fue ingresado.
         */
        if (dpi && !/^\d{13}$/.test(dpi.trim())) {
            newErrors.dpi =
                "El DPI debe contener exactamente 13 dígitos numéricos.";
        }

        /*
         * Validar teléfono únicamente si fue ingresado.
         */
        if (phone && !/^\d{8}$/.test(phone.trim())) {
            newErrors.phone =
                "El teléfono debe contener exactamente 8 dígitos.";
        }

        /*
         * Validar NIT únicamente si fue ingresado.
         */
        if (nit) {
            const normalizedNit = nit.trim();

            if (
                normalizedNit.length < 8 ||
                normalizedNit.length > 9
            ) {
                newErrors.nit =
                    `El NIT debe contener entre 8 y 9 caracteres. ` +
                    `Usted ingresó ${normalizedNit.length} caracteres.`;
            } else if (
                !/^[a-zA-Z0-9]+$/.test(normalizedNit)
            ) {
                newErrors.nit =
                    "El NIT debe contener únicamente caracteres alfanuméricos.";
            }
        }

        /*
         * Validar número de seguro únicamente si fue ingresado.
         */
        if (insuranceNumber) {
            const normalizedInsuranceNumber =
                insuranceNumber.trim();

            if (
                normalizedInsuranceNumber.length < 5 ||
                normalizedInsuranceNumber.length > 50
            ) {
                newErrors.insuranceNumber =
                    "El número de seguro debe contener entre 5 y 50 caracteres.";
            }
        }

        /*
         * Validar rol.
         */
        if (!roleId) {
            newErrors.roleId =
                "Debe seleccionar un rol para el usuario.";
        }

        /*
         * Validar sucursal.
         *
         * Esta validación coincide con UserCreate.
         * Si en edición la sucursal puede ser opcional,
         * puedes retirar este bloque.
         */
        if (!branchId) {
            newErrors.branchId =
                "Debe seleccionar una sucursal para el usuario.";
        }

        /*
         * La especialidad solamente es obligatoria cuando
         * el rol seleccionado es Médico.
         */
        if (isDoctorRole && !specialtyId) {
            newErrors.specialtyId =
                "Debe seleccionar una especialidad para el médico.";
        }

        /*
         * Validar estado.
         */
        if (active === null || active === undefined) {
            newErrors.active =
                "Debe seleccionar un estado para el usuario.";
        }

        setErrors(newErrors);

        return Object.keys(newErrors).length === 0;
    };

    /*
     * Procesar actualización del usuario.
     */
    const handleSubmit = async event => {
        event.preventDefault();

        if (!validateForm()) {
            return;
        }

        if (!user?.id) {
            alert(
                "No se encontró el identificador del usuario que se desea actualizar."
            );
            return;
        }

        setSaving(true);

        try {
            const request = {
                fullName: fullName.trim(),
                dpi: dpi.trim(),
                nit: nit.trim(),
                phone: phone.trim(),
                email: email.trim(),
                username: username.trim(),
                newPassword: newPassword.length > 0 ? newPassword : null,
                roleId: Number(roleId),
                insuranceNumber: insuranceNumber.trim(),
                branchId: branchId
                    ? Number(branchId)
                    : null,
                specialtyId:
                    isDoctorRole && specialtyId
                        ? Number(specialtyId)
                        : null,
                active
            };

            await updateUser(user.id, request);

            alert("Usuario actualizado correctamente.");

            setErrors({});

            if (onUpdated) {
                onUpdated();
            }
        } catch (error) {
            console.error(
                "Error al actualizar el usuario:",
                error
            );

            alert(
                error.response?.data?.message ||
                "Error al actualizar usuario."
            );
        } finally {
            setSaving(false);
        }
    };

    /*
     * Evita mostrar el formulario si todavía no existe
     * un usuario seleccionado.
     */
    if (!user) {
        return (
            <div className="card">
                <h2 className="section-title">
                    Editar Usuario
                </h2>

                <div className="error-message">
                    No se seleccionó un usuario para editar.
                </div>

                <div className="button-row">
                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={onCancel}
                    >
                        Regresar
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="card">
            <h2 className="section-title">
                Editar Usuario
            </h2>

            <form onSubmit={handleSubmit}>
                <div className="form-grid">
                    {/* Nombre completo */}
                    <div className="form-group">
                        <label htmlFor="edit-full-name">
                            Nombre completo
                        </label>

                        <input
                            id="edit-full-name"
                            type="text"
                            placeholder="Nombre completo"
                            value={fullName}
                            className={
                                errors.fullName
                                    ? "input-error"
                                    : ""
                            }
                            onChange={event => {
                                setFullName(event.target.value);
                                clearFieldError("fullName");
                            }}
                            disabled={saving}
                        />

                        {errors.fullName && (
                            <div className="error-message">
                                {errors.fullName}
                            </div>
                        )}
                    </div>

                    {/* Correo electrónico */}
                    <div className="form-group">
                        <label htmlFor="edit-email">
                            Correo electrónico
                        </label>

                        <input
                            id="edit-email"
                            type="email"
                            placeholder="Correo electrónico"
                            value={email}
                            className={
                                errors.email
                                    ? "input-error"
                                    : ""
                            }
                            onChange={event => {
                                setEmail(event.target.value);
                                clearFieldError("email");
                            }}
                            disabled={saving}
                        />

                        {errors.email && (
                            <div className="error-message">
                                {errors.email}
                            </div>
                        )}
                    </div>

                    {/* Nombre de usuario */}
                    <div className="form-group">
                        <label htmlFor="edit-username">
                            Nombre de usuario
                        </label>

                        <input
                            id="edit-username"
                            type="text"
                            placeholder="Nombre de usuario"
                            value={username}
                            className={
                                errors.username
                                    ? "input-error"
                                    : ""
                            }
                            onChange={event => {
                                setUsername(event.target.value);
                                clearFieldError("username");
                            }}
                            disabled={saving}
                        />

                        {errors.username && (
                            <div className="error-message">
                                {errors.username}
                            </div>
                        )}
                    </div>

                    {/* Nueva contraseña */}
                    <div className="form-group">
                        <label htmlFor="edit-new-password">
                            Nueva contraseña
                        </label>
                        <input
                            id="edit-new-password"
                            type="password"
                            value={newPassword}
                            placeholder="Dejar vacio para conservar la contraseña actual"
                            autoComplete="new-password"
                            className={
                                errors.newPassword
                                    ? "input-error"
                                    : ""
                            }
                            onChange={event => {
                                setNewPassword(event.target.value);
                                clearFieldError("newPassword");
                            }}
                        />

                        <small className="field-help">
                            Deje este campo vacío si no desea cambiar la contraseña. La nueva contraseña debe contener al menos 12 caracteres.
                        </small>

                        {errors.newPassword && (
                            <div className="error-message">
                                {errors.newPassword}
                            </div>
                        )}
                    </div>

                    {/* DPI */}
                    <div className="form-group">
                        <label htmlFor="edit-dpi">
                            Documento de Identificación DPI
                        </label>

                        <input
                            id="edit-dpi"
                            type="text"
                            inputMode="numeric"
                            maxLength={13}
                            placeholder="Documento de Identificación"
                            value={dpi}
                            className={
                                errors.dpi
                                    ? "input-error"
                                    : ""
                            }
                            onChange={event => {
                                setDpi(event.target.value);
                                clearFieldError("dpi");
                            }}
                            disabled={saving}
                        />

                        {errors.dpi && (
                            <div className="error-message">
                                {errors.dpi}
                            </div>
                        )}
                    </div>

                    {/* Teléfono */}
                    <div className="form-group">
                        <label htmlFor="edit-phone">
                            Número de teléfono
                        </label>

                        <input
                            id="edit-phone"
                            type="text"
                            inputMode="numeric"
                            maxLength={8}
                            placeholder="Número de teléfono"
                            value={phone}
                            className={
                                errors.phone
                                    ? "input-error"
                                    : ""
                            }
                            onChange={event => {
                                setPhone(event.target.value);
                                clearFieldError("phone");
                            }}
                            disabled={saving}
                        />

                        {errors.phone && (
                            <div className="error-message">
                                {errors.phone}
                            </div>
                        )}
                    </div>

                    {/* NIT */}
                    <div className="form-group">
                        <label htmlFor="edit-nit">
                            NIT
                        </label>

                        <input
                            id="edit-nit"
                            type="text"
                            maxLength={9}
                            placeholder="NIT"
                            value={nit}
                            className={
                                errors.nit
                                    ? "input-error"
                                    : ""
                            }
                            onChange={event => {
                                setNit(event.target.value);
                                clearFieldError("nit");
                            }}
                            disabled={saving}
                        />

                        {errors.nit && (
                            <div className="error-message">
                                {errors.nit}
                            </div>
                        )}
                    </div>

                    {/* Número de seguro */}
                    <div className="form-group">
                        <label htmlFor="edit-insurance-number">
                            Número de seguro
                        </label>

                        <input
                            id="edit-insurance-number"
                            type="text"
                            maxLength={50}
                            placeholder="Número de seguro"
                            value={insuranceNumber}
                            className={
                                errors.insuranceNumber
                                    ? "input-error"
                                    : ""
                            }
                            onChange={event => {
                                setInsuranceNumber(
                                    event.target.value
                                );
                                clearFieldError(
                                    "insuranceNumber"
                                );
                            }}
                            disabled={saving}
                        />

                        {errors.insuranceNumber && (
                            <div className="error-message">
                                {errors.insuranceNumber}
                            </div>
                        )}
                    </div>

                    {/* Rol */}
                    <div className="form-group">
                        <label htmlFor="edit-role">
                            Rol
                        </label>

                        <select
                            id="edit-role"
                            value={roleId}
                            className={
                                errors.roleId
                                    ? "input-error"
                                    : ""
                            }
                            onChange={event => {
                                const newRoleId =
                                    event.target.value;

                                setRoleId(newRoleId);
                                clearFieldError("roleId");

                                const newSelectedRole =
                                    roles.find(
                                        role =>
                                            role.id ===
                                            Number(newRoleId)
                                    );

                                //nos permite limpiar el campo de especialidad si el rol seleccionado no es médico

                                const newRoleIsDoctor = normalizeText(newSelectedRole?.name) === "MEDICO";
                                if (!newRoleIsDoctor) {
                                    setSpecialtyId("");
                                    clearFieldError("specialtyId");
                                }
                            }}
                            disabled={
                                loadingCatalogs || saving
                            }
                        >
                            <option value="">
                                Seleccione un rol
                            </option>

                            {roles.map(role => (
                                <option
                                    key={role.id}
                                    value={role.id}
                                >
                                    {role.name}
                                </option>
                            ))}
                        </select>

                        {errors.roleId && (
                            <div className="error-message">
                                {errors.roleId}
                            </div>
                        )}
                    </div>

                    {/* Sucursal */}
                    <div className="form-group">
                        <label htmlFor="edit-branch">
                            Sucursal
                        </label>

                        <select
                            id="edit-branch"
                            value={branchId}
                            className={
                                errors.branchId
                                    ? "input-error"
                                    : ""
                            }
                            onChange={event => {
                                setBranchId(event.target.value);
                                clearFieldError("branchId");
                            }}
                            disabled={
                                loadingCatalogs || saving
                            }
                        >
                            <option value="">
                                Seleccione una sucursal
                            </option>

                            {branches.map(branch => (
                                <option
                                    key={branch.id}
                                    value={branch.id}
                                >
                                    {branch.name}
                                </option>
                            ))}
                        </select>

                        {errors.branchId && (
                            <div className="error-message">
                                {errors.branchId}
                            </div>
                        )}
                    </div>

                    {/* Especialidad para médicos */}
                    {isDoctorRole && (
                        <div className="form-group">
                            <label htmlFor="edit-specialty">
                                Especialidad
                            </label>

                            <select
                                id="edit-specialty"
                                value={specialtyId}
                                className={
                                    errors.specialtyId
                                        ? "input-error"
                                        : ""
                                }
                                onChange={event => {
                                    setSpecialtyId(
                                        event.target.value
                                    );
                                    clearFieldError(
                                        "specialtyId"
                                    );
                                }}
                                disabled={
                                    loadingCatalogs || saving
                                }
                            >
                                <option value="">
                                    Seleccione una especialidad
                                </option>

                                {specialties.map(
                                    specialty => (
                                        <option
                                            key={specialty.id}
                                            value={specialty.id}
                                        >
                                            {specialty.name}
                                        </option>
                                    )
                                )}
                            </select>

                            {errors.specialtyId && (
                                <div className="error-message">
                                    {errors.specialtyId}
                                </div>
                            )}
                        </div>
                    )}

                    {/* Estado */}
                    <div className="form-group">
                        <label htmlFor="edit-status">
                            Estado
                        </label>

                        <select
                            id="edit-status"
                            value={String(active)}
                            className={
                                errors.active
                                    ? "input-error"
                                    : ""
                            }
                            onChange={event => {
                                setActive(
                                    event.target.value === "true"
                                );
                                clearFieldError("active");
                            }}
                            disabled={saving}
                        >
                            <option value="true">
                                Activo
                            </option>

                            <option value="false">
                                Inactivo
                            </option>
                        </select>

                        {errors.active && (
                            <div className="error-message">
                                {errors.active}
                            </div>
                        )}
                    </div>
                </div>

                {/* Botones */}
                <div className="button-row">
                    <button
                        type="submit"
                        className="btn btn-primary"
                        disabled={saving || loadingCatalogs}
                    >
                        {saving
                            ? "Actualizando..."
                            : "Actualizar Usuario"}
                    </button>

                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={onCancel}
                        disabled={saving}
                    >
                        Cancelar
                    </button>
                </div>
            </form>
        </div>
    );
}

export default UserEdit;