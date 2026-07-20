export function isPublicPath(pathname: string): boolean {
  if (pathname.startsWith("/_next") || pathname === "/favicon.ico") return true;
  return ["/login", "/oauth2"].some((p) => pathname.startsWith(p));
}

export function shouldRedirectToLogin(pathname: string, hasToken: boolean): boolean {
  if (isPublicPath(pathname)) return false;
  return !hasToken;
}

export function shouldRedirectAuthenticatedToApp(pathname: string, hasToken: boolean): boolean {
  if (!hasToken) return false;
  return pathname === "/login" || pathname.startsWith("/login?");
}
