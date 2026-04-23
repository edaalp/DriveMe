import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api, { hasDocumentUrl } from "../api";

const STATUSES = ["ALL", "PENDING", "VERIFIED", "REJECTED"];

export default function PassengersPage() {
  const [passengers, setPassengers] = useState([]);
  const [filter, setFilter] = useState("PENDING");
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const openDocBlob = async (passengerId, endpoint) => {
    try {
      const res = await api.get(`/admin/passengers/${passengerId}/document/${endpoint}`, {
        responseType: "blob",
      });
      const blob = new Blob([res.data], { type: res.headers["content-type"] });
      window.open(URL.createObjectURL(blob), "_blank");
    } catch {
      alert("Document not available");
    }
  };

  useEffect(() => {
    setLoading(true);
    const params = filter === "ALL" ? {} : { status: filter };
    api
      .get("/admin/passengers", { params })
      .then((res) => setPassengers(res.data))
      .finally(() => setLoading(false));
  }, [filter]);

  return (
    <div>
      <h1 style={{ marginBottom: "1rem" }}>Passengers</h1>

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
                <th>Profile</th>
                <th>ID document</th>
                <th>Status</th>
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
                  <td onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline"
                      disabled={!hasDocumentUrl(p.profilePictureUrl)}
                      title={
                        hasDocumentUrl(p.profilePictureUrl)
                          ? "Open profile photo"
                          : "No profile photo on file"
                      }
                      onClick={() => openDocBlob(p.id, "profile-picture")}
                    >
                      View
                    </button>
                  </td>
                  <td onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline"
                      disabled={!hasDocumentUrl(p.identityDocumentUrl)}
                      title={
                        hasDocumentUrl(p.identityDocumentUrl)
                          ? "Open identity document"
                          : "No identity document on file"
                      }
                      onClick={() => openDocBlob(p.id, "identity-document")}
                    >
                      View
                    </button>
                  </td>
                  <td>
                    <span className={`badge badge-${p.verificationStatus?.toLowerCase()}`}>
                      {p.verificationStatus}
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
