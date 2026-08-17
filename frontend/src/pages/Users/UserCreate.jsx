import { useEffect, useState } from "react";
import { createUser } from "../../api/userApi";
import { getRoles } from "../../api/roleApi";
import { getBranches } from "../../api/branchApi";
import { getSpecialties } from "../../api/specialtyApi";

import "./Users.css";
import "../../styles/forms.css";
import "../../styles/ui.css";

function UserCreate({ onCreated }) {
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [dpi, setDpi] = useState("");
    const [phone, setPhone] = useState("");
    const [roleId, setRoleId] = useState("");
    const [nit, setNit] = useState("");
    const [insuranceNumber, setInsuranceNumber] = useState("");
    const [branchId, setBranchId] = useState("");
    const [specialtyId, setSpecialtyId] = useState("");
    const [active, setActive] = useState(true);

    const [roles, setRoles] = useState([]);
    const [branches, setBranches] = useState([]);
    const [specialties, setSpecialties] = useState([]);
    const [errors, setErrors] = useState({});



    useEffect(() => {
        loadRoles();
        loadBranches();
        loadSpecialties();
    }, []);

    const loadRoles = async () => {
        const response = await getRoles();
        setRoles(response.data);
    };

    const loadBranches = async () => {
        const response = await getBranches();
        setBranches(response.data);
    };

    const loadSpecialties = async () => {
        const response = await getSpecialties();
        setSpecialties(response.data);
    };

    const selectedRole = roles.find(
        role => role.id === Number(roleId)
    );

    const isDoctorRole =
        selectedRole?.name === "Médico";


    const validateForm = () => {
        const newErrors = {};

        if (!fullName.trim()) {
            newErrors.fullName = "El campo Nombre es obligatorio.";
        } else if (fullName.length < 10 || fullName.length > 100) {
            newErrors.fullName =
                `El nombre debe contener entre 10 y 100 caracteres. Usted ingresó ${fullName.length} caracteres.`;
        }

        if (!username.trim()) {
            newErrors.username = "El campo Usuario es obligatorio.";
        } else if (username.length < 8) {
            newErrors.username = "El usuario debe contener al menos 8 caracteres.";
        } else if (username.length > 9) {
            newErrors.username = "El usuario no puede exceder los 9 caracteres.";
        } else if (!/^[a-zA-Z0-9]+$/.test(username)) {
            newErrors.username =
                "El usuario debe contener únicamente caracteres alfanuméricos.";
        }

        if (!roleId) {
            newErrors.roleId = "Debe seleccionar un rol para el usuario.";
        }

        if (!branchId) {
            newErrors.branchId = "Debe seleccionar una sucursal para el usuario.";
        }

        if (dpi && !/^\d{13}$/.test(dpi)) {
            newErrors.dpi =
                "El DPI debe contener exactamente 13 dígitos numéricos.";
        }

        if (phone && !/^\d{8}$/.test(phone)) {
            newErrors.phone =
                "El teléfono debe contener exactamente 8 dígitos.";
        }

        if (nit) {
            if (nit.length < 8 || nit.length > 9) {
                newErrors.nit =
                    `El NIT debe contener entre 8 y 9 caracteres. Usted ingresó ${nit.length} caracteres.`;
            } else if (!/^[a-zA-Z0-9]+$/.test(nit)) {
                newErrors.nit =
                    "El NIT debe contener únicamente caracteres alfanuméricos.";
            }
        }

        if (insuranceNumber) {
            if (insuranceNumber.length < 5 || insuranceNumber.length > 50) {
                newErrors.insuranceNumber =
                    "El número de seguro debe contener entre 5 y 50 caracteres.";
            }
        }

        if (isDoctorRole && !specialtyId) {
            newErrors.specialtyId =
                "Debe seleccionar una especialidad para el médico.";
        }

        if (active === null || active === undefined) {
            newErrors.active =
                "Debe seleccionar un estado para el usuario.";
        }

        setErrors(newErrors);

        return Object.keys(newErrors).length === 0;
    };

    const handleCreateUser = async (e) => {
        e.preventDefault();

        if (!validateForm()) {
            return;
        }

        try {
            await createUser({
                fullName,
                dpi,
                phone,
                nit,
                insuranceNumber,
                email,
                username,
                password,
                roleId: Number(roleId),
                branchId: branchId ? Number(branchId) : null,
                specialtyId:
                    isDoctorRole && specialtyId
                        ? Number(specialtyId)
                        : null,
                active
            });

            setFullName("");
            setDpi("");
            setPhone("");
            setNit("");
            setInsuranceNumber("");
            setEmail("");
            setUsername("");
            setPassword("");
            setRoleId("");
            setBranchId("");
            setSpecialtyId("");
            setActive(true);
            setErrors({});

            alert("Usuario creado correctamente");
            if (onCreated) {
                onCreated();
            }
        } catch (error) {
            console.error(error);

            alert(
                error.response?.data?.message ||
                "Error al crear usuario"
            );
        }
    };

    return (
        <div className="card">
            <h2 className="section-title">Crear Usuario</h2>

            <form onSubmit={handleCreateUser}>
                <div className="form-grid">
                    <div className="form-group">
                        <label>Nombre completo</label>
                        <input
                            type="text"
                            placeholder="Nombre completo"
                            value={fullName}
                            className={errors.fullName ? "input-error" : ""}
                            onChange={(e) => setFullName(e.target.value)}
                        />

                        {errors.fullName && (
                            <div className="error-message">
                                {errors.fullName}
                            </div>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Correo electrónico</label>
                        <input
                            type="email"
                            placeholder="Correo electrónico"
                            value={email}
                            className={errors.email ? "input-error" : ""}
                            onChange={(e) => setEmail(e.target.value)}
                        />

                        {errors.email && (
                            <div className="error-message">
                                {errors.email}
                            </div>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Nombre de usuario</label>
                        <input
                            type="text"
                            placeholder="Nombre de usuario"
                            value={username}
                            className={errors.username ? "input-error" : ""}
                            onChange={(e) => setUsername(e.target.value)}
                        />

                        {errors.username && (
                            <div className="error-message">
                                {errors.username}
                            </div>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Contraseña</label>
                        <input
                            type="password"
                            placeholder="Contraseña"
                            value={password}
                            className={errors.password ? "input-error" : ""}
                            onChange={(e) => setPassword(e.target.value)}
                        />

                        {errors.password && (
                            <div className="error-message">
                                {errors.password}
                            </div>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Documento de Identificación DPI</label>
                        <input
                            type="text"
                            placeholder="Documento de Identificación"
                            value={dpi}
                            className={errors.dpi ? "input-error" : ""}
                            onChange={(e) => setDpi(e.target.value)}
                        />

                        {errors.dpi && (
                            <div className="error-message">
                                {errors.dpi}
                            </div>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Número de teléfono</label>
                        <input
                            type="text"
                            placeholder="Número de teléfono"
                            value={phone}
                            className={errors.phone ? "input-error" : ""}
                            onChange={(e) => setPhone(e.target.value)}
                        />

                        {errors.phone && (
                            <div className="error-message">
                                {errors.phone}
                            </div>
                        )}
                    </div>

                    <div className="form-group">
                        <label>NIT</label>
                        <input
                            type="text"
                            placeholder="NIT"
                            value={nit}
                            className={errors.nit ? "input-error" : ""}
                            onChange={(e) => setNit(e.target.value)}
                        />

                        {errors.nit && (
                            <div className="error-message">
                                {errors.nit}
                            </div>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Número de seguro</label>
                        <input
                            type="text"
                            placeholder="Número de seguro"
                            value={insuranceNumber}
                            className={errors.insuranceNumber ? "input-error" : ""}
                            onChange={(e) => setInsuranceNumber(e.target.value)}
                        />

                        {errors.insuranceNumber && (
                            <div className="error-message">
                                {errors.insuranceNumber}
                            </div>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Rol</label>
                        <select
                            value={roleId}
                            className={errors.roleId ? "input-error" : ""}
                            onChange={(e) => {
                                setRoleId(e.target.value);

                                const selected = roles.find(
                                    role => role.id === Number(e.target.value)
                                );

                                if (selected?.name !== "Médico") {
                                    setSpecialtyId("");
                                }
                            }}
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

                    <div className="form-group">
                        <label>Sucursal</label>
                        <select
                            value={branchId}
                            className={errors.branchId ? "input-error" : ""}
                            onChange={(e) => setBranchId(e.target.value)}
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

                    {isDoctorRole && (
                        <div className="form-group">
                            <label>Especialidad</label>
                            <select
                                value={specialtyId}
                                className={errors.specialtyId ? "input-error" : ""}
                                onChange={(e) => setSpecialtyId(e.target.value)}
                            >
                                <option value="">
                                    Seleccione una especialidad
                                </option>

                                {specialties.map(specialty => (
                                    <option
                                        key={specialty.id}
                                        value={specialty.id}
                                    >
                                        {specialty.name}
                                    </option>
                                ))}
                            </select>

                            {errors.specialtyId && (
                                <div className="error-message">
                                    {errors.specialtyId}
                                </div>
                            )}
                        </div>
                    )}

                    <div className="form-group">
                        <label>Estado</label>
                        <select
                            value={String(active)}
                            className={errors.active ? "input-error" : ""}
                            onChange={(e) =>
                                setActive(e.target.value === "true")
                            }
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

                <div className="button-row">
                    <button
                        type="submit"
                        className="btn btn-primary"
                    >
                        Crear Usuario
                    </button>
                </div>
            </form>
        </div>
    );
}

export default UserCreate;