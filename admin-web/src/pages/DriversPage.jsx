import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api, { hasDocumentUrl, openDocumentUrl } from "../api";

const STATUSES = ["ALL", "PENDING", "VERIFIED", "REJECTED"];

export default function DriversPage() {
  const [drivers, setDrivers] = useState([]);
  const [filter, setFilter] = useState("PENDING");
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    const params = filter === "ALL" ? {} : { status: filter };
    api
      .get("/admin/drivers", { params })
      .then((res) => setDrivers(res.data))
      .finally(() => setLoading(false));
  }, [filter]);

  return (
    <div>
      <h1 style={{ marginBottom: "1rem" }}>Drivers</h1>

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
      ) : drivers.length === 0 ? (
        <div className="card" style={{ textAlign: "center", padding: "2rem" }}>
          No drivers found.
        </div>
      ) : (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>License #</th>
                <th>Rating</th>
                <th>License PDF</th>
                <th>Criminal PDF</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {drivers.map((d) => (
                <tr
                  key={d.id}
                  className="clickable"
                  onClick={() => navigate(`/drivers/${d.id}`)}
                >
                  <td>{d.fullName}</td>
                  <td>{d.email}</td>
                  <td>{d.phoneNumber}</td>
                  <td>{d.driverLicenseNumber || d.licenseNumber}</td>
                  <td>{d.avgRating?.toFixed(1)}</td>
                  <td onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline"
                      disabled={!hasDocumentUrl(d.driverLicenseDocumentUrl)}
                      title={
                        hasDocumentUrl(d.driverLicenseDocumentUrl)
                          ? "Open license document"
                          : "No license file"
                      }
                      onClick={() => openDocumentUrl(d.driverLicenseDocumentUrl)}
                    >
                      PDF
                    </button>
                  </td>
                  <td onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline"
                      disabled={!hasDocumentUrl(d.criminalRecordDocumentUrl)}
                      title={
                        hasDocumentUrl(d.criminalRecordDocumentUrl)
                          ? "Open criminal record"
                          : "No criminal record file"
                      }
                      onClick={() =>
                        openDocumentUrl(d.criminalRecordDocumentUrl)
                      }
                    >
                      PDF
                    </button>
                  </td>
                  <td>
                    <span
                      className={`badge badge-${d.verificationStatus?.toLowerCase()}`}
                    >
                      {d.verificationStatus}
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
