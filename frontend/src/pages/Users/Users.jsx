import { useState } from "react";
import MainLayout from "../../layouts/MainLayout";
import UserList from "./UserList";
import UserCreate from "./UserCreate";
import UserEdit from "./UserEdit";
import { changeUserStatus } from "../../api/userApi";

import ConfirmModal from "../../components/ConfirmModal";


function Users() {
    const [view, setView] = useState("list");
    const [selectedUser, setSelectedUser] = useState(null);
    const [refreshKey, setRefreshKey] = useState(0);
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [userToDelete, setUserToDelete] = useState(null);

    const handleEdit = (user) => {
        setSelectedUser(user);
        setView("edit");
    };

    const handleDelete = (user) => {
        setUserToDelete(user);
        setShowDeleteModal(true);
    };

    const confirmDeleteUser = async () => {
        if (!userToDelete) {
            return;
        }

        try {
            await changeUserStatus(
                userToDelete.id,
                false
            );

            alert(
                `El usuario ${userToDelete.username} ha sido eliminado correctamente.`
            );

            setShowDeleteModal(false);
            setUserToDelete(null);

            setRefreshKey(prev => prev + 1);
            setView("list");

        } catch (error) {
            console.error(error);
            alert("Error al eliminar usuario.");
        }
    };

    const cancelDeleteUser = () => {
        setShowDeleteModal(false);
        setUserToDelete(null);
    };

    const handleUpdated = () => {
        setSelectedUser(null);
        setRefreshKey(prev => prev + 1);
        setView("list");
    };

    const handleCancelEdit = () => {
        setSelectedUser(null);
        setView("list");
    };

    return (
        <MainLayout>
            <h1>Módulo Usuarios</h1>

            <div>
                <button onClick={() => setView("list")}>
                    Listar Usuarios
                </button>

                <button onClick={() => setView("create")}>
                    Crear Usuario
                </button>
            </div>

            <hr />

            {view === "list" && (
                <UserList
                    key={refreshKey}
                    onEdit={handleEdit}
                    onDelete={handleDelete}
                />
            )}

            {view === "create" && (
                <UserCreate />
            )}

            {view === "edit" && selectedUser && (
                <UserEdit
                    user={selectedUser}
                    onUpdated={handleUpdated}
                    onCancel={handleCancelEdit}
                />
            )}

            {showDeleteModal && userToDelete && (
                <ConfirmModal
                    title="Confirmar eliminación"
                    message={`¿Está seguro que desea eliminar el usuario "${userToDelete.username}"? Esta acción no se puede deshacer.`}
                    onConfirm={confirmDeleteUser}
                    onCancel={cancelDeleteUser}
                />
            )}
        </MainLayout>
    );
}

export default Users;

