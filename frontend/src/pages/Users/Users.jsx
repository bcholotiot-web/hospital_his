import { useEffect, useState } from "react";
import MainLayout from "../../layouts/MainLayout";
import { getUsers, createUser } from "../../api/userApi";
import { getRoles } from "../../api/roleApi";

function Users() {

    const [users, setUsers] = useState([]);
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [roles, setRoles] = useState([]);
    const [roleId, setRoleId] = useState("");

    useEffect(() => {
        loadUsers();
        loadRoles();
    }, []);

    const loadUsers = async () => {
        try {
            const response = await getUsers();
            setUsers(response.data);
        } catch (error) {
            console.error(error);
        }
    };

    const loadRoles = async () => {
        try {
            const response = await getRoles();
            setRoles(response.data);
        } catch (error) {
            console.error(error);
        }
    };

    const handleCreateUser = async (e) => {
        e.preventDefault();
        try {
            await createUser({
                fullName,
                dpi: Math.floor(Math.random() * 10000000000000).toString(),
                nit: "12345678",
                phone: "55112233",
                email,
                username,
                password,
                roleId,
                insuranceNumber: null,
                branchId: null,
                specialtyId: null,
                active: true

            });

            loadUsers();
            setFullName("");
            setEmail("");
            setUsername("");
            setPassword("");
            alert("Usuario creado correctamente");
        } catch (error) {
            console.error(error);
            alert("Error al crear usuario");
        }
    };

    return (
        <MainLayout>
            <h1>Usuarios</h1>
            <h2>Crear Usuario</h2>

            <form onSubmit={handleCreateUser}>
                <input type="text" placeholder="Nombre" value={fullName} onChange={(e) => setFullName(e.target.value)} />
                <br />
                <br />
                <input type="email" placeholder="Correo" value={email} onChange={(e) => setEmail(e.target.value)} />
                <br />
                <br />
                <input type="text" placeholder="Usuario" value={username} onChange={(e) => setUsername(e.target.value)} />
                <br />
                <br />
                <input type="password" placeholder="Contraseña" value={password} onChange={(e) => setPassword(e.target.value)} />
                <br />
                <br />

                <select value={roleId} onChange={(e) => setRoleId(e.target.value)}>
                    <option value="">Seleccione un rol</option>
                    {
                        roles.map(role => (<option key={role.id} value={role.id}>{role.name}</option>))
                    }

                </select>
                <button type="submit">Crear Usuario</button>
            </form>

            <hr />

            <table border="1">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Nombre</th>
                        <th>Usuario</th>
                        <th>Correo</th>
                        <th>Rol</th>
                        <th>Estado</th>
                    </tr>
                </thead>
                <tbody>

                    {
                        users.map(user => (
                            <tr key={user.id}>
                                <td>{user.id}</td>
                                <td>{user.fullName}</td>
                                <td>{user.username}</td>
                                <td>{user.email}</td>
                                <td>{user.role}</td>
                                <td>{user.active ? "Activo" : "Inactivo"}
                                </td>
                            </tr>
                        ))
                    }
                </tbody>
            </table>
        </MainLayout>
    );
}

export default Users;
