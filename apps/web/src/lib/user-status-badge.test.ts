import { presenceBadgeVariant, revokedBadgeVariant } from "./user-status-badge";

describe("user status badge variants", () => {
  it("maps online to success (green)", () => {
    expect(presenceBadgeVariant(true)).toBe("success");
  });

  it("maps offline to destructive (red)", () => {
    expect(presenceBadgeVariant(false)).toBe("destructive");
  });

  it("maps revoked to warning (yellow)", () => {
    expect(revokedBadgeVariant()).toBe("warning");
  });
});
