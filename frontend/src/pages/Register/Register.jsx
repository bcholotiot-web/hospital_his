import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { register } from "../../api/authApi";
import "./Register.css";
import "../../styles/forms.css";

function Register() {
    const navigate = useNavigate();

    const [fullName, setFullName] = useState("");
    const [dpi, setDpi] = useState("");
    const [nit, setNit] = useState("");
    const [phone, setPhone] = useState("");
    const [insuranceNumber, setInsuranceNumber] = useState("");
    const [email, setEmail] = useState("");
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");

    const [errors, setErrors] = useState({});
    const [generalMessage, setGeneralMessage] = useState("");

    const validateForm = () => {
        const newErrors = {};

        if (!fullName.trim()) {
            newErrors.fullName = "El nombre completo es obligatorio.";
        } else if (fullName.length < 10 || fullName.length > 100) {
            newErrors.fullName =
                `El nombre debe contener entre 10 y 100 caracteres. Usted ingresó ${fullName.length} caracteres.`;
        }

        if (!dpi.trim()) {
            newErrors.dpi =
                "El campo DPI es obligatorio. Por favor, ingrese su número de DPI.";
        } else if (dpi.length !== 13) {
            newErrors.dpi =
                `El DPI debe contener exactamente 13 dígitos. Usted ingresó ${dpi.length} dígitos.`;
        } else if (!/^\d+$/.test(dpi)) {
            newErrors.dpi =
                "El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales.";
        }

        if (!nit.trim()) {
            newErrors.nit = "El campo NIT es obligatorio.";
        } else if (nit.length < 8 || nit.length > 9) {
            newErrors.nit =
                `El NIT debe contener entre 8 y 9 caracteres. Usted ingresó ${nit.length} caracteres.`;
        } else if (!/^[a-zA-Z0-9]+$/.test(nit)) {
            newErrors.nit =
                "El NIT debe contener únicamente caracteres alfanuméricos.";
        }

        if (!phone.trim()) {
            newErrors.phone = "El número de teléfono es obligatorio.";
        } else if (!/^\d{8}$/.test(phone)) {
            newErrors.phone =
                "El número de teléfono debe contener exactamente 8 dígitos numéricos.";
        }

        if (insuranceNumber.trim()) {
            if (
                insuranceNumber.length < 5 ||
                insuranceNumber.length > 50
            ) {
                newErrors.insuranceNumber =
                    "El número de seguro debe contener entre 5 y 50 caracteres.";
            }
        }

        if (!email.trim()) {
            newErrors.email = "El correo electrónico es obligatorio.";
        } else if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/.test(email)) {
            newErrors.email =
                "El formato del correo electrónico no es válido. Ejemplo: usuario@dominio.com";
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

        if (!password.trim()) {
            newErrors.password = "La contraseña es obligatoria.";
        } else if (password.length < 12) {
            newErrors.password =
                "La contraseña debe contener al menos 12 caracteres.";
        }

        setErrors(newErrors);

        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        setGeneralMessage("");

        if (!validateForm()) {
            return;
        }

        try {
            const response = await register({
                fullName,
                dpi,
                nit,
                phone,
                insuranceNumber,
                email,
                username,
                password
            });

            setGeneralMessage(
                response.data.message ||
                "¡Registro exitoso! Su cuenta ha sido creada. Ahora puede iniciar sesión con sus credenciales."
            );

            setFullName("");
            setDpi("");
            setNit("");
            setPhone("");
            setInsuranceNumber("");
            setEmail("");
            setUsername("");
            setPassword("");
            setErrors({});

            setTimeout(() => {
                navigate("/login");
            }, 1500);

        } catch (error) {
            const message =
                error.response?.data?.message ||
                "Error al registrar usuario.";

            setGeneralMessage(message);
        }
    };
    return (
        <div className="register-page">
            <div className="register-card">
                <h1>Hospital HIS</h1>
                <h2>Registro de Usuario Externo</h2>

                {generalMessage && (
                    <div className="general-message">
                        {generalMessage}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Nombre completo</label>
                        <input
                            type="text"
                            value={fullName}
                            className={errors.fullName ? "input-error" : ""}
                            onChange={(e) => setFullName(e.target.value)}
                        />

                        {errors.fullName && (
                            <p className="error-message">
                                {errors.fullName}
                            </p>
                        )}
                    </div>

                    <div className="form-group">
                        <label>DPI</label>
                        <input
                            type="text"
                            value={dpi}
                            className={errors.dpi ? "input-error" : ""}
                            onChange={(e) => setDpi(e.target.value)}
                        />

                        {errors.dpi && (
                            <p className="error-message">
                                {errors.dpi}
                            </p>
                        )}
                    </div>

                    <div className="form-group">
                        <label>NIT</label>
                        <input
                            type="text"
                            value={nit}
                            className={errors.nit ? "input-error" : ""}
                            onChange={(e) => setNit(e.target.value)}
                        />

                        {errors.nit && (
                            <p className="error-message">
                                {errors.nit}
                            </p>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Teléfono</label>
                        <input
                            type="text"
                            value={phone}
                            className={errors.phone ? "input-error" : ""}
                            onChange={(e) => setPhone(e.target.value)}
                        />

                        {errors.phone && (
                            <p className="error-message">
                                {errors.phone}
                            </p>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Número de afiliado del seguro médico, opcional</label>
                        <input
                            type="text"
                            value={insuranceNumber}
                            className={errors.insuranceNumber ? "input-error" : ""}
                            onChange={(e) => setInsuranceNumber(e.target.value)}
                        />

                        {errors.insuranceNumber && (
                            <p className="error-message">
                                {errors.insuranceNumber}
                            </p>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Correo electrónico</label>
                        <input
                            type="email"
                            value={email}
                            className={errors.email ? "input-error" : ""}
                            onChange={(e) => setEmail(e.target.value)}
                        />

                        {errors.email && (
                            <p className="error-message">
                                {errors.email}
                            </p>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Nombre de usuario</label>
                        <input
                            type="text"
                            value={username}
                            className={errors.username ? "input-error" : ""}
                            onChange={(e) => setUsername(e.target.value)}
                        />

                        {errors.username && (
                            <p className="error-message">
                                {errors.username}
                            </p>
                        )}
                    </div>

                    <div className="form-group">
                        <label>Contraseña</label>
                        <input
                            type="password"
                            value={password}
                            className={errors.password ? "input-error" : ""}
                            onChange={(e) => setPassword(e.target.value)}
                        />

                        {errors.password && (
                            <p className="error-message">
                                {errors.password}
                            </p>
                        )}
                    </div>

                    <button
                        type="submit"
                        className="btn-register"
                    >
                        Registrarse
                    </button>
                </form>

                <div className="register-actions">
                    <Link to="/">
                        Volver al portal
                    </Link>

                    <Link to="/login">
                        Ya tengo cuenta, iniciar sesión
                    </Link>
                </div>
            </div>
        </div>
    );
} export default Register;