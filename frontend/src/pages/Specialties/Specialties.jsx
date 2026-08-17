import { useEffect, useState } from "react";
import MainLayout from "../../layouts/MainLayout";
import {
    getSpecialties,
    createSpecialty,
    updateSpecialty,
    changeSpecialtyStatus
} from "../../api/specialtyApi";
import "../../styles/ui.css";

function Specialties() {
    const [specialties, setSpecialties] = useState([]);

    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [active, setActive] = useState(true);

    const [editingSpecialty, setEditingSpecialty] = useState(null);
    const [errors, setErrors] = useState({});

    useEffect(() => {
        loadSpecialties();
    }, []);

    const loadSpecialties = async () => {
        try {
            const response = await getSpecialties();
            setSpecialties(response.data);
        } catch (error) {
            console.error(error);
            alert("Error al cargar especialidades.");
        }
    };

    const validateForm = () => {
        const newErrors = {};

        if (!name.trim()) {
            newErrors.name = "El nombre de la especialidad es obligatorio.";
        }

        if (!description.trim()) {
            newErrors.description = "La descripción de la especialidad es obligatoria.";
        }

        if (active === null || active === undefined) {
            newErrors.active = "Debe seleccionar un estado.";
        }

        setErrors(newErrors);

        return Object.keys(newErrors).length === 0;
    };

    const clearForm = () => {
        setName("");
        setDescription("");
        setActive(true);
        setEditingSpecialty(null);
        setErrors({});
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) {
            return;
        }

        try {
            const data = {
                name,
                description,
                active
            };

            if (editingSpecialty) {
                await updateSpecialty(editingSpecialty.id, data);
                alert("Especialidad actualizada correctamente.");
            } else {
                await createSpecialty(data);
                alert("Especialidad creada correctamente.");
            }

            clearForm();
            loadSpecialties();

        } catch (error) {
            console.error(error);

            alert(
                error.response?.data?.message ||
                "Error al guardar especialidad."
            );
        }
    };

    const handleEdit = (specialty) => {
        setEditingSpecialty(specialty);
        setName(specialty.name || "");
        setDescription(specialty.description || "");
        setActive(specialty.active ?? true);
        setErrors({});
    };

    const handleDelete = async (specialty) => {
        const confirmDelete = window.confirm(
            `¿Está seguro que desea eliminar lógicamente la especialidad "${specialty.name}"?`
        );

        if (!confirmDelete) {
            return;
        }

        try {
            await changeSpecialtyStatus(specialty.id, false);
            alert("Especialidad eliminada lógicamente.");
            loadSpecialties();

        } catch (error) {
            console.error(error);
            alert("Error al eliminar especialidad.");
        }
    };

    return (
        <MainLayout>
            <h1 className="page-title">Especialidades</h1>

            <div className="card">
                <h2 className="section-title">
                    {editingSpecialty ? "Editar Especialidad" : "Crear Especialidad"}
                </h2>

                <form onSubmit={handleSubmit}>
                    <div className="form-grid">
                        <div className="form-group">
                            <label>Nombre</label>
                            <input
                                type="text"
                                placeholder="Nombre de la especialidad"
                                value={name}
                                className={errors.name ? "input-error" : ""}
                                onChange={(e) => setName(e.target.value)}
                            />

                            {errors.name && (
                                <div className="error-message">
                                    {errors.name}
                                </div>
                            )}
                        </div>

                        <div className="form-group">
                            <label>Descripción</label>
                            <input
                                type="text"
                                placeholder="Descripción"
                                value={description}
                                className={errors.description ? "input-error" : ""}
                                onChange={(e) => setDescription(e.target.value)}
                            />

                            {errors.description && (
                                <div className="error-message">
                                    {errors.description}
                                </div>
                            )}
                        </div>

                        <div className="form-group">
                            <label>Estado</label>
                            <select
                                value={String(active)}
                                className={errors.active ? "input-error" : ""}
                                onChange={(e) =>
                                    setActive(e.target.value === "true")
                                }
                            >
                                <option value="true">Activo</option>
                                <option value="false">Inactivo</option>
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
                            {editingSpecialty ? "Actualizar" : "Crear"}
                        </button>

                        {editingSpecialty && (
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={clearForm}
                            >
                                Cancelar
                            </button>
                        )}
                    </div>
                </form>
            </div>

            <br />

            <div className="table-card">
                <h2 className="section-title">Listado de Especialidades</h2>

                {specialties.length === 0 ? (
                    <p style={{ color: "#dc2626" }}>
                        No hay especialidades registradas.
                    </p>
                ) : (
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Nombre</th>
                                <th>Descripción</th>
                                <th>Estado</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>

                        <tbody>
                            {specialties.map(specialty => (
                                <tr key={specialty.id}>
                                    <td>{specialty.id}</td>
                                    <td>{specialty.name}</td>
                                    <td>{specialty.description}</td>
                                    <td>
                                        <span
                                            className={
                                                specialty.active
                                                    ? "status-active"
                                                    : "status-inactive"
                                            }
                                        >
                                            {specialty.active ? "Activo" : "Inactivo"}
                                        </span>
                                    </td>
                                    <td>
                                        <div className="button-row">
                                            <button
                                                className="btn btn-secondary"
                                                onClick={() => handleEdit(specialty)}
                                            >
                                                Editar
                                            </button>

                                            <button
                                                className="btn btn-danger-outline"
                                                onClick={() => handleDelete(specialty)}
                                            >
                                                Eliminar
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>
        </MainLayout>
    );
}

export default Specialties;