import { Link } from "react-router-dom";

function MainLayout({ children }) {

    return (
        <div style={{
            display: "flex",
            minHeight: "100vh"
        }}>

            <aside
                style={{
                    width: "250px",
                    background: "#1f2937",
                    color: "white",
                    padding: "20px"
                }}
            >

                <h2>Hospital HIS</h2>

                <nav>

                    <p>
                        <Link
                            to="/dashboard"
                            style={{ color: "white" }}
                        >
                            Dashboard
                        </Link>
                    </p>

                    <p>
                        <Link
                            to="/users"
                            style={{ color: "white" }}
                        >
                            Usuarios
                        </Link>
                    </p>

                    <p>
                        <Link
                            to="/branches"
                            style={{ color: "white" }}
                        >
                            Sucursales
                        </Link>
                    </p>

                    <p>
                        <Link
                            to="/specialties"
                            style={{ color: "white" }}
                        >
                            Especialidades
                        </Link>
                    </p>

                </nav>

            </aside>

            <main
                style={{
                    flex: 1,
                    padding: "20px"
                }}
            >
                {children}
            </main>
        </div>
    );
}

export default MainLayout;