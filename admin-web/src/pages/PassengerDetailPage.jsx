import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import api, { hasDocumentUrl, openDocumentUrl, resolveAssetUrl } from "../api";

export default function PassengerDetailPage() {
  const { id } = useParams();
  const [passenger, setPassenger] = useState(null);

  useEffect(() => {
    api.get(`/admin/passengers/${id}`).then((res) => setPassenger(res.data));
  }, [id]);

  if (!passenger) return <div className="loading">Loading...</div>;

  const { profilePictureUrl, tcPhotoFrontUrl, tcPhotoBackUrl } = passenger;

  return (
    <div>
      <Link to="/passengers" className="back-link">
        &larr; Back to Passengers
      </Link>

      <div className="detail-header">
        <h1>{passenger.fullName}</h1>
        <span className={`badge badge-${passenger.active ? "verified" : "rejected"}`}>
          {passenger.active ? "Active" : "Inactive"}
        </span>
      </div>

      {/* Profile / Selfie */}
      {hasDocumentUrl(profilePictureUrl) && (
        <div className="card" style={{ marginTop: "1rem" }}>
          <h3 style={{ marginBottom: "0.75rem" }}>Profile photo (selfie)</h3>
          <div className="document-preview" style={{ maxWidth: "320px" }}>
            <img
              src={resolveAssetUrl(profilePictureUrl)}
              alt="Passenger selfie"
              style={{ width: "100%", borderRadius: "8px", objectFit: "cover" }}
            />
          </div>
        </div>
      )}

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
      {hasDocumentUrl(tcPhotoFrontUrl) && (
        <div className="card" style={{ marginTop: "1rem" }}>
          <h3 style={{ marginBottom: "0.75rem" }}>TC Identity Card — Front</h3>
          <div className="document-preview">
            <img
              src={resolveAssetUrl(tcPhotoFrontUrl)}
              alt="TC card front"
              style={{ width: "100%", borderRadius: "8px" }}
            />
          </div>
          <div className="actions-bar" style={{ marginTop: "0.75rem" }}>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => openDocumentUrl(tcPhotoFrontUrl)}
            >
              Open in new tab
            </button>
          </div>
        </div>
      )}

      {/* TC identity card back */}
      {hasDocumentUrl(tcPhotoBackUrl) && (
        <div className="card" style={{ marginTop: "1rem" }}>
          <h3 style={{ marginBottom: "0.75rem" }}>TC Identity Card — Back</h3>
          <div className="document-preview">
            <img
              src={resolveAssetUrl(tcPhotoBackUrl)}
              alt="TC card back"
              style={{ width: "100%", borderRadius: "8px" }}
            />
          </div>
          <div className="actions-bar" style={{ marginTop: "0.75rem" }}>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => openDocumentUrl(tcPhotoBackUrl)}
            >
              Open in new tab
            </button>
          </div>
        </div>
      )}

      {/* No documents notice */}
      {!hasDocumentUrl(profilePictureUrl) &&
        !hasDocumentUrl(tcPhotoFrontUrl) &&
        !hasDocumentUrl(tcPhotoBackUrl) && (
          <div className="card" style={{ marginTop: "1rem", textAlign: "center", padding: "2rem" }}>
            <p style={{ color: "var(--text-muted)" }}>No documents uploaded for this passenger.</p>
          </div>
        )}
    </div>
  );
}
