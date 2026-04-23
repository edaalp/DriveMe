import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api, { hasDocumentUrl, openDocumentUrl } from "../api";

export default function PassengersPage() {
  const [passengers, setPassengers] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    api
      .get("/admin/passengers")
      .then((res) => setPassengers(res.data))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <h1 style={{ marginBottom: "1rem" }}>Passengers</h1>

      {loading ? (
        <div className="loading">Loading...</div>
      ) : passengers.length === 0 ? (
        <div className="card" style={{ textAlign: "center", padding: "2rem" }}>
          No passengers found.
        </div>
      ) : (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>TC No</th>
                <th>Selfie</th>
                <th>TC Front</th>
                <th>TC Back</th>
                <th>Active</th>
              </tr>
            </thead>
            <tbody>
              {passengers.map((p) => (
                <tr
                  key={p.id}
                  className="clickable"
                  onClick={() => navigate(`/passengers/${p.id}`)}
                >
                  <td>{p.fullName}</td>
                  <td>{p.email}</td>
                  <td>{p.phoneNumber}</td>
                  <td>{p.tcNo || "—"}</td>
                  <td onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline"
                      disabled={!hasDocumentUrl(p.profilePictureUrl)}
                      onClick={() => openDocumentUrl(p.profilePictureUrl)}
                    >
                      View
                    </button>
                  </td>
                  <td onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline"
                      disabled={!hasDocumentUrl(p.tcPhotoFrontUrl)}
                      onClick={() => openDocumentUrl(p.tcPhotoFrontUrl)}
                    >
                      View
                    </button>
                  </td>
                  <td onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline"
                      disabled={!hasDocumentUrl(p.tcPhotoBackUrl)}
                      onClick={() => openDocumentUrl(p.tcPhotoBackUrl)}
                    >
                      View
                    </button>
                  </td>
                  <td>
                    <span className={`badge badge-${p.active ? "verified" : "rejected"}`}>
                      {p.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
