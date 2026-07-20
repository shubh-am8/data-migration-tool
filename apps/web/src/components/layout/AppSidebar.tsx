"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { MenuIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { ThemeToggle } from "@/components/theme/ThemeToggle";
import { UserMenu, type ShellUser } from "@/components/layout/UserMenu";
import { useState } from "react";

export const NAV_ITEMS = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/infra", label: "Infra" },
  { href: "/users", label: "Users", adminOnly: true },
  { href: "/connectors/marketplace", label: "Marketplace" },
  { href: "/docs", label: "Docs" },
  { href: "/connections", label: "Connections" },
  { href: "/jobs", label: "Jobs" },
  { href: "/workers", label: "Workers" },
  { href: "/settings", label: "Settings" },
] as const;

function NavLinks({
  pathname,
  admin,
  onNavigate,
}: {
  pathname: string;
  admin: boolean;
  onNavigate?: () => void;
}) {
  return (
    <nav className="flex flex-col gap-1">
      {NAV_ITEMS.filter((item) => !("adminOnly" in item && item.adminOnly) || admin).map((item) => (
        <Link
          key={item.href}
          href={item.href}
          onClick={onNavigate}
          className={cn(
            "rounded-md px-3 py-2 text-sm transition-colors hover:bg-muted",
            pathname.startsWith(item.href) && "bg-muted font-medium"
          )}
        >
          {item.label}
        </Link>
      ))}
    </nav>
  );
}

export function AppSidebar({ user, admin }: { user: ShellUser | null; admin: boolean }) {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  return (
    <>
      <aside className="hidden h-full w-56 shrink-0 flex-col gap-2 border-r bg-muted/30 p-4 md:flex">
        <div className="mb-4 px-2">
          <p className="text-lg font-semibold">Migration Tool</p>
          <p className="text-xs text-muted-foreground">Data transfer platform</p>
        </div>
        <NavLinks pathname={pathname} admin={admin} />
        <div className="mt-auto flex flex-col gap-2 border-t pt-3">
          <div className="flex items-center justify-between px-1">
            <span className="text-xs text-muted-foreground">Theme</span>
            <ThemeToggle />
          </div>
          <UserMenu user={user} />
        </div>
      </aside>

      <div className="flex items-center justify-between border-b p-3 md:hidden">
        <p className="font-semibold">Migration Tool</p>
        <div className="flex items-center gap-1">
          <ThemeToggle />
          <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger
              render={
                <Button variant="ghost" size="icon-sm" aria-label="Open menu">
                  <MenuIcon />
                </Button>
              }
            />
            <SheetContent side="left" className="w-64 p-4">
              <SheetHeader>
                <SheetTitle>Menu</SheetTitle>
              </SheetHeader>
              <div className="mt-4 flex flex-col gap-4">
                <NavLinks pathname={pathname} admin={admin} onNavigate={() => setOpen(false)} />
                <UserMenu user={user} />
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </>
  );
}
