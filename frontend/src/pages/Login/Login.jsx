import { useState } from "react";
import { login } from "../../api/authApi";
import { useNavigate } from "react-router-dom";

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
            navigate("/dashboard")
        } catch (error) {
            console.error(error);
            alert("Usuario o contraseña incorrectos");
        }
    };

    return (
        <div>
            <h1>Hospital HIS</h1>

            <h2>Iniciar Sesión</h2>

            <form onSubmit={handleLogin}>

                <div>
                    <label>Usuario</label>
                    <br />
                    <input
                        type="text"
                        value={username}
                        onChange={(e) =>
                            setUsername(e.target.value)}
                    />
                </div>

                <br />

                <div>
                    <label>Contraseña</label>
                    <br />
                    <input
                        type="password"
                        value={password}
                        onChange={(e) =>
                            setPassword(e.target.value)}
                    />
                </div>

                <br />

                <button type="submit">
                    Iniciar Sesión
                </button>

            </form>
        </div>
    );
}

export default Login;