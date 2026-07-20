import { isPublicPath, shouldRedirectAuthenticatedToApp, shouldRedirectToLogin } from "./auth-gate";

describe("auth-gate", () => {
  it("treats login and oauth as public", () => {
    expect(isPublicPath("/login")).toBe(true);
    expect(isPublicPath("/oauth2/callback")).toBe(true);
  });

  it("skips _next and favicon", () => {
    expect(isPublicPath("/_next/static/chunk.js")).toBe(true);
    expect(isPublicPath("/favicon.ico")).toBe(true);
  });

  it("redirects protected paths without token", () => {
    expect(shouldRedirectToLogin("/jobs/new", false)).toBe(true);
    expect(shouldRedirectToLogin("/jobs/new", true)).toBe(false);
  });

  it("never redirects public paths even without token", () => {
    expect(shouldRedirectToLogin("/login", false)).toBe(false);
  });

  it("sends authenticated users away from login to the app", () => {
    expect(shouldRedirectAuthenticatedToApp("/login", true)).toBe(true);
    expect(shouldRedirectAuthenticatedToApp("/login", false)).toBe(false);
    expect(shouldRedirectAuthenticatedToApp("/dashboard", true)).toBe(false);
  });
});
