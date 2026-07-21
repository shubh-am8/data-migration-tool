import { notifyConnectionTestResult } from "./connection-test";
import { notify } from "./notify";

jest.mock("./notify", () => ({
  notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() },
}));

beforeEach(() => {
  jest.clearAllMocks();
});

describe("notifyConnectionTestResult", () => {
  it("notifies error when success is false", () => {
    notifyConnectionTestResult({ success: false, message: "timeout", latencyMs: 1 });
    expect(notify.error).toHaveBeenCalledWith("Connection test failed", "timeout");
  });

  it("notifies success when success is true", () => {
    notifyConnectionTestResult({ success: true, message: "Connected", latencyMs: 12 });
    expect(notify.success).toHaveBeenCalledWith("Connection OK", "Connected");
  });

  it("notifies warning when message contains warn (case-insensitive)", () => {
    notifyConnectionTestResult({
      success: true,
      message: "Connected with WARNings",
      latencyMs: 5,
    });
    expect(notify.warning).toHaveBeenCalledWith(
      "Connection test warning",
      "Connected with WARNings"
    );
  });
});
