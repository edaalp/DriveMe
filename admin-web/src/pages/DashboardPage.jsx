import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api";

export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    api.get("/admin/stats").then((res) => setStats(res.data));
  }, []);

  if (!stats) return <div className="loading">Loading...</div>;

  return (
    <div>
      <h1 style={{ marginBottom: "1.5rem" }}>Dashboard</h1>
      <div className="stats-grid">
        <div
          className="card stat-card"
          style={{ cursor: "pointer" }}
          onClick={() => navigate("/vehicles")}
        >
          <div className="number">{stats.pendingVehicles}</div>
          <div className="label">Pending Vehicles</div>
        </div>
        <div
          className="card stat-card"
          style={{ cursor: "pointer" }}
          onClick={() => navigate("/drivers")}
        >
          <div className="number">{stats.pendingDrivers}</div>
          <div className="label">Pending Drivers</div>
        </div>
      </div>
    </div>
  );
}
