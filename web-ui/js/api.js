const API_BASE = "/api";

async function apiFetch(path, options = {}) {
    const token = getToken();
    const headers = {
        ...(options.headers || {}),
    };
    if (token) {
        headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch(API_BASE + path, { ...options, headers });

    if (response.status === 401) {
        clearAuth();
        window.location.href = "login.html";
        throw new Error("Unauthorized");
    }

    if (!response.ok) {
        const errorBody = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorBody}`);
    }

    if (response.status === 204) return null;
    return response.json();
}
