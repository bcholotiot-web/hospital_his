import { Link } from "react-router-dom";
import "./Home.css";

function Home() {
    return (
        <div className="home-page">
            <header className="home-header">
                <div className="home-logo">
                    <h1>Hospital HIS</h1>
                </div>
                <nav className="home-nav">
                    <a href="#services">Servicios</a>
                    <a href="#specialties">Especialidades</a>
                    <a href="#contact">Contacto</a>
                    <Link to="/login">Iniciar Sesión</Link>
                    <Link to="/register" className="nav-register">Registrarse</Link>
                </nav>
            </header>

            <section className="hero-section">
                <div className="hero-content">
                    <h1>Sistema Informático Hospitalario</h1>

                    <p>Consulta servicios, especialidades médicas y accede al portal para gestionar tus citas de forma rápida y segura.</p>
                    <div className="hero-actions">
                        <Link to="/login" className="btn-primary">
                            Iniciar Sesión
                        </Link>

                        <Link to="/register" className="btn-secondary">
                            Registrarse
                        </Link>

                        <button className="btn-outline">
                            Agendar Cita
                        </button>
                    </div>
                </div>
            </section>

            <section id="services" className="section">
                <h2>Servicios del Hospital</h2>
                <div className="cards-container">
                    <div className="info-card">
                        <h3>Consulta Médica</h3>
                        <p>Atención médica general y especializada para pacientes registrados en el sistema.</p>
                    </div>

                    <div className="info-card">
                        <h3>Laboratorio Clínico</h3>
                        <p>Gestión de órdenes, toma de muestras y consulta de  resultados de laboratorio.</p>
                    </div>

                    <div className="info-card">
                        <h3>Farmacia</h3>
                        <p>Despacho de medicamentos recetados por médicos del hospital.</p>
                    </div>
                </div>
            </section>

            <section id="specialties" className="section section-alt">
                <h2>Especialidades Disponibles</h2>

                <div className="cards-container">
                    <div className="info-card">
                        <h3>Medicina General</h3>
                        <p>Atención inicial, diagnóstico general y seguimiento médico.</p>
                    </div>

                    <div className="info-card">
                        <h3>Cardiología</h3>
                        <p>Evaluación y seguimiento de enfermedades del corazón.</p>
                    </div>

                    <div className="info-card">
                        <h3>Pediatría</h3>
                        <p>Atención médica enfocada en pacientes pediátricos.</p>
                    </div>
                </div>
            </section>

            <section id="contact" className="section">
                <h2>Información del Hospital</h2>

                <div className="contact-box">
                    <p><strong>Horario de atención:</strong> Lunes a viernes de 8:00 a 17:00</p>

                    <p><strong>Ubicación:</strong> Sede Central</p>

                    <p><strong>Teléfono:</strong> 0000-0000</p>
                </div>
            </section>

            <footer className="home-footer">
                <p>Sistema Informático Hospitalario HIS</p>
            </footer>

        </div>
    );
}
export default Home;

/**
 * import { Link } from "react-router-dom";
import "./Home.css";

function Home() {
    return (

        <div className="home-page">
            <header className="home-header">
                <div className="home-logo">
                    Hospital HIS
                </div>

                <nav className="home-nav">
                    #services
                        Servicios
                    </a>

                    #specialties
                        Especialidades
                    </a>

                    #contact
                        Contacto
                    </a>

                    <Link to="/login">
                        Iniciar Sesión
                    </Link>

                    <Link
                        to="/register"
                        className="nav-register"
                    >
                        Registrarse
                    </Link>
                </nav>
            </header>

            <section className="hero-section">
                <div className="hero-content">
                    <h1>
                        Sistema Informático Hospitalario
                    </h1>

                    <p>
                        Consulta servicios, especialidades médicas y accede al portal
                        para gestionar tus citas de forma rápida y segura.
                    </p>

                    <div className="hero-actions">
                        <Link
                            to="/login"
                            className="btn-primary"
                        >
                            Iniciar Sesión
                        </Link>

                        <Link
                            to="/register"
                            className="btn-secondary"
                        >
                            Registrarse
                        </Link>

                        <button className="btn-outline">
                            Agendar Cita
                        </button>
                    </div>
                

            <section
                id="services"
                className="section"
            >
                <h2>Servicios del Hospital</h2>

                <div className="cards-container">
                    <div className="info-card">
                        <h3>Consulta Médica</h3>

                        <p>
                            Atención médica general y especializada para pacientes
                            registrados en el sistema.
                        </p>
                    </div>

                    <div className="info-card">
                        <h3>Laboratorio Clínico</h3>

                        <p>
                            Gestión de órdenes, toma de muestras y consulta de resultados
                            de laboratorio.
                        </p>
                    </div>

                    <div className="info-card">
                        <h3>Farmacia</h3>

                        <p>
                            Despacho de medicamentos recetados por médicos del hospital.
                        </p>
                    </div>
                </div>
            </section>

            <section
                id="specialties"
                className="section section-alt"
            >
                <h2>Especialidades Disponibles</h2>

                <div className="cards-container">
                    <div className="info-card">
                        <h3>Medicina General</h3>

                        <p>
                            Atención inicial, diagnóstico general y seguimiento médico.
                        </p>
                    </div>

                    <div className="info-card">
                        <h3>Cardiología</h3>

                        <p>
                            Evaluación y seguimiento de enfermedades del corazón.
                        </p>
                    </div>

                    <div className="info-card">
                        <h3>Pediatría</h3>

                        <p>
                            Atención médica enfocada en pacientes pediátricos.
                        </p>
                    </div>
                </div>
            </section>

            <section
                id="contact"
                className="section"
            >
                <h2>Información del Hospital</h2>

                <div className="contact-box">
                    <p>
                        <strong>Horario de atención:</strong> Lunes a viernes de 8:00 a 17:00
                    </p>

                    <p>
                        <strong>Ubicación:</strong> Sede Central
                    </p>

                    <p>
                        <strong>Teléfono:</strong> 0000-0000
                    </p>
                </div>
            </section>

            <footer className="home-footer">
                <p>
                    Sistema Informático Hospitalario HIS
                </p>
            </footer>
        </div>
    );
}

export default Home;
 */