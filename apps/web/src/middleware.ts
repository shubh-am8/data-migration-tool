import { NextRequest, NextResponse } from "next/server";

const PUBLIC = ["/login", "/oauth2"];

export function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;
  if (PUBLIC.some((p) => pathname.startsWith(p))) {
    return NextResponse.next();
  }
  if (pathname.startsWith("/_next") || pathname === "/favicon.ico") {
    return NextResponse.next();
  }

  if (!req.cookies.get("migration_token")) {
    return NextResponse.redirect(new URL("/login", req.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
