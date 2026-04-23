import axios from "axios";

const viteOrigin =
  typeof import.meta !== "undefined" && import.meta.env?.VITE_API_ORIGIN
    ? String(import.meta.env.VITE_API_ORIGIN).trim()
    : "";
/** Backend origin (no /api) — match `app.public-base-url` / DriverMapper. Override with Vite `VITE_API_ORIGIN`. */
export const API_ORIGIN = viteOrigin || "http://localhost:8080";

const API_BASE = `${API_ORIGIN}/api`;

const api = axios.create({
  baseURL: API_BASE,
});

/**
 * Normalizes accidental double origins, e.g. http://localhost:8080http://localhost:8080/uploads/...
 */
function stripDuplicateHttpPrefix(url) {
  const m = url.match(/^(https?:\/\/[^/]+)(https?:\/\/.+)$/i);
  return m ? m[2] : url;
}

/**
 * Resolves upload / asset URLs so the admin browser loads them from the same origin as the API
 * (`API_ORIGIN`, usually http://localhost:8080). Rewrites emulator URLs (10.0.2.2) and any
 * `/uploads/**` absolute URL whose host differs from `API_ORIGIN`.
 */
export function resolveAssetUrl(url) {
  if (typeof url !== "string" || !url.trim()) return "";
  let href = url.trim();
  if (href.startsWith("blob:")) return href;
  href = stripDuplicateHttpPrefix(href);

  if (href.startsWith("http://") || href.startsWith("https://")) {
    try {
      const u = new URL(href);
      const base = new URL(API_ORIGIN);
      const path = `${u.pathname}${u.search}${u.hash}`;
      // Always fetch app uploads from the host the admin uses for /api (browser cannot reach 10.0.2.2)
      if (u.pathname.startsWith("/uploads/")) {
        return `${base.origin}${path}`;
      }
      if (u.hostname === "10.0.2.2") {
        return `${base.protocol}//${base.host}${path}`;
      }
      return href;
    } catch (_) {
      return href;
    }
  }

  // scheme-less: localhost:8080/uploads/... or 127.0.0.1:8080/...
  if (/^(localhost|127\.0\.0\.1)(:\d+)?(\/|$)/i.test(href)) {
    return `http://${href}`;
  }
  if (href.startsWith("/")) return `${API_ORIGIN}${href}`;
  return `${API_ORIGIN}/${href}`;
}

/** Opens absolute or backend-relative document URLs in a new browser tab. */
export function openDocumentUrl(url) {
  let href = url;
  // If it's already a full URL (starts with http), use it directly
  if (!url.startsWith("http")) {
    href = resolveAssetUrl(url);
  }
  if (!href) return;
  window.open(href, "_blank", "noopener,noreferrer");
}

export function hasDocumentUrl(url) {
  return typeof url === "string" && url.trim().length > 0;
}

/**
 * Generates the download URL for a passenger's profile picture from the database.
 * @param {string} passengerId The passenger UUID
 * @returns {string} The API endpoint URL
 */
export function getPassengerProfilePictureUrl(passengerId) {
  return `${API_ORIGIN}/api/passengers/${passengerId}/document/profile-picture`;
}

/**
 * Generates the download URL for a passenger's TC photo front from the database.
 * @param {string} passengerId The passenger UUID
 * @returns {string} The API endpoint URL
 */
export function getPassengerTcPhotoFrontUrl(passengerId) {
  return `${API_ORIGIN}/api/passengers/${passengerId}/document/tc-front`;
}

/**
 * Generates the download URL for a passenger's TC photo back from the database.
 * @param {string} passengerId The passenger UUID
 * @returns {string} The API endpoint URL
 */
export function getPassengerTcPhotoBackUrl(passengerId) {
  return `${API_ORIGIN}/api/passengers/${passengerId}/document/tc-back`;
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
