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
import LaboratoryOrders from "../pages/Laboratory/LaboratoryOrders";
import LaboratoryPaymentPage from "../pages/LaboratoryPayment/LaboratoryPaymentPage";
import PharmacyDashboard from "../pages/Pharmacy/PharmacyDashboard";
import PharmacyDispensationPage from "../pages/Pharmacy/PharmacyDispensationPage";
import FollowUpAppointmentWizard from "../pages/Appointments/FollowUpAppointmentWizard";
import Roles from "../pages/Roles/Roles";
import RolePermissions from "../pages/RolePermissions/RolePermissions";
import LaboratoryTests from "../pages/LaboratoryTests/LaboratoryTests";
import Medications from "../pages/Medications/Medications";
import PharmacyInventory from "../pages/Pharmacy/PharmacyInventory";
import InventoryMovements from "../pages/Pharmacy/InventoryMovements";
import AppointmentStatuses from "../pages/AppointmentStatuses/AppointmentStatuses";
import CreateInventoryMovement from "../pages/Pharmacy/CreateInventoryMovement";
import InventoryMovementDetail from "../pages/Pharmacy/InventoryMovementDetail";
import MedicalAgenda from "../pages/MedicalAgenda/MedicalAgenda";
/*
 * CU-13:
 * Administración de especialidades disponibles
 * en cada sede.
 */
import BranchSpecialtyList from "../pages/BranchSpecialty/BranchSpecialtyList";

import BranchSpecialtyForm
    from "../pages/BranchSpecialty/BranchSpecialtyForm";


function AppRouter() {

    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
                <Route path="/users" element={<PrivateRoute allowedRoles={["ADMIN", "Administrador"]}><Users /></PrivateRoute>} />
                <Route path="/branches" element={<PrivateRoute requiredPermission="BRANCH_READ"><Branches /></PrivateRoute>} />
                <Route path="/specialties" element={<PrivateRoute requiredPermission="SPECIALTY_READ"><Specialties /></PrivateRoute>} />

                {/*
                * CU-13:
                * Listado administrativo de asignaciones
                * entre sedes y especialidades.
                */}
                <Route path="/branch-specialties" element={<PrivateRoute requiredPermission="BRANCH_SPECIALTY_READ"><BranchSpecialtyList /></PrivateRoute>} />

                {/*
                * CU-13:
                * Formulario para crear o reactivar
                * una asignación.
                */}
                <Route path="/branch-specialties/create" element={<PrivateRoute requiredPermission="BRANCH_SPECIALTY_CREATE"><BranchSpecialtyForm /></PrivateRoute>} />
                <Route path="/appointments/new" element={<PrivateRoute allowedRoles={["PACIENTE", "Paciente"]}> <AppointmentWizard /></PrivateRoute>} />
                <Route path="/payments/success" element={<PrivateRoute allowedRoles={["PACIENTE", "Paciente"]}><PaymentConfirmation /></PrivateRoute>} />
                <Route path="/payments/:appointmentId" element={<PrivateRoute allowedRoles={["PACIENTE", "Paciente"]}><PaymentPage /></PrivateRoute>} />
                <Route path="/appointments" element={<PrivateRoute allowedRoles={["PACIENTE", "Paciente"]}><MyAppointments /></PrivateRoute>} />
                <Route path="/reception" element={<PrivateRoute allowedRoles={["RECEPCIONISTA", "Recepcionista"]}><Reception /></PrivateRoute>} />
                <Route path="/cashier" element={<PrivateRoute allowedRoles={["CAJERO", "Cajero"]}><Cashier /></PrivateRoute>} />
                <Route path="/nursing" element={<PrivateRoute allowedRoles={["ENFERMERIA", "Enfermeria", "Enfermería"]}><NursingDashboard /></PrivateRoute>} />
                <Route path="/doctor" element={<PrivateRoute allowedRoles={["MEDICO", "Medico", "Médico"]}><DoctorDashboard /></PrivateRoute>} />
                <Route path="/laboratory" element={<PrivateRoute allowedRoles={["LABORATORIO", "Laboratorio"]}><LaboratoryOrders /></PrivateRoute>} />
                <Route path="/cashier/laboratory-payments" element={<PrivateRoute allowedRoles={["CAJERO", "Cajero"]}><LaboratoryPaymentPage /></PrivateRoute>} />
                <Route path="/pharmacy" element={<PrivateRoute allowedRoles={["FARMACIA"]} requiredPermission="INVENTORY_READ"><PharmacyDashboard /></PrivateRoute>} />
                <Route path="/pharmacy/dispensations" element={<PrivateRoute allowedRoles={["FARMACIA", "Farmacia"]}><PharmacyDispensationPage /></PrivateRoute>} />
                <Route path="/doctor/follow-up/:consultationId" element={<PrivateRoute allowedRoles={["MEDICO", "Medico", "Médico"]}><FollowUpAppointmentWizard /></PrivateRoute>} />
                <Route path="/roles" element={<PrivateRoute requiredPermission="ROLE_READ"><Roles /></PrivateRoute>} />
                <Route path="/role-permissions" element={<PrivateRoute requiredPermission="PERMISSION_READ"><RolePermissions /></PrivateRoute>} />
                <Route path="/admin/laboratory-tests" element={<PrivateRoute requiredPermission="LABORATORY_TEST_READ"><LaboratoryTests /></PrivateRoute>} />
                <Route path="/admin/medications" element={<PrivateRoute requiredPermission="MEDICATION_READ"><Medications /></PrivateRoute>} />
                <Route path="/pharmacy/inventory" element={<PrivateRoute allowedRoles={["FARMACIA"]} requiredPermission="INVENTORY_READ"><PharmacyInventory /></PrivateRoute>} />
                <Route path="/pharmacy/inventory-movements" element={<PrivateRoute allowedRoles={["ADMIN", "FARMACIA"]} requiredPermission="INVENTORY_MOVEMENT_READ"><InventoryMovements /></PrivateRoute>} />
                <Route path="/pharmacy/inventory-movements/create" element={<PrivateRoute allowedRoles={["ADMIN", "FARMACIA"]} requiredPermission="INVENTORY_MOVEMENT_CREATE"><CreateInventoryMovement /></PrivateRoute>} />
                <Route path="/pharmacy/inventory-movements/:movementId" element={<PrivateRoute allowedRoles={["ADMIN", "FARMACIA"]} requiredPermission="INVENTORY_MOVEMENT_READ"><InventoryMovementDetail /></PrivateRoute>} />
                <Route path="/admin/appointment-statuses" element={<PrivateRoute requiredPermission="APPOINTMENT_STATUS_READ"> <AppointmentStatuses /></PrivateRoute>} />
                <Route path="/doctor/agenda" element={<PrivateRoute allowedRoles={["MEDICO"]} requiredPermission="MEDICAL_AGENDA_READ"><MedicalAgenda /></PrivateRoute>} />


            </Routes>
        </BrowserRouter>
    );
}

export default AppRouter;