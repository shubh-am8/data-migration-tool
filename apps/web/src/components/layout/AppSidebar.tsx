"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { MenuIcon } from "lucide-react";
import { shellAsideClass } from "@/lib/shell-layout";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { Separator } from "@/components/ui/separator";
import { ThemeToggle } from "@/components/theme/ThemeToggle";
import { UserMenu, type ShellUser } from "@/components/layout/UserMenu";
import { visibleNavSections } from "@/lib/nav-sections";
import { useState } from "react";

function NavLinks({
  pathname,
  admin,
  onNavigate,
}: {
  pathname: string;
  admin: boolean;
  onNavigate?: () => void;
}) {
  const sections = visibleNavSections(admin);
  return (
    <nav className="flex flex-col gap-4" aria-label="Main">
      {sections.map((section, index) => (
        <div key={section.id} className="flex flex-col gap-1">
          {index > 0 ? <Separator className="mb-1" /> : null}
          <p className="px-3 text-xs font-medium uppercase tracking-wide text-muted-foreground">
            {section.label}
          </p>
          {section.items.map((item) => (
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
        </div>
      ))}
    </nav>
  );
}

export function AppSidebar({ user, admin }: { user: ShellUser | null; admin: boolean }) {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  return (
    <>
      <aside className={shellAsideClass}>
        <div className="mb-4 px-2">
          <p className="text-lg font-semibold">Migration Tool</p>
          <p className="text-xs text-muted-foreground">Data transfer platform</p>
        </div>
        <div className="flex-1 overflow-y-auto">
          <NavLinks pathname={pathname} admin={admin} />
        </div>
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
