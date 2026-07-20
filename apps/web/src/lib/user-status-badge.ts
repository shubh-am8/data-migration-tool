export function presenceBadgeVariant(online: boolean): "success" | "danger" {
  return online ? "success" : "danger";
}

export function revokedBadgeVariant(): "warning" {
  return "warning";
}
