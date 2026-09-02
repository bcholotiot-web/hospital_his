import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "../pages/Home/Home";
import Login from "../pages/Login/Login";
import Dashboard from "../pages/Dashboard/Dashboard";
import Users from "../pages/Users/Users";
import Branches from "../pages/Branches/Branches";
import Specialties from "../pages/Specialties/Specialties";
import Register from "../pages/Register/Register";
import PrivateRoute from "./PrivateRoute";
import AppointmentWizard from "../pages/Appointments/AppointmentWizard";
import PaymentPage from "../pages/Payments/PaymentPage";
import PaymentConfirmation from "../pages/Payments/PaymentConfirmation";
import MyAppointments from "../pages/Appointments/MyAppointments";
import Reception from "../pages/Reception/Reception";
import Cashier from "../pages/Cashier/Cashier";
import NursingDashboard from "../pages/Nursing/NursingDashboard";
import DoctorDashboard from "../pages/MedicalConsultation/DoctorDashboard";

function AppRouter() {

    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
                <Route path="/users" element={<PrivateRoute allowedRoles={["ADMIN", "Administrador"]}><Users /></PrivateRoute>} />
                <Route path="/branches" element={<PrivateRoute><Branches /></PrivateRoute>} />
                <Route path="/specialties" element={<PrivateRoute><Specialties /></PrivateRoute>} />
                <Route path="/appointments/new" element={<PrivateRoute allowedRoles={["PACIENTE", "Paciente"]}> <AppointmentWizard /></PrivateRoute>} />
                <Route path="/payments/success" element={<PrivateRoute allowedRoles={["PACIENTE", "Paciente"]}><PaymentConfirmation /></PrivateRoute>} />
                <Route path="/payments/:appointmentId" element={<PrivateRoute allowedRoles={["PACIENTE", "Paciente"]}><PaymentPage /></PrivateRoute>} />
                <Route path="/appointments" element={<PrivateRoute allowedRoles={["PACIENTE", "Paciente"]}><MyAppointments /></PrivateRoute>} />
                <Route path="/reception" element={<PrivateRoute allowedRoles={["RECEPCIONISTA", "Recepcionista"]}><Reception /></PrivateRoute>} />
                <Route path="/cashier" element={<PrivateRoute allowedRoles={["CAJERO", "Cajero"]}><Cashier /></PrivateRoute>} />
                <Route path="/nursing" element={<PrivateRoute allowedRoles={["ENFERMERIA", "Enfermeria", "Enfermería"]}><NursingDashboard /></PrivateRoute>} />
                <Route path="/doctor" element={<PrivateRoute allowedRoles={["MEDICO", "Medico", "Médico"]}><DoctorDashboard /></PrivateRoute>} />
            </Routes>
        </BrowserRouter>
    );
}

export default AppRouter;