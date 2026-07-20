import { postLoginDestination } from "./login-redirect";

describe("postLoginDestination", () => {
  it("sends authenticated users to dashboard", () => {
    expect(postLoginDestination({ authenticated: true })).toBe("/dashboard");
  });
  it("keeps anonymous users on login", () => {
    expect(postLoginDestination({ authenticated: false })).toBeNull();
  });
});
