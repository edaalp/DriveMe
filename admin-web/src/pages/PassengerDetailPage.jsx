import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import api, {
  getPassengerProfilePictureUrl,
  getPassengerTcPhotoFrontUrl,
  getPassengerTcPhotoBackUrl,
  openDocumentUrl,
  fetchDocumentBlobUrl
} from "../api";

function useProtectedDocument(url) {
  const [blobUrl, setBlobUrl] = useState("");

  useEffect(() => {
    if (!url) return;
    let cancelled = false;
    let objectUrl = "";

    fetchDocumentBlobUrl(url)
      .then((nextUrl) => {
        if (cancelled) {
          if (nextUrl) URL.revokeObjectURL(nextUrl);
          return;
        }
        objectUrl = nextUrl;
        setBlobUrl(nextUrl);
      })
      .catch(() => {
        if (!cancelled) setBlobUrl("");
      });

    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
      setBlobUrl("");
    };
  }, [url]);

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

  const profileUrl = getPassengerProfilePictureUrl(id);
  const tcFrontUrl = getPassengerTcPhotoFrontUrl(id);
  const tcBackUrl = getPassengerTcPhotoBackUrl(id);
  const profileBlobUrl = useProtectedDocument(profileUrl);
  const tcFrontBlobUrl = useProtectedDocument(tcFrontUrl);
  const tcBackBlobUrl = useProtectedDocument(tcBackUrl);

  if (!passenger) return <div className="loading">Loading...</div>;

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

      {/* Profile / Selfie */}
      <div className="card" style={{ marginTop: "1rem" }}>
        <h3 style={{ marginBottom: "0.75rem" }}>Profile photo (selfie)</h3>
        <div className="document-preview" style={{ maxWidth: "320px" }}>
          <img
            src={profileBlobUrl}
            alt="Passenger selfie"
            style={{ width: "100%", borderRadius: "8px", objectFit: "cover" }}
            onError={(e) => { e.target.style.display = 'none'; e.target.nextElementSibling.style.display = 'block'; }}
          />
          <p style={{ display: 'none', color: 'var(--text-muted)' }}>No profile picture available</p>
        </div>
        <div className="actions-bar" style={{ marginTop: "0.75rem" }}>
          <button
            type="button"
            className="btn btn-primary"
            disabled={!profileBlobUrl}
            onClick={() => openDocumentUrl(profileUrl)}
          >
            Open in new tab
          </button>
        </div>
      </div>

      {/* Info grid */}
      <div className="card" style={{ marginTop: "1rem" }}>
        <div className="info-grid">
          <div className="info-item">
            <div className="label">Email</div>
            <div className="value">{passenger.email}</div>
          </div>
          <div className="info-item">
            <div className="label">Phone</div>
            <div className="value">{passenger.phoneNumber}</div>
          </div>
          <div className="info-item">
            <div className="label">TC No</div>
            <div className="value">{passenger.tcNo || "—"}</div>
          </div>
        </div>
      </div>

      {/* TC identity card front */}
      <div className="card" style={{ marginTop: "1rem" }}>
        <h3 style={{ marginBottom: "0.75rem" }}>TC Identity Card — Front</h3>
        <div className="document-preview">
          <img
            src={tcFrontBlobUrl}
            alt="TC card front"
            style={{ width: "100%", borderRadius: "8px" }}
            onError={(e) => { e.target.style.display = 'none'; e.target.nextElementSibling.style.display = 'block'; }}
          />
          <p style={{ display: 'none', color: 'var(--text-muted)' }}>No TC front photo available</p>
        </div>
        <div className="actions-bar" style={{ marginTop: "0.75rem" }}>
          <button
            type="button"
            className="btn btn-primary"
            disabled={!tcFrontBlobUrl}
            onClick={() => openDocumentUrl(tcFrontUrl)}
          >
            Open in new tab
          </button>
        </div>
      </div>

      {/* TC identity card back */}
      <div className="card" style={{ marginTop: "1rem" }}>
        <h3 style={{ marginBottom: "0.75rem" }}>TC Identity Card — Back</h3>
        <div className="document-preview">
          <img
            src={tcBackBlobUrl}
            alt="TC card back"
            style={{ width: "100%", borderRadius: "8px" }}
            onError={(e) => { e.target.style.display = 'none'; e.target.nextElementSibling.style.display = 'block'; }}
          />
          <p style={{ display: 'none', color: 'var(--text-muted)' }}>No TC back photo available</p>
        </div>
        <div className="actions-bar" style={{ marginTop: "0.75rem" }}>
          <button
            type="button"
            className="btn btn-primary"
            disabled={!tcBackBlobUrl}
            onClick={() => openDocumentUrl(tcBackUrl)}
          >
            Open in new tab
          </button>
        </div>
      </div>

      {passenger.rejectionReason && (
        <div className="card" style={{ marginTop: "1rem" }}>
          <div className="error-msg">
            <strong>Rejection reason:</strong> {passenger.rejectionReason}
          </div>
        </div>
      )}

      {passenger.verificationStatus === "PENDING" && (
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
