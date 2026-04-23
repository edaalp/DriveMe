import React from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext";

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="layout">
      <aside className="sidebar">
        <h2>DriveMe Admin</h2>
        <nav>
          <NavLink to="/" end>
            Dashboard
          </NavLink>
          <NavLink to="/vehicles">Vehicles</NavLink>
          <NavLink to="/drivers">Drivers</NavLink>
          <NavLink to="/passengers">Passengers</NavLink>
          <NavLink to="/reported-issues">Reported Issues</NavLink>
        </nav>
        <div className="sidebar-footer">
          <div style={{ marginBottom: "0.5rem", fontSize: "0.8rem" }}>
            {user?.fullName}
          </div>
          <button onClick={handleLogout}>Log out</button>
        </div>
      </aside>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
