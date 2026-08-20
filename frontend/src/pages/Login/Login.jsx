import { useState } from "react";
import { login } from "../../api/authApi";
import { Link, useNavigate } from "react-router-dom";

import "./Login.css";
import "../../styles/ui.css";

function Login() {

    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const navigate = useNavigate();

    const handleLogin = async (e) => {

        e.preventDefault();

        try {
            const response = await login({
                username,
                password
            });
            console.log(response.data);
            localStorage.setItem("token", response.data.token);

            localStorage.setItem(
                "role",
                response.data.role
            );

            localStorage.setItem(
                "userId",
                String(response.data.userId)
            );

            localStorage.setItem(
                "fullName",
                response.data.fullName
            );

            navigate("/dashboard")
        } catch (error) {
            console.error(error);
            alert("Usuario o contraseña incorrectos");
        }
    };

    return (
        <div className="login-page">
            <div className="login-card">
                <h1>Hospital HIS</h1>
                <h2>Iniciar Sesión</h2>

                <form onSubmit={handleLogin}>
                    <div className="form-group">
                        <label>Usuario</label>
                        <input
                            type="text"
                            value={username}
                            onChange={(e) =>
                                setUsername(e.target.value)}
                        />
                    </div>

                    <div className="form-group">
                        <label>Contraseña</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) =>
                                setPassword(e.target.value)}
                        />
                    </div>

                    <button
                        type="submit"
                        className="btn btn-primary"
                        style={{ width: "100%", marginTop: "18px" }}
                    >
                        Iniciar Sesión
                    </button>
                </form>

                <div className="login-actions">
                    <Link to="/">
                        Volver al portal
                    </Link>

                    <Link to="/register">
                        Registrarse
                    </Link>
                </div>
            </div>
        </div>
    );

}

export default Login;