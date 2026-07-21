const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    ...options,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });
  if (res.status === 401) {
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
    throw new Error("Unauthorized");
  }
  if (!res.ok) {
    const text = await res.text();
    if (text.startsWith("{")) {
      try {
        const json = JSON.parse(text) as { error?: string; message?: string };
        const msg = json.error ?? json.message;
        if (msg) throw new Error(msg);
      } catch (e) {
        if (e instanceof Error && !(e instanceof SyntaxError)) throw e;
      }
    }
    throw new Error(text || `API error ${res.status}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

export function loginWithGoogle() {
  window.location.href = `${API_URL}/oauth2/authorization/google`;
}

export async function logout() {
  const res = await fetch(`${API_URL}/api/auth/logout`, {
    method: "POST",
    credentials: "include",
  });
  if (res.status === 401 || !res.ok) {
    // still clear client session
  }
  window.location.href = "/login";
}

export { API_URL };
