import {
    Link,
    useNavigate
} from "react-router-dom";

import {
    useAuth
} from "../context/AuthContext";

import "./MainLayout.css";

function MainLayout({
    children
}) {
    const navigate =
        useNavigate();

    const {
        loading,
        logout,
        hasRole,
        hasPermission
    } = useAuth();

    /**
     * Roles operativos.
     */
    const isAdmin =
        hasRole("ADMIN") ||
        hasRole("ADMINISTRADOR");

    const isPatient =
        hasRole("PACIENTE");

    const isReceptionist =
        hasRole("RECEPCIONISTA");

    const isCashier =
        hasRole("CAJERO");

    const isNursing =
        hasRole("ENFERMERIA");

    const isDoctor =
        hasRole("MEDICO");

    const isLaboratory =
        hasRole("LABORATORIO");

    const isPharmacy =
        hasRole("FARMACIA");

    /**
     * Cerrar sesión utilizando AuthContext.
     */
    const handleLogout = () => {
        logout();

        navigate(
            "/login",
            {
                replace: true
            }
        );
    };

    if (loading) {
        return (
            <div
                style={{
                    minHeight: "100vh",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center"
                }}
            >
                Cargando sesión...
            </div>
        );
    }

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

                    {/*
                     * Administración de usuarios.
                     *
                     * Todavía se conserva por rol ADMIN
                     * porque no hemos creado permisos
                     * USER_READ, USER_CREATE, etc.
                     */}
                    {isAdmin && (
                        <Link
                            to="/users"
                            className="main-sidebar-link"
                        >
                            Usuarios
                        </Link>
                    )}

                    {/*
                     * Catálogo administrativo
                     * de sucursales.
                     */}
                    {hasPermission(
                        "BRANCH_READ"
                    ) && (
                            <Link
                                to="/branches"
                                className="main-sidebar-link"
                            >
                                Sucursales
                            </Link>
                        )}

                    {/*
                     * Catálogo administrativo
                     * de especialidades.
                     */}
                    {hasPermission(
                        "SPECIALTY_READ"
                    ) && (
                            <Link
                                to="/specialties"
                                className="main-sidebar-link"
                            >
                                Especialidades
                            </Link>
                        )}

                    {/*
                     * CU-13:
                     * Especialidades disponibles
                     * por sucursal.
                     */}
                    {hasPermission(
                        "BRANCH_SPECIALTY_READ"
                    ) && (
                            <Link
                                to="/branch-specialties"
                                className="main-sidebar-link"
                            >
                                Especialidades por Sede
                            </Link>
                        )}

                    {/*
                     * Administración de roles.
                     */}
                    {hasPermission(
                        "ROLE_READ"
                    ) && (
                            <Link
                                to="/roles"
                                className="main-sidebar-link"
                            >
                                Roles
                            </Link>
                        )}

                    {/*
                     * Administración de permisos
                     * asignados a roles.
                     */}
                    {hasPermission(
                        "PERMISSION_READ"
                    ) && (
                            <Link
                                to="/role-permissions"
                                className="main-sidebar-link"
                            >
                                Permisos por Rol
                            </Link>
                        )}

                    {/*
                     * Catálogo administrativo
                     * de exámenes.
                     */}
                    {hasPermission(
                        "LABORATORY_TEST_READ"
                    ) && (
                            <Link
                                to="/admin/laboratory-tests"
                                className="main-sidebar-link"
                            >
                                Exámenes de Laboratorio
                            </Link>
                        )}

                    {/*
                     * Catálogo administrativo
                     * de medicamentos.
                     */}
                    {hasPermission(
                        "MEDICATION_READ"
                    ) && (
                            <Link
                                to="/admin/medications"
                                className="main-sidebar-link"
                            >
                                Medicamentos
                            </Link>
                        )}

                    {/*
                     * Configuración visual de los
                     * estados técnicos de cita.
                     */}
                    {hasPermission(
                        "APPOINTMENT_STATUS_READ"
                    ) && (
                            <Link
                                to="/admin/appointment-statuses"
                                className="main-sidebar-link"
                            >
                                Estados de Cita
                            </Link>
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
                        <>
                            <Link
                                to="/cashier"
                                className="main-sidebar-link"
                            >
                                Cobro en Caja
                            </Link>

                            <Link
                                to="/cashier/laboratory-payments"
                                className="main-sidebar-link"
                            >
                                Cobros de Laboratorio
                            </Link>
                        </>
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
                            {hasPermission(
                                "INVENTORY_READ"
                            ) && (
                                    <>
                                        <Link
                                            to="/pharmacy"
                                            className="main-sidebar-link"
                                        >
                                            Panel de Farmacia
                                        </Link>

                                        <Link
                                            to="/pharmacy/inventory"
                                            className="main-sidebar-link"
                                        >
                                            Inventario
                                        </Link>
                                    </>
                                )}

                            <Link
                                to="/pharmacy/dispensations"
                                className="main-sidebar-link"
                            >
                                Despacho de Medicamentos
                            </Link>

                            {hasPermission(
                                "INVENTORY_MOVEMENT_READ"
                            ) && (
                                    <Link
                                        to="/pharmacy/inventory-movements"
                                        className="main-sidebar-link"
                                    >
                                        Movimientos de Inventario
                                    </Link>
                                )}
                        </>
                    )}
                </nav>

                <div className="main-sidebar-footer">
                    <button
                        type="button"
                        className="logout-button"
                        onClick={
                            handleLogout
                        }
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