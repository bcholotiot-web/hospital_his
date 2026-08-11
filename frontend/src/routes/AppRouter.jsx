import { BrowserRouter, Routes, Route }
    from "react-router-dom";

import Home
    from "../pages/Home/Home";

import Login
    from "../pages/Login/Login";

import Dashboard
    from "../pages/Dashboard/Dashboard";

import Users
    from "../pages/Users/Users";

import Branches
    from "../pages/Branches/Branches";

import Specialties
    from "../pages/Specialties/Specialties";

function AppRouter() {

    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/login" element={<Login />} />
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/users" element={<Users />} />
                <Route path="/branches" element={<Branches />} />
                <Route path="/specialties" element={<Specialties />} />
            </Routes>
        </BrowserRouter>
    );
}

export default AppRouter;