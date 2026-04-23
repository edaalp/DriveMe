import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import api, { hasDocumentUrl } from "../api";

function isPdfPath(url) {
  if (typeof url !== "string" || !url.trim()) return false;
  const path = url.trim().split("?")[0].toLowerCase();
  return path.endsWith(".pdf");
}

function useBlobFetch(driver, id, endpoint) {
  const [blobUrl, setBlobUrl] = React.useState(null);
  React.useEffect(() => {
    if (!driver || !id) return;
    let cancelled = false;
    let objUrl = null;
    api
      .get(`/admin/drivers/${id}/document/${endpoint}`, { responseType: "blob" })
      .then((res) => {
        if (cancelled) return;
        const blob = new Blob([res.data], { type: res.headers["content-type"] });
        objUrl = URL.createObjectURL(blob);
        setBlobUrl(objUrl);
      })
      .catch(() => { if (!cancelled) setBlobUrl(null); });
    return () => { cancelled = true; if (objUrl) URL.revokeObjectURL(objUrl); setBlobUrl(null); };
  }, [driver, id]);
  return blobUrl;
}

export default function DriverDetailPage() {
  const { id } = useParams();
  const [driver, setDriver] = useState(null);
  const [reason, setReason] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  const load = () => {
    api.get(`/admin/drivers/${id}`).then((res) => setDriver(res.data));
  };

  useEffect(() => { load(); }, [id]);

  const licenseBlobUrl = useBlobFetch(driver, id, "license");
  const criminalBlobUrl = useBlobFetch(driver, id, "criminal-record");
  const profileBlobUrl = useBlobFetch(driver, id, "profile-picture");

  const handleVerify = async (decision) => {
    if (decision === "REJECTED" && !reason.trim()) {
      alert("Please provide a reason for rejection.");
      return;
    }
    setActionLoading(true);
    try {
      await api.put(`/admin/drivers/${id}/verify`, { decision, reason });
      load();
      setReason("");
    } catch (err) {
      alert(err.response?.data?.message || "Action failed");
    } finally {
      setActionLoading(false);
    }
  };

  if (!driver) return <div className="loading">Loading...</div>;

  const licenseUrl = driver.driverLicenseDocumentUrl;
  const criminalUrl = driver.criminalRecordDocumentUrl;
  const profileUrl = driver.profilePictureUrl;

  return (
    <div>
      <Link to="/drivers" className="back-link">
        &larr; Back to Drivers
      </Link>

      <div className="detail-header">
        <h1>{driver.fullName}</h1>
        <span
          className={`badge badge-${driver.verificationStatus?.toLowerCase()}`}
        >
          {driver.verificationStatus}
        </span>
      </div>

      {profileBlobUrl && (
        <div className="card" style={{ marginTop: "1rem" }}>
          <h3 style={{ marginBottom: "0.75rem" }}>Profile (selfie)</h3>
          <div className="document-preview" style={{ maxWidth: "320px" }}>
            <img
              src={profileBlobUrl}
              alt="Driver profile"
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
            <div className="value">{driver.email}</div>
          </div>
          <div className="info-item">
            <div className="label">Phone</div>
            <div className="value">{driver.phoneNumber}</div>
          </div>
          <div className="info-item">
            <div className="label">License Number</div>
            <div className="value">
              {driver.driverLicenseNumber || driver.licenseNumber}
            </div>
          </div>
          <div className="info-item">
            <div className="label">TCK No</div>
            <div className="value">{driver.tckNo}</div>
          </div>
          <div className="info-item">
            <div className="label">License Issue Date</div>
            <div className="value">
              {driver.licenseIssueDate
                ? new Date(driver.licenseIssueDate).toLocaleDateString()
                : "N/A"}
            </div>
          </div>
          <div className="info-item">
            <div className="label">Avg Rating</div>
            <div className="value">{driver.avgRating?.toFixed(1)}</div>
          </div>
          <div className="info-item">
            <div className="label">Max Pickup Radius</div>
            <div className="value">{driver.maxPickupRadiusKm} km</div>
          </div>
          <div className="info-item">
            <div className="label">Accepts Pets</div>
            <div className="value">{driver.acceptsPets ? "Yes" : "No"}</div>
          </div>
          <div className="info-item">
            <div className="label">Available</div>
            <div className="value">{driver.available ? "Yes" : "No"}</div>
          </div>
        </div>

        {driver.rejectionReason && (
          <div className="error-msg">
            <strong>Rejection reason:</strong> {driver.rejectionReason}
          </div>
        )}
      </div>

      {licenseBlobUrl && (
        <div className="card" style={{ marginTop: "1rem" }}>
          <h3 style={{ marginBottom: "0.75rem" }}>License document preview</h3>
          <div className="document-preview">
            {isPdfPath(licenseUrl) ? (
              <iframe
                src={licenseBlobUrl}
                title="License document"
              />
            ) : (
              <img
                src={licenseBlobUrl}
                alt="License document"
              />
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
            disabled={!licenseBlobUrl}
            onClick={() => licenseBlobUrl && window.open(licenseBlobUrl, "_blank")}
          >
            Open license (new tab)
          </button>
          <button
            type="button"
            className="btn btn-primary"
            disabled={!criminalBlobUrl}
            onClick={() => criminalBlobUrl && window.open(criminalBlobUrl, "_blank")}
          >
            Open criminal record (new tab)
          </button>
        </div>
      </div>

      {criminalBlobUrl && (
        <div className="card" style={{ marginTop: "1rem" }}>
          <h3 style={{ marginBottom: "0.75rem" }}>Criminal Record Preview</h3>
          {driver.criminalRecordFileName && (
            <p style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>
              {driver.criminalRecordFileName}
            </p>
          )}
          <div className="document-preview">
            {isPdfPath(criminalUrl) ||
            driver.criminalRecordFileName?.toLowerCase().endsWith(".pdf") ? (
              <iframe src={criminalBlobUrl} title="Criminal record" />
            ) : (
              <img src={criminalBlobUrl} alt="Criminal record" />
            )}
          </div>
        </div>
      )}

      {driver.verificationStatus === "PENDING" && (
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
