import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import api from "../api";

function isPdfPath(url) {
  if (typeof url !== "string" || !url.trim()) return false;
  const path = url.trim().split("?")[0].toLowerCase();
  return path.endsWith(".pdf");
}

function useBlobFetch(passenger, id, endpoint) {
  const [blobUrl, setBlobUrl] = React.useState(null);
  React.useEffect(() => {
    if (!passenger || !id) return;
    let cancelled = false;
    let objUrl = null;
    api
      .get(`/admin/passengers/${id}/document/${endpoint}`, { responseType: "blob" })
      .then((res) => {
        if (cancelled) return;
        const blob = new Blob([res.data], { type: res.headers["content-type"] });
        objUrl = URL.createObjectURL(blob);
        setBlobUrl(objUrl);
      })
      .catch(() => {
        if (!cancelled) setBlobUrl(null);
      });
    return () => {
      cancelled = true;
      if (objUrl) URL.revokeObjectURL(objUrl);
      setBlobUrl(null);
    };
  }, [passenger, id, endpoint]);
  return blobUrl;
}

export default function PassengerDetailPage() {
  const { id } = useParams();
  const [passenger, setPassenger] = useState(null);
  const [reason, setReason] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  const load = () => {
    api.get(`/admin/passengers/${id}`).then((res) => setPassenger(res.data));
  };

  useEffect(() => {
    load();
  }, [id]);

  const profileBlobUrl = useBlobFetch(passenger, id, "profile-picture");
  const identityBlobUrl = useBlobFetch(passenger, id, "identity-document");

  const handleVerify = async (decision) => {
    if (decision === "REJECTED" && !reason.trim()) {
      alert("Please provide a reason for rejection.");
      return;
    }
    setActionLoading(true);
    try {
      await api.put(`/admin/passengers/${id}/verify`, { decision, reason });
      load();
      setReason("");
    } catch (err) {
      alert(err.response?.data?.message || "Action failed");
    } finally {
      setActionLoading(false);
    }
  };

  if (!passenger) return <div className="loading">Loading...</div>;

  const profileUrl = passenger.profilePictureUrl;
  const identityUrl = passenger.identityDocumentUrl;

  return (
    <div>
      <Link to="/passengers" className="back-link">
        &larr; Back to Passengers
      </Link>

      <div className="detail-header">
        <h1>{passenger.fullName}</h1>
        <span className={`badge badge-${passenger.verificationStatus?.toLowerCase()}`}>
          {passenger.verificationStatus}
        </span>
      </div>

      {profileBlobUrl && (
        <div className="card" style={{ marginTop: "1rem" }}>
          <h3 style={{ marginBottom: "0.75rem" }}>Profile photo</h3>
          <div className="document-preview" style={{ maxWidth: "320px" }}>
            <img
              src={profileBlobUrl}
              alt="Passenger profile"
              style={{
                width: "100%",
                borderRadius: "8px",
                objectFit: "cover",
              }}
            />
          </div>
        </div>
      )}

      <div className="card">
        <div className="info-grid">
          <div className="info-item">
            <div className="label">Email</div>
            <div className="value">{passenger.email}</div>
          </div>
          <div className="info-item">
            <div className="label">Phone</div>
            <div className="value">{passenger.phoneNumber}</div>
          </div>
          {passenger.userName ? (
            <div className="info-item">
              <div className="label">Username</div>
              <div className="value">{passenger.userName}</div>
            </div>
          ) : null}
          <div className="info-item">
            <div className="label">Account active</div>
            <div className="value">{passenger.active ? "Yes" : "No"}</div>
          </div>
        </div>

        {passenger.rejectionReason && (
          <div className="error-msg">
            <strong>Rejection reason:</strong> {passenger.rejectionReason}
          </div>
        )}
      </div>

      {identityBlobUrl && (
        <div className="card" style={{ marginTop: "1rem" }}>
          <h3 style={{ marginBottom: "0.75rem" }}>Identity document preview</h3>
          {passenger.identityDocumentFileName && (
            <p style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>
              {passenger.identityDocumentFileName}
            </p>
          )}
          <div className="document-preview">
            {isPdfPath(identityUrl) ||
            passenger.identityDocumentFileName?.toLowerCase().endsWith(".pdf") ? (
              <iframe src={identityBlobUrl} title="Identity document" />
            ) : (
              <img src={identityBlobUrl} alt="Identity document" />
            )}
          </div>
        </div>
      )}

      <div className="card" style={{ marginTop: "1rem" }}>
        <h3 style={{ marginBottom: "0.75rem" }}>Documents</h3>
        <div className="actions-bar" style={{ flexWrap: "wrap", gap: "0.5rem" }}>
          <button
            type="button"
            className="btn btn-primary"
            disabled={!profileBlobUrl}
            onClick={() => profileBlobUrl && window.open(profileBlobUrl, "_blank")}
          >
            Open profile photo (new tab)
          </button>
          <button
            type="button"
            className="btn btn-primary"
            disabled={!identityBlobUrl}
            onClick={() => identityBlobUrl && window.open(identityBlobUrl, "_blank")}
          >
            Open identity document (new tab)
          </button>
        </div>
      </div>

      {passenger.verificationStatus === "PENDING" && (
        <div className="card" style={{ marginTop: "1rem" }}>
          <h3 style={{ marginBottom: "0.75rem" }}>Verification decision</h3>
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
