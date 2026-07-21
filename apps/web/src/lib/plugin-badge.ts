export type PluginBadgeVariant = "success" | "info" | "warning" | "secondary";

export function pluginBadgeVariant(pluginId: string): PluginBadgeVariant {
  const id = pluginId.toLowerCase();
  if (id.includes("postgres")) return "info";
  if (id.includes("mysql") || id.includes("mariadb")) return "success";
  if (id.includes("oracle") || id.includes("mssql") || id.includes("sqlserver")) return "warning";
  return "secondary";
}
