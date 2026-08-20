import { Link, useNavigate } from "react-router-dom";
import "./MainLayout.css";

function MainLayout({ children }) {
    const navigate = useNavigate();

    const role = localStorage.getItem("role");

    const isAdmin =
        role === "ADMIN" ||
        role === "Administrador";

    const isPatient =
        role === "PACIENTE" ||
        role === "Paciente";

    const handleLogout = () => {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        localStorage.removeItem("userId");
        localStorage.removeItem("fullName");

        navigate("/login", {
            replace: true
        });
    };

    return (
        <div className="main-layout">
            <aside className="main-sidebar">
                <h2 className="main-sidebar-title">
                    Hospital HIS
                </h2>

                <nav className="main-sidebar-nav">
                    <Link
                        to="/dashboard"
                        className="main-sidebar-link"
                    >
                        Dashboard
                    </Link>

                    {isAdmin && (
                        <>
                            <Link
                                to="/users"
                                className="main-sidebar-link"
                            >
                                Usuarios
                            </Link>

                            <Link
                                to="/branches"
                                className="main-sidebar-link"
                            >
                                Sucursales
                            </Link>

                            <Link
                                to="/specialties"
                                className="main-sidebar-link"
                            >
                                Especialidades
                            </Link>
                        </>
                    )}

                    {isPatient && (
                        <Link
                            to="/appointments/new"
                            className="main-sidebar-link"
                        >
                            Agendar Cita
                        </Link>
                    )}
                </nav>

                <div className="main-sidebar-footer">
                    <button
                        type="button"
                        className="logout-button"
                        onClick={handleLogout}
                    >
                        Cerrar Sesión
                    </button>
                </div>
            </aside>

            <main className="main-content">
                {children}
            </main>
        </div>
    );
}

export default MainLayout;