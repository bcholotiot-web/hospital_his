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

                    {/*
                     * Administración de usuarios.
                     *
                     * Se conserva por rol ADMIN porque
                     * todavía no existen permisos USER_*.
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
                     * Catálogo de sucursales.
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
                     * Catálogo de especialidades.
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
                     * Especialidades disponibles
                     * por sede.
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
                     * Asignación de permisos por rol.
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
                     * Configuración visual de estados
                     * técnicos de cita.
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

                    {/*
                     * Menú del paciente.
                     */}
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

                    {/*
                     * Menú de recepción.
                     */}
                    {isReceptionist && (
                        <Link
                            to="/reception"
                            className="main-sidebar-link"
                        >
                            Recepción de Pacientes
                        </Link>
                    )}

                    {/*
                     * Menú de caja.
                     */}
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

                    {/*
                     * Menú de enfermería.
                     */}
                    {isNursing && (
                        <Link
                            to="/nursing"
                            className="main-sidebar-link"
                        >
                            Signos Vitales
                        </Link>
                    )}

                    {/*
                     * Menú médico.
                     */}
                    {isDoctor && (
                        <>
                            <Link
                                to="/doctor"
                                className="main-sidebar-link"
                            >
                                Consultas Médicas
                            </Link>

                            {hasPermission(
                                "MEDICAL_AGENDA_READ"
                            ) && (
                                    <Link
                                        to="/doctor/agenda"
                                        className="main-sidebar-link"
                                    >
                                        Agenda Médica
                                    </Link>
                                )}
                        </>
                    )}

                    {/*
                     * Menú de laboratorio.
                     */}
                    {isLaboratory && (
                        <Link
                            to="/laboratory"
                            className="main-sidebar-link"
                        >
                            Gestión de Laboratorio
                        </Link>
                    )}

                    {/*
                     * Módulos operativos exclusivos
                     * del rol FARMACIA.
                     */}
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
                        </>
                    )}

                    {/*
                     * CU-15:
                     * La bitácora está disponible para
                     * cualquier rol que posea el permiso.
                     *
                     * Actualmente:
                     * ADMIN y FARMACIA.
                     */}
                    {hasPermission(
                        "INVENTORY_MOVEMENT_READ"
                    ) && (
                            <Link
                                to="/pharmacy/inventory-movements"
                                className="main-sidebar-link"
                            >
                                Bitácora de Inventario
                            </Link>
                        )}

                    {/*
                     * Registro manual de movimientos.
                     *
                     * La interfaz y el backend limitan
                     * los tipos disponibles por rol.
                     */}
                    {hasPermission(
                        "INVENTORY_MOVEMENT_CREATE"
                    ) && (
                            <Link
                                to="/pharmacy/inventory-movements/create"
                                className="main-sidebar-link"
                            >
                                Registrar Movimiento
                            </Link>
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