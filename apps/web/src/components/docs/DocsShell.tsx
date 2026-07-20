"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { DocsNav } from "@/components/docs/DocsNav";
import { ComponentErrorBoundary } from "@/components/shared/ComponentErrorBoundary";

export function DocsShell({
  children,
  searchSlot,
}: {
  children: React.ReactNode;
  searchSlot?: React.ReactNode;
}) {
  const [open, setOpen] = useState(false);

  const navPanel = (
    <div className="flex flex-col gap-4">
      {searchSlot}
      <DocsNav onNavigate={() => setOpen(false)} />
    </div>
  );

  return (
    <div className="flex min-h-0 flex-1 flex-col md:flex-row md:gap-6">
      <div className="flex items-center justify-between border-b pb-3 md:hidden">
        <p className="text-sm font-medium">Documentation</p>
        <Sheet open={open} onOpenChange={setOpen}>
          <SheetTrigger render={<Button variant="outline" size="sm">Menu</Button>} />
          <SheetContent side="left" className="w-64 p-4">
            <SheetHeader>
              <SheetTitle>Documentation</SheetTitle>
            </SheetHeader>
            <div className="mt-4 overflow-y-auto">{navPanel}</div>
          </SheetContent>
        </Sheet>
      </div>

      <aside className="hidden w-64 shrink-0 overflow-y-auto md:flex md:w-[25%] md:max-w-xs md:flex-col">
        {navPanel}
      </aside>

      <div className="flex min-w-0 flex-1 flex-col gap-6 pt-4 md:pt-0">
        <ComponentErrorBoundary fallbackTitle="Could not load this page">
          {children}
        </ComponentErrorBoundary>
      </div>
    </div>
  );
}
