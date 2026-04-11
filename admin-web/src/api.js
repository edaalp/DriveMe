import axios from "axios";

/** Backend origin (no /api) — used for opening public upload URLs in a new tab. */
export const API_ORIGIN = "http://localhost:8080";

const API_BASE = `${API_ORIGIN}/api`;

const api = axios.create({
  baseURL: API_BASE,
});

/** Opens absolute or backend-relative document URLs in a new browser tab. */
export function openDocumentUrl(url) {
  if (typeof url !== "string" || !url.trim()) return;
  let href = url.trim();
  if (href.startsWith("/")) {
    href = `${API_ORIGIN}${href}`;
  }
  window.open(href, "_blank", "noopener,noreferrer");
}

export function hasDocumentUrl(url) {
  return typeof url === "string" && url.trim().length > 0;
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("admin_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      localStorage.removeItem("admin_token");
      localStorage.removeItem("admin_user");
      window.location.href = "/login";
    }
    return Promise.reject(err);
  }
);

export default api;
