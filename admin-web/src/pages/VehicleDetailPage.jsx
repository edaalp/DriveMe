import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import api from "../api";

export default function VehicleDetailPage() {
  const { id } = useParams();
  const [vehicle, setVehicle] = useState(null);
  const [reason, setReason] = useState("");
  const [actionLoading, setActionLoading] = useState(false);
  const [docUrl, setDocUrl] = useState(null);

  const load = () => {
    api.get(`/admin/vehicles/${id}`).then((res) => setVehicle(res.data));
  };

  useEffect(() => {
    load();
    api
      .get(`/admin/vehicles/${id}/document`, { responseType: "blob" })
      .then((res) => {
        const blob = new Blob([res.data], {
          type: res.headers["content-type"],
        });
        setDocUrl(URL.createObjectURL(blob));
      })
      .catch(() => setDocUrl(null));
  }, [id]);

  const handleVerify = async (decision) => {
    if (decision === "REJECTED" && !reason.trim()) {
      alert("Please provide a reason for rejection.");
      return;
    }
    setActionLoading(true);
    try {
      await api.put(`/admin/vehicles/${id}/verify`, { decision, reason });
      load();
      setReason("");
    } catch (err) {
      alert(err.response?.data?.message || "Action failed");
    } finally {
      setActionLoading(false);
    }
  };

  if (!vehicle) return <div className="loading">Loading...</div>;

  return (
    <div>
      <Link to="/vehicles" className="back-link">
        &larr; Back to Vehicles
      </Link>

      <div className="detail-header">
        <h1>
          {vehicle.brand} {vehicle.model} {vehicle.year}
        </h1>
        <span className={`badge badge-${vehicle.status?.toLowerCase()}`}>
          {vehicle.status}
        </span>
      </div>

      <div className="card">
        <div className="info-grid">
          <div className="info-item">
            <div className="label">Plate Number</div>
            <div className="value">{vehicle.plateNumber}</div>
          </div>
          <div className="info-item">
            <div className="label">Brand</div>
            <div className="value">{vehicle.brand}</div>
          </div>
          <div className="info-item">
            <div className="label">Model</div>
            <div className="value">{vehicle.model}</div>
          </div>
          <div className="info-item">
            <div className="label">Year</div>
            <div className="value">{vehicle.year}</div>
          </div>
          <div className="info-item">
            <div className="label">Transmission</div>
            <div className="value">{vehicle.transmission}</div>
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: "1rem" }}>
        <h3 style={{ marginBottom: "0.75rem" }}>Owner Information</h3>
        <div className="info-grid">
          <div className="info-item">
            <div className="label">Full Name</div>
            <div className="value">{vehicle.ownerFullName || "—"}</div>
          </div>
          <div className="info-item">
            <div className="label">Email</div>
            <div className="value">{vehicle.ownerEmail || "—"}</div>
          </div>
          <div className="info-item">
            <div className="label">Phone</div>
            <div className="value">{vehicle.ownerPhoneNumber || "—"}</div>
          </div>
        </div>

        {vehicle.rejectionReason && (
          <div className="error-msg">
            <strong>Rejection reason:</strong> {vehicle.rejectionReason}
          </div>
        )}
      </div>

      {vehicle.hasDocument && (
        <div className="card" style={{ marginTop: "1rem" }}>
          <h3 style={{ marginBottom: "0.75rem" }}>Document</h3>
          <p style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>
            {vehicle.documentFileName}
          </p>
          <div className="document-preview">
            {vehicle.documentFileName?.toLowerCase().endsWith(".pdf") ? (
              <iframe src={docUrl} title="Document preview" />
            ) : (
              <img src={docUrl} alt="Vehicle document" />
            )}
          </div>
        </div>
      )}

      {vehicle.status === "PENDING" && (
        <div className="card" style={{ marginTop: "1rem" }}>
          <h3 style={{ marginBottom: "0.75rem" }}>Verification Decision</h3>
          <div className="form-group">
            <label>Reason (required for rejection)</label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Enter reason..."
            />
          </div>
          <div className="actions-bar">
            <button
              className="btn btn-success"
              onClick={() => handleVerify("VERIFIED")}
              disabled={actionLoading}
            >
              Approve
            </button>
            <button
              className="btn btn-danger"
              onClick={() => handleVerify("REJECTED")}
              disabled={actionLoading}
            >
              Reject
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
