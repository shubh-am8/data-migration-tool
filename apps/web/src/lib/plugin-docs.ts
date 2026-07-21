/** In-app docs slug for a marketplace plugin card. */
export function pluginDocSlug(pluginId: string): string {
  switch (pluginId) {
    case "postgresql":
      return "connectors-overview";
    case "lab-devtools":
      return "development";
    default:
      return "marketplace";
  }
}
