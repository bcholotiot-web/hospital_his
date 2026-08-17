import { Link, useNavigate } from "react-router-dom";

function MainLayout({ children }) {
    const navigate = useNavigate();

    const handleLogout = () => {
        localStorage.removeItem("token");
        navigate("/login");
    };

    return (
        <div
            style={{
                display: "flex",
                minHeight: "100vh",
                backgroundColor: "#f3f4f6"
            }}>
            <aside
                style={{
                    width: "250px",
                    backgroundColor: "#111827",
                    color: "white",
                    padding: "20px"
                }}>
                <h2
                    style={{
                        marginBottom: "30px"
                    }}>
                    Hospital HIS
                </h2>

                <nav>
                    <p>
                        <Link
                            to="/dashboard"
                            style={{
                                color: "white",
                                textDecoration: "none"
                            }}>
                            Dashboard
                        </Link>
                    </p>

                    <p>
                        <Link
                            to="/users"
                            style={{
                                color: "white",
                                textDecoration: "none"
                            }}>
                            Usuarios
                        </Link>
                    </p>

                    <p>
                        <Link
                            to="/branches"
                            style={{
                                color: "white",
                                textDecoration: "none"
                            }}>
                            Sucursales
                        </Link>
                    </p>

                    <p>
                        <Link
                            to="/specialties"
                            style={{
                                color: "white",
                                textDecoration: "none"
                            }}>
                            Especialidades
                        </Link>
                    </p>
                </nav>

                <button
                    type="button"
                    onClick={handleLogout}
                    style={{
                        marginTop: "30px",
                        width: "100%",
                        backgroundColor: "#dc2626",
                        color: "white",
                        border: "none",
                        padding: "10px",
                        borderRadius: "5px",
                        cursor: "pointer"
                    }}>
                    Cerrar Sesión
                </button>
            </aside>

            <main
                style={{
                    flex: 1,
                    padding: "30px"
                }}>
                {children}
            </main>
        </div>
    );
}

export default MainLayout;