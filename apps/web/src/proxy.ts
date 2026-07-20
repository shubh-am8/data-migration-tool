import { NextRequest, NextResponse } from "next/server";
import { shouldRedirectAuthenticatedToApp, shouldRedirectToLogin } from "@/lib/auth-gate";

export function proxy(req: NextRequest) {
  const { pathname } = req.nextUrl;
  const hasToken = Boolean(req.cookies.get("migration_token"));
  if (shouldRedirectAuthenticatedToApp(pathname, hasToken)) {
    return NextResponse.redirect(new URL("/dashboard", req.url));
  }
  if (shouldRedirectToLogin(pathname, hasToken)) {
    return NextResponse.redirect(new URL("/login", req.url));
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
