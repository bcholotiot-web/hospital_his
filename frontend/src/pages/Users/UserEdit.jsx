import { useEffect, useState } from "react";
import { updateUser } from "../../api/userApi";
import { getRoles } from "../../api/roleApi";
import { getBranches } from "../../api/branchApi";
import { getSpecialties } from "../../api/specialtyApi";

import "../../styles/forms.css";

function UserEdit({ user, onUpdated, onCancel }) {
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [username, setUsername] = useState("");
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

    useEffect(() => {
        loadCatalogs();
    }, []);

    useEffect(() => {
        if (user) {
            setFullName(user.fullName || "");
            setEmail(user.email || "");
            setUsername(user.username || "");
            setDpi(user.dpi || "");
            setPhone(user.phone || "");
            setNit(user.nit || "");
            setInsuranceNumber(user.insuranceNumber || "");
            setActive(user.active ?? true);

            setRoleId(user.roleId ? String(user.roleId) : "");
            setBranchId(user.branchId ? String(user.branchId) : "");
            setSpecialtyId(user.specialtyId ? String(user.specialtyId) : "");
        }
    }, [user]);

    const loadCatalogs = async () => {
        try {
            const rolesResponse = await getRoles();
            const branchesResponse = await getBranches();
            const specialtiesResponse = await getSpecialties();

            setRoles(rolesResponse.data);
            setBranches(branchesResponse.data);
            setSpecialties(specialtiesResponse.data);
        } catch (error) {
            console.error(error);
        }
    };

    const selectedRole = roles.find(
        role => role.id === Number(roleId)
    );

    const isDoctorRole =
        selectedRole?.name === "Médico";

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!fullName.trim()) {
            alert("El campo Nombre es obligatorio.");
            return;
        }

        if (fullName.length < 10 || fullName.length > 100) {
            alert("El campo Nombre debe tener entre 10 y 100 caracteres. Usted ingresó " + fullName.length + " caracteres.");
            return;
        }

        if (!username.trim()) {
            alert("El campo Usuario es obligatorio.");
            return;
        }

        if (username.length < 8) {
            alert("El usuario debe contener al menos 8 caracteres.");
            return;
        }

        if (username.length > 9) {
            alert("El usuario no puede exceder los 9 caracteres.");
            return;
        }

        if (!/^[a-zA-Z0-9]+$/.test(username)) {
            alert("El usuario debe contener únicamente caracteres alfanuméricos.");
            return;
        }

        if (dpi && !/^\d{13}$/.test(dpi)) {
            alert("El DPI debe contener exactamente 13 dígitos numéricos.");
            return;
        }

        if (phone && !/^\d{8}$/.test(phone)) {
            alert("El teléfono debe contener exactamente 8 dígitos.");
            return;
        }

        if (nit) {
            if (nit.length < 8 || nit.length > 9) {
                alert(
                    `El NIT debe contener entre 8 y 9 caracteres. Usted ingresó ${nit.length} caracteres.`
                );
                return;
            }

            if (!/^[a-zA-Z0-9]+$/.test(nit)) {
                alert("El NIT debe contener únicamente caracteres alfanuméricos.");
                return;
            }
        }




        if (!roleId) {
            alert("Debe seleccionar un rol.");
            return;
        }

        if (isDoctorRole && !specialtyId) {
            alert("Debe seleccionar una especialidad para el médico.");
            return;
        }
        if (active === null || active === undefined) {
            alert("Debe seleccionar un estado para el usuario.");
            return;
        }
        if (insuranceNumber) {
            if (insuranceNumber.length < 5 || insuranceNumber.length > 50) {
                alert(
                    `El número de seguro debe contener entre 5 y 50 caracteres.`
                );
                return;
            }
        }

        try {
            await updateUser(user.id, {
                fullName,
                dpi,
                nit,
                phone,
                email,
                username,
                roleId: Number(roleId),
                insuranceNumber,
                branchId: branchId ? Number(branchId) : null,
                specialtyId:
                    isDoctorRole && specialtyId
                        ? Number(specialtyId)
                        : null,
                active
            });

            alert("Usuario actualizado correctamente.");
            onUpdated();

        } catch (error) {
            console.error(error);

            alert(
                error.response?.data?.message ||
                "Error al actualizar usuario."
            );
        }
    };

    return (
        <div>
            <h2>Editar Usuario</h2>

            <form onSubmit={handleSubmit}>
                <p>Nombre Completo</p>
                <input
                    type="text"
                    placeholder="Nombre Completo"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                />

                <br />
                <br />

                <p>Correo Electrónico</p>
                <input
                    type="email"
                    placeholder="Correo Electrónico"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                />

                <br />
                <br />
                <p>Nombre de Usuario</p>
                <input
                    type="text"
                    placeholder="Nombre de Usuario"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                />

                <br />
                <br />
                <p>Documento de Identificación DPI</p>
                <input
                    type="text"
                    placeholder="Documento de Identificación"
                    value={dpi}
                    onChange={(e) => setDpi(e.target.value)}
                />

                <br />
                <br />
                <p>Número de Teléfono</p>
                <input
                    type="text"
                    placeholder="Número de Teléfono"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                />

                <br />
                <br />
                <p>NIT</p>
                <input
                    type="text"
                    placeholder="NIT"
                    value={nit}
                    onChange={(e) => setNit(e.target.value)}
                />

                <br />
                <br />
                <p>Número de Seguro</p>
                <input
                    type="text"
                    placeholder="Número de Seguro"
                    value={insuranceNumber}
                    onChange={(e) => setInsuranceNumber(e.target.value)}
                />

                <br />
                <br />
                <p>Rol</p>
                <select
                    value={roleId}
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

                <br />
                <br />

                <p>Sucursal</p>
                <select
                    value={branchId}
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

                <br />
                <br />

                {isDoctorRole && (
                    <>
                        <p>Especialidad</p>
                        <select
                            value={specialtyId}
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

                        <br />
                        <br />
                    </>
                )}
                <p>Estado</p>
                <select
                    value={String(active)}
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

                <br />
                <br />

                <button type="submit">
                    Actualizar
                </button>

                <button
                    type="button"
                    onClick={onCancel}
                >
                    Cancelar
                </button>
            </form>
        </div>
    );
} export default UserEdit;