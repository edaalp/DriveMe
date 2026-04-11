import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import api, { hasDocumentUrl, openDocumentUrl } from "../api";

export default function DriverDetailPage() {
  const { id } = useParams();
  const [driver, setDriver] = useState(null);
  const [reason, setReason] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  /** Legacy blob preview when API has no public criminal-record URL (old rows). */
  const [criminalBlobUrl, setCriminalBlobUrl] = useState(null);

  const load = () => {
    api.get(`/admin/drivers/${id}`).then((res) => setDriver(res.data));
  };

  useEffect(() => {
    load();
  }, [id]);

  useEffect(() => {
    if (!driver) return;

    if (hasDocumentUrl(driver.criminalRecordDocumentUrl)) {
      setCriminalBlobUrl(null);
      return;
    }

    const urlRef = { current: null };
    let cancelled = false;

    api
      .get(`/admin/drivers/${id}/document/criminal-record`, {
        responseType: "blob",
      })
      .then((res) => {
        if (cancelled) return;
        const blob = new Blob([res.data], {
          type: res.headers["content-type"],
        });
        urlRef.current = URL.createObjectURL(blob);
        setCriminalBlobUrl(urlRef.current);
      })
      .catch(() => {
        if (!cancelled) setCriminalBlobUrl(null);
      });

    return () => {
      cancelled = true;
      if (urlRef.current) {
        URL.revokeObjectURL(urlRef.current);
        urlRef.current = null;
      }
      setCriminalBlobUrl(null);
    };
  }, [driver, id]);

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
  const criminalPreviewSrc = hasDocumentUrl(criminalUrl)
    ? criminalUrl.trim()
    : criminalBlobUrl;

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

      <div className="card" style={{ marginTop: "1rem" }}>
        <h3 style={{ marginBottom: "0.75rem" }}>Documents</h3>
        <p style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>
          Open files in a new browser tab. URLs are served from the API
          <code style={{ marginLeft: "0.35rem" }}>/uploads/</code> path.
        </p>
        <div className="actions-bar" style={{ flexWrap: "wrap", gap: "0.5rem" }}>
          <button
            type="button"
            className="btn btn-primary"
            disabled={!hasDocumentUrl(licenseUrl)}
            onClick={() => openDocumentUrl(licenseUrl)}
          >
            Open license (new tab)
          </button>
          <button
            type="button"
            className="btn btn-primary"
            disabled={!hasDocumentUrl(criminalUrl)}
            onClick={() => openDocumentUrl(criminalUrl)}
          >
            Open criminal record (new tab)
          </button>
        </div>
      </div>

      {driver.criminalRecordFileName && criminalPreviewSrc && (
        <div className="card" style={{ marginTop: "1rem" }}>
          <h3 style={{ marginBottom: "0.75rem" }}>Criminal Record Preview</h3>
          <p style={{ fontSize: "0.85rem", color: "var(--text-muted)" }}>
            {driver.criminalRecordFileName}
          </p>
          <div className="document-preview">
            {driver.criminalRecordFileName?.toLowerCase().endsWith(".pdf") ? (
              <iframe src={criminalPreviewSrc} title="Criminal record" />
            ) : (
              <img src={criminalPreviewSrc} alt="Criminal record" />
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
