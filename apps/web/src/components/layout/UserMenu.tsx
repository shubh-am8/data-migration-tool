"use client";

import { LogOutIcon } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { logout } from "@/lib/api-client";
import { notify } from "@/lib/notify";

export interface ShellUser {
  email?: string;
  name?: string;
  pictureUrl?: string;
}

export function UserMenu({ user }: { user: ShellUser | null }) {
  const label = user?.name || user?.email || "Account";
  const initials = (user?.name || user?.email || "?")
    .split(/[\s@]/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase())
    .join("");

  async function onLogout() {
    try {
      await logout();
      notify.success("Signed out");
    } catch {
      notify.error("Sign out failed");
      window.location.href = "/login";
    }
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="inline-flex items-center gap-2 rounded-lg px-2 py-1 text-sm hover:bg-muted">
        <Avatar className="size-7">
          {user?.pictureUrl ? <AvatarImage src={user.pictureUrl} alt="" /> : null}
          <AvatarFallback>{initials}</AvatarFallback>
        </Avatar>
        <span className="hidden max-w-28 truncate md:inline">{label}</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuGroup>
          <DropdownMenuLabel className="font-normal">
            <div className="flex flex-col gap-0.5">
              <span className="text-sm font-medium">{user?.name || "User"}</span>
              <span className="truncate text-xs text-muted-foreground">{user?.email}</span>
            </div>
          </DropdownMenuLabel>
        </DropdownMenuGroup>
        <DropdownMenuSeparator />
        <DropdownMenuGroup>
          <DropdownMenuItem onClick={onLogout}>
            <LogOutIcon data-icon="inline-start" />
            Log out
          </DropdownMenuItem>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
