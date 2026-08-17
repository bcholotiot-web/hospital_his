import { useEffect, useState } from "react";
import MainLayout from "../../layouts/MainLayout";
import {
    getBranches,
    createBranch,
    updateBranch,
    changeBranchStatus
} from "../../api/branchApi";
import "../../styles/ui.css";

function Branches() {
    const [branches, setBranches] = useState([]);

    const [name, setName] = useState("");
    const [address, setAddress] = useState("");
    const [active, setActive] = useState(true);

    const [editingBranch, setEditingBranch] = useState(null);
    const [errors, setErrors] = useState({});

    useEffect(() => {
        loadBranches();
    }, []);

    const loadBranches = async () => {
        try {
            const response = await getBranches();
            setBranches(response.data);
        } catch (error) {
            console.error(error);
        }
    };

    const validateForm = () => {
        const newErrors = {};

        if (!name.trim()) {
            newErrors.name = "El nombre de la sucursal es obligatorio.";
        }

        setErrors(newErrors);

        return Object.keys(newErrors).length === 0;
    };

    const clearForm = () => {
        setName("");
        setAddress("");
        setActive(true);
        setEditingBranch(null);
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
                address,
                active
            };

            if (editingBranch) {
                await updateBranch(editingBranch.id, data);
                alert("Sucursal actualizada correctamente.");
            } else {
                await createBranch(data);
                alert("Sucursal creada correctamente.");
            }

            clearForm();
            loadBranches();

        } catch (error) {
            console.error(error);

            alert(
                error.response?.data?.message ||
                "Error al guardar sucursal."
            );
        }
    };

    const handleEdit = (branch) => {
        setEditingBranch(branch);
        setName(branch.name || "");
        setAddress(branch.address || "");
        setActive(branch.active ?? true);
        setErrors({});
    };

    const handleDelete = async (branch) => {
        const confirmDelete = window.confirm(
            `¿Está seguro que desea eliminar lógicamente la sucursal "${branch.name}"?`
        );

        if (!confirmDelete) {
            return;
        }

        try {
            await changeBranchStatus(branch.id, false);
            alert("Sucursal eliminada lógicamente.");
            loadBranches();

        } catch (error) {
            console.error(error);
            alert("Error al eliminar sucursal.");
        }
    };

    return (
        <MainLayout>
            <h1 className="page-title">Sucursales</h1>

            <div className="card">
                <h2 className="section-title">
                    {editingBranch ? "Editar Sucursal" : "Crear Sucursal"}
                </h2>

                <form onSubmit={handleSubmit}>
                    <div className="form-grid">
                        <div className="form-group">
                            <label>Nombre</label>
                            <input
                                type="text"
                                placeholder="Nombre de la sucursal"
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
                            <label>Dirección</label>
                            <input
                                type="text"
                                placeholder="Dirección"
                                value={address}
                                onChange={(e) => setAddress(e.target.value)}
                            />
                        </div>

                        <div className="form-group">
                            <label>Estado</label>
                            <select
                                value={String(active)}
                                onChange={(e) =>
                                    setActive(e.target.value === "true")
                                }
                            >
                                <option value="true">Activo</option>
                                <option value="false">Inactivo</option>
                            </select>
                        </div>
                    </div>

                    <div className="button-row">
                        <button
                            type="submit"
                            className="btn btn-primary"
                        >
                            {editingBranch ? "Actualizar" : "Crear"}
                        </button>

                        {editingBranch && (
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
                <h2 className="section-title">Listado de Sucursales</h2>

                <table className="data-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Nombre</th>
                            <th>Dirección</th>
                            <th>Estado</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>

                    <tbody>
                        {branches.map(branch => (
                            <tr key={branch.id}>
                                <td>{branch.id}</td>
                                <td>{branch.name}</td>
                                <td>{branch.address}</td>
                                <td>
                                    <span
                                        className={
                                            branch.active
                                                ? "status-active"
                                                : "status-inactive"
                                        }
                                    >
                                        {branch.active ? "Activo" : "Inactivo"}
                                    </span>
                                </td>
                                <td>
                                    <div className="button-row">
                                        <button
                                            className="btn btn-secondary"
                                            onClick={() => handleEdit(branch)}
                                        >
                                            Editar
                                        </button>

                                        <button
                                            className="btn btn-danger-outline"
                                            onClick={() => handleDelete(branch)}
                                        >
                                            Eliminar
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </MainLayout>
    );
}

export default Branches;