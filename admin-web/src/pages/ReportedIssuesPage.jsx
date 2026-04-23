import React, { useEffect, useState } from "react";
import api from "../api";

export default function ReportedIssuesPage() {
  const [issues, setIssues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("all"); // "all" | "open" | "resolved"
  const [resolvingId, setResolvingId] = useState(null);

  useEffect(() => {
    fetchIssues();
  }, []);

  function fetchIssues() {
    setLoading(true);
    api
      .get("/admin/reported-issues")
      .then((res) => setIssues(res.data))
      .finally(() => setLoading(false));
  }

  async function handleResolve(id) {
    setResolvingId(id);
    try {
      await api.put(`/admin/reported-issues/${id}/resolve`);
      setIssues((prev) =>
        prev.map((issue) =>
          issue.id === id ? { ...issue, resolved: true } : issue
        )
      );
    } catch (e) {
      alert("Failed to resolve issue.");
    } finally {
      setResolvingId(null);
    }
  }

  const filtered = issues.filter((issue) => {
    if (filter === "open") return !issue.resolved;
    if (filter === "resolved") return issue.resolved;
    return true;
  });

  const openCount = issues.filter((i) => !i.resolved).length;

  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", gap: "1rem", marginBottom: "1rem" }}>
        <h1 style={{ margin: 0 }}>Reported Issues</h1>
        {openCount > 0 && (
          <span
            style={{
              background: "#ef4444",
              color: "#fff",
              borderRadius: "999px",
              padding: "2px 10px",
              fontSize: "0.8rem",
              fontWeight: 600,
            }}
          >
            {openCount} open
          </span>
        )}
      </div>

      {/* Filter tabs */}
      <div style={{ display: "flex", gap: "0.5rem", marginBottom: "1rem" }}>
        {["all", "open", "resolved"].map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`btn btn-sm ${filter === f ? "" : "btn-outline"}`}
            style={{ textTransform: "capitalize" }}
          >
            {f}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="loading">Loading...</div>
      ) : filtered.length === 0 ? (
        <div className="card" style={{ textAlign: "center", padding: "2rem" }}>
          No {filter === "all" ? "" : filter} issues found.
        </div>
      ) : (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>Reporter</th>
                <th>Role</th>
                <th>Description</th>
                <th>Trip ID</th>
                <th>Submitted</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((issue) => (
                <tr key={issue.id}>
                  <td>{issue.reporterName}</td>
                  <td>
                    <span
                      className={`badge badge-${issue.reporterRole === "DRIVER" ? "verified" : "pending"}`}
                    >
                      {issue.reporterRole}
                    </span>
                  </td>
                  <td style={{ maxWidth: "340px", wordBreak: "break-word" }}>
                    {issue.description}
                  </td>
                  <td style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                    {issue.tripId ? issue.tripId.slice(0, 8) + "…" : "—"}
                  </td>
                  <td style={{ fontSize: "0.8rem", whiteSpace: "nowrap" }}>
                    {new Date(issue.createdAt).toLocaleString()}
                  </td>
                  <td>
                    <span
                      className={`badge badge-${issue.resolved ? "verified" : "rejected"}`}
                    >
                      {issue.resolved ? "Resolved" : "Open"}
                    </span>
                  </td>
                  <td>
                    {!issue.resolved && (
                      <button
                        className="btn btn-sm btn-outline"
                        disabled={resolvingId === issue.id}
                        onClick={() => handleResolve(issue.id)}
                      >
                        {resolvingId === issue.id ? "…" : "Resolve"}
                      </button>
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
