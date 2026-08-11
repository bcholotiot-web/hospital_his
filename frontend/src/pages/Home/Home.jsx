import { Link } from "react-router-dom";

function Home() {
    return (
        <div>
            <h1>Hospital HIS</h1>
            <p>
                Sistema Integral Hospitalario
            </p>
            <Link to="/login">
                Iniciar Sesión
            </Link>
        </div>
    );
}

export default Home;