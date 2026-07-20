export function presenceBadgeVariant(online: boolean): "success" | "destructive" {
  return online ? "success" : "destructive";
}

export function revokedBadgeVariant(): "warning" {
  return "warning";
}
