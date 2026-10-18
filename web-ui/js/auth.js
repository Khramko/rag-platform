const TOKEN_KEY = "rag_jwt";
const USER_KEY = "rag_user";

function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

function setToken(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
}

function getUser() {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
}

function clearAuth() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
}

function requireAuth() {
    if (!getToken()) {
        window.location.href = "login.html";
        throw new Error("Not authenticated");
    }
}

async function register(email, password, displayName) {
    const response = await fetch("/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password, displayName }),
    });
    if (!response.ok) {
        const err = await response.json().catch(() => ({}));
        throw new Error(err.detail || err.message || "Registration failed");
    }
    const data = await response.json();
    setToken(data.token, data.user);
    return data;
}

async function login(email, password) {
    const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
    });
    if (!response.ok) {
        const err = await response.json().catch(() => ({}));
        throw new Error(err.detail || err.message || "Login failed");
    }
    const data = await response.json();
    setToken(data.token, data.user);
    return data;
}

function logout() {
    clearAuth();
    window.location.href = "login.html";
}
