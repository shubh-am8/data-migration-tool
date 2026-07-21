import { notify } from "./notify";

export type ConnectionTestResult = {
  success: boolean;
  message: string;
  latencyMs: number;
};

export function notifyConnectionTestResult(result: ConnectionTestResult): void {
  if (/warn/i.test(result.message)) {
    notify.warning("Connection test warning", result.message);
    return;
  }
  if (result.success) {
    notify.success("Connection OK", result.message);
    return;
  }
  notify.error("Connection test failed", result.message);
}
