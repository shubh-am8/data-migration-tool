import { toast } from "sonner";

/** App-wide toast helper — use instead of raw sonner calls. */
export const notify = {
  success: (message: string, description?: string) =>
    toast.success(message, description ? { description } : undefined),
  error: (message: string, description?: string) =>
    toast.error(message, description ? { description } : undefined),
  info: (message: string, description?: string) =>
    toast.info(message, description ? { description } : undefined),
  warning: (message: string, description?: string) =>
    toast.warning(message, description ? { description } : undefined),
  loading: (message: string) => toast.loading(message),
};
