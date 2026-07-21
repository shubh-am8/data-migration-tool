export type NavItem = { href: string; label: string; adminOnly?: boolean };
export type NavSection = { id: string; label: string; items: readonly NavItem[] };

export const NAV_SECTIONS: readonly NavSection[] = [
  {
    id: "platform",
    label: "Platform",
    items: [
      { href: "/dashboard", label: "Dashboard" },
      { href: "/lab", label: "Lab Playground" },
      { href: "/infra", label: "Infra" },
    ],
  },
  {
    id: "work",
    label: "Work",
    items: [
      { href: "/connections", label: "Connections" },
      { href: "/jobs", label: "Jobs" },
      { href: "/workers", label: "Workers" },
      { href: "/connectors/marketplace", label: "Marketplace" },
    ],
  },
  {
    id: "admin",
    label: "Admin",
    items: [
      { href: "/users", label: "Users", adminOnly: true },
      { href: "/settings", label: "Settings" },
    ],
  },
  {
    id: "resources",
    label: "Resources",
    items: [{ href: "/docs", label: "Docs" }],
  },
] as const;

export const NAV_ITEMS: readonly NavItem[] = NAV_SECTIONS.flatMap((s) => [...s.items]);

export function visibleNavSections(admin: boolean): NavSection[] {
  return NAV_SECTIONS.map((section) => ({
    ...section,
    items: section.items.filter((item) => !item.adminOnly || admin),
  })).filter((section) => section.items.length > 0);
}
