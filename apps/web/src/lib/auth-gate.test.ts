import { isPublicPath, shouldRedirectToLogin } from "./auth-gate";

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
});
