export type ConnectionTestStatus = "idle" | "running" | "passed" | "failed";

/**
 * @param needsTest true when config is new or changed since load
 * @param passedFingerprint fingerprint that last passed test; must match current when needsTest
 */
export function canSaveConnection(
  needsTest: boolean,
  testStatus: ConnectionTestStatus,
  passedFingerprint: string | null,
  currentFingerprint: string
): boolean {
  if (testStatus === "failed") return false;
  if (!needsTest) return true;
  return testStatus === "passed" && passedFingerprint === currentFingerprint;
}
