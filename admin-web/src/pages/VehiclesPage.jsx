import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api";

const STATUSES = ["ALL", "PENDING", "VERIFIED", "REJECTED"];

export default function VehiclesPage() {
  const [vehicles, setVehicles] = useState([]);
  const [filter, setFilter] = useState("PENDING");
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    const params = filter === "ALL" ? {} : { status: filter };
    api
      .get("/admin/vehicles", { params })
      .then((res) => setVehicles(res.data))
      .finally(() => setLoading(false));
  }, [filter]);

  return (
    <div>
      <h1 style={{ marginBottom: "1rem" }}>Vehicles</h1>

      <div className="filter-tabs">
        {STATUSES.map((s) => (
          <button
            key={s}
            className={`filter-tab ${filter === s ? "active" : ""}`}
            onClick={() => setFilter(s)}
          >
            {s}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="loading">Loading...</div>
      ) : vehicles.length === 0 ? (
        <div className="card" style={{ textAlign: "center", padding: "2rem" }}>
          No vehicles found.
        </div>
      ) : (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>Plate</th>
                <th>Brand / Model</th>
                <th>Year</th>
                <th>Owner</th>
                <th>Status</th>
                <th>Document</th>
              </tr>
            </thead>
            <tbody>
              {vehicles.map((v) => (
                <tr
                  key={v.id}
                  className="clickable"
                  onClick={() => navigate(`/vehicles/${v.id}`)}
                >
                  <td>{v.plateNumber}</td>
                  <td>
                    {v.brand} {v.model}
                  </td>
                  <td>{v.year}</td>
                  <td>{v.ownerFullName || "—"}</td>
                  <td>
                    <span className={`badge badge-${v.status?.toLowerCase()}`}>
                      {v.status}
                    </span>
                  </td>
                  <td>{v.hasDocument ? "Uploaded" : "Missing"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
