"use client";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface WizardStepFooterProps {
  onBack?: () => void;
  backLabel?: string;
  children: React.ReactNode;
  className?: string;
}

/** Sticky footer so wizard nav stays visible on small screens. */
export function WizardStepFooter({ onBack, backLabel = "Back", children, className }: WizardStepFooterProps) {
  return (
    <div
      className={cn(
        "sticky bottom-0 z-10 -mx-4 mt-auto flex flex-wrap gap-2 border-t bg-background/95 px-4 py-3 backdrop-blur supports-[backdrop-filter]:bg-background/80 md:-mx-6 md:px-6",
        className
      )}
    >
      {onBack ? (
        <Button type="button" variant="outline" onClick={onBack}>
          {backLabel}
        </Button>
      ) : null}
      {children}
    </div>
  );
}
