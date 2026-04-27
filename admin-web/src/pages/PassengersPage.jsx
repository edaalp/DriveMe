import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api, {
  hasDocumentUrl,
  openDocumentUrl,
  getPassengerProfilePictureUrl,
  getPassengerTcPhotoFrontUrl,
  getPassengerTcPhotoBackUrl
} from "../api";

const STATUSES = ["ALL", "PENDING", "VERIFIED", "REJECTED"];

export default function PassengersPage() {
  const [passengers, setPassengers] = useState([]);
  const [filter, setFilter] = useState("PENDING");
  const [loading, setLoading] = useState(true);
  const [actioningId, setActioningId] = useState(null);
  const [reason, setReason] = useState("");
  const [actionLoading, setActionLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    const params = filter === "ALL" ? {} : { status: filter };
    api
      .get("/admin/passengers", { params })
      .then((res) => setPassengers(res.data))
      .finally(() => setLoading(false));
  }, [filter]);

  const handleVerify = async (id, decision) => {
    if (decision === "REJECTED" && !reason.trim()) {
      alert("Please provide a reason for rejection.");
      return;
    }
    setActionLoading(true);
    try {
      await api.put(`/admin/passengers/${id}/verify`, { decision, reason });

      // Remove the passenger from the list if they no longer match the current filter
      if (filter !== "ALL" && filter !== decision) {
        // If we're filtering by status and the passenger's new status doesn't match, remove them
        setPassengers(passengers.filter((p) => p.id !== id));
      } else {
        // Otherwise update the passenger in the list
        setPassengers(
          passengers.map((p) =>
            p.id === id
              ? { ...p, verificationStatus: decision, rejectionReason: reason }
              : p
          )
        );
      }

      setActioningId(null);
      setReason("");
    } catch (err) {
      alert(err.response?.data?.message || "Action failed");
    } finally {
      setActionLoading(false);
    }
  };

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
                <th>TC No</th>
                <th>Selfie</th>
                <th>TC Front</th>
                <th>TC Back</th>
                <th>Status</th>
                <th>Actions</th>
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
                      onClick={() => openDocumentUrl(getPassengerProfilePictureUrl(p.id))}
                    >
                      View
                    </button>
                  </td>
                  <td onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline"
                      onClick={() => openDocumentUrl(getPassengerTcPhotoFrontUrl(p.id))}
                    >
                      View
                    </button>
                  </td>
                  <td onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline"
                      onClick={() => openDocumentUrl(getPassengerTcPhotoBackUrl(p.id))}
                    >
                      View
                    </button>
                  </td>
                  <td>
                    <span
                      className={`badge badge-${p.verificationStatus?.toLowerCase()}`}
                    >
                      {p.verificationStatus}
                    </span>
                  </td>
                  <td onClick={(e) => e.stopPropagation()}>
                    {p.verificationStatus === "PENDING" ? (
                      <div>
                        {actioningId === p.id ? (
                          <div style={{ display: "flex", gap: "0.25rem", flexDirection: "column", alignItems: "flex-start" }}>
                            <input
                              type="text"
                              placeholder="Reason for rejection"
                              value={reason}
                              onChange={(e) => setReason(e.target.value)}
                              style={{ width: "150px", padding: "0.25rem", fontSize: "0.8rem" }}
                              onKeyDown={(e) => {
                                if (e.key === "Escape") setActioningId(null);
                              }}
                            />
                            <div style={{ display: "flex", gap: "0.25rem" }}>
                              <button
                                className="btn btn-sm btn-success"
                                onClick={() => handleVerify(p.id, "VERIFIED")}
                                disabled={actionLoading}
                              >
                                Approve
                              </button>
                              <button
                                className="btn btn-sm btn-danger"
                                onClick={() => handleVerify(p.id, "REJECTED")}
                                disabled={actionLoading}
                              >
                                Reject
                              </button>
                              <button
                                className="btn btn-sm btn-outline"
                                onClick={() => setActioningId(null)}
                                disabled={actionLoading}
                              >
                                Cancel
                              </button>
                            </div>
                          </div>
                        ) : (
                          <button
                            className="btn btn-sm btn-outline"
                            onClick={() => setActioningId(p.id)}
                          >
                            Review
                          </button>
                        )}
                      </div>
                    ) : (
                      <span style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>—</span>
                    )}
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
