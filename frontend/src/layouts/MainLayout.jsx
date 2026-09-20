import { Link, useNavigate } from "react-router-dom";
import "./MainLayout.css";

function MainLayout({ children }) {
    const navigate = useNavigate();

    const role = localStorage.getItem("role");
    const normalizedRole = normalizeRole(role);

    const isAdmin =
        normalizedRole === "ADMIN" ||
        normalizedRole === "ADMINISTRADOR";

    const isPatient =
        normalizedRole === "PACIENTE";

    const isReceptionist =
        normalizedRole === "RECEPCIONISTA";

    const isCashier =
        normalizedRole === "CAJERO";

    const isNursing =
        normalizedRole === "ENFERMERIA";

    const isDoctor =
        normalizedRole === "MEDICO";

    const isLaboratory =
        normalizedRole === "LABORATORIO";

    const isPharmacy =
        normalizedRole === "FARMACIA";

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
                        <>
                            <Link
                                to="/appointments/new"
                                className="main-sidebar-link"
                            >
                                Agendar Cita
                            </Link>

                            <Link
                                to="/appointments"
                                className="main-sidebar-link"
                            >
                                Mis Citas
                            </Link>
                        </>
                    )}

                    {isReceptionist && (
                        <Link
                            to="/reception"
                            className="main-sidebar-link"
                        >
                            Recepción de Pacientes
                        </Link>
                    )}

                    {isCashier && (
                        <Link
                            to="/cashier"
                            className="main-sidebar-link"
                        >
                            Cobro en Caja
                        </Link>
                    )}

                    {isNursing && (
                        <Link
                            to="/nursing"
                            className="main-sidebar-link"
                        >
                            Signos Vitales
                        </Link>
                    )}

                    {isDoctor && (
                        <Link
                            to="/doctor"
                            className="main-sidebar-link"
                        >
                            Consultas Médicas
                        </Link>
                    )}

                    {isLaboratory && (
                        <Link
                            to="/laboratory"
                            className="main-sidebar-link"
                        >
                            Gestión de Laboratorio
                        </Link>
                    )}

                    {isPharmacy && (
                        <>
                            <Link
                                to="/pharmacy"
                                className="main-sidebar-link"
                            >
                                Panel de Farmacia
                            </Link>

                            <Link
                                to="/pharmacy/dispensations"
                                className="main-sidebar-link"
                            >
                                Despacho de Medicamentos
                            </Link>
                        </>
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

/**
 * Normaliza el nombre del rol almacenado
 * después del login.
 */
function normalizeRole(value) {
    if (!value) {
        return "";
    }

    return String(value)
        .normalize("NFD")
        .replace(
            /[\u0300-\u036f]/g,
            ""
        )
        .trim()
        .toUpperCase();
}

export default MainLayout;