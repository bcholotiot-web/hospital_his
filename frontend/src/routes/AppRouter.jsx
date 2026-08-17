import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "../pages/Home/Home";
import Login from "../pages/Login/Login";
import Dashboard from "../pages/Dashboard/Dashboard";
import Users from "../pages/Users/Users";
import Branches from "../pages/Branches/Branches";
import Specialties from "../pages/Specialties/Specialties";
import Register from "../pages/Register/Register";
import PrivateRoute from "./PrivateRoute";

function AppRouter() {

    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
                <Route path="/users" element={<PrivateRoute><Users /></PrivateRoute>} />
                <Route path="/branches" element={<PrivateRoute><Branches /></PrivateRoute>} />
                <Route path="/specialties" element={<PrivateRoute><Specialties /></PrivateRoute>} />

            </Routes>
        </BrowserRouter>
    );
}

export default AppRouter;