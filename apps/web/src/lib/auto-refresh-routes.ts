/** Routes where the top-bar auto-refresh control is shown and bumpRefresh is ticked. */
export const AUTO_REFRESH_ROUTES = ["/dashboard", "/infra"] as const;

export function isAutoRefreshRoute(pathname: string): boolean {
  return AUTO_REFRESH_ROUTES.some((route) => pathname === route || pathname.startsWith(`${route}/`));
}
