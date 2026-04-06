export const API_URL = import.meta.env.VITE_API_URL || "/api";

function getHeaders(token, hasBody = false) {
  const headers = {};
  if (hasBody) {
    headers["Content-Type"] = "application/json";
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

export async function apiRequest(path, { method = "GET", token, body } = {}) {
  const isFormData = typeof FormData !== "undefined" && body instanceof FormData;
  const response = await fetch(`${API_URL}${path}`, {
    method,
    headers: isFormData ? getHeaders(token, false) : getHeaders(token, body !== undefined),
    body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body)
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ details: ["Request failed"] }));
    throw new Error(error.details?.join(", ") || "Request failed");
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("content-type") || "";
  if (!contentType.includes("application/json")) {
    return null;
  }

  return response.json();
}

export async function openProtectedFile(path, token) {
  const response = await fetch(`${API_URL}${path}`, {
    headers: getHeaders(token, false)
  });

  if (!response.ok) {
    throw new Error("Unable to open attachment");
  }

  const blob = await response.blob();
  const objectUrl = window.URL.createObjectURL(blob);
  window.open(objectUrl, "_blank", "noopener,noreferrer");
  window.setTimeout(() => window.URL.revokeObjectURL(objectUrl), 60_000);
}

export async function downloadProtectedFile(path, token, fallbackFilename) {
  const response = await fetch(`${API_URL}${path}`, {
    headers: getHeaders(token, false)
  });

  if (!response.ok) {
    throw new Error("Unable to download file");
  }

  const blob = await response.blob();
  const objectUrl = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  const contentDisposition = response.headers.get("content-disposition") || "";
  const filenameMatch = contentDisposition.match(/filename=\"?([^"]+)\"?/i);
  link.href = objectUrl;
  link.download = filenameMatch?.[1] || fallbackFilename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => window.URL.revokeObjectURL(objectUrl), 60_000);
}
