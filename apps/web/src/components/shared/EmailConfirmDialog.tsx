"use client";

import { useEffect, useState } from "react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Field, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";

function initials(name: string, email: string) {
  const base = (name || email || "?").trim();
  const parts = base.split(/\s+/).filter(Boolean);
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return base.slice(0, 2).toUpperCase();
}

export type EmailConfirmDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  confirmLabel: string;
  confirmVariant?: "danger" | "warning";
  subject: { name: string; email: string; pictureUrl?: string | null };
  onConfirm: () => void | Promise<void>;
};

export function EmailConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  confirmVariant = "danger",
  subject,
  onConfirm,
}: EmailConfirmDialogProps) {
  const [input, setInput] = useState("");
  const [confirming, setConfirming] = useState(false);

  const matches =
    input.trim().toLowerCase() === subject.email.trim().toLowerCase();

  useEffect(() => {
    if (!open) {
      setInput("");
      setConfirming(false);
    }
  }, [open]);

  async function handleConfirm() {
    if (!matches || confirming) return;
    setConfirming(true);
    try {
      await onConfirm();
    } finally {
      setConfirming(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>

        <div className="flex items-center gap-3">
          <Avatar className="size-10">
            {subject.pictureUrl ? (
              <AvatarImage src={subject.pictureUrl} alt="" />
            ) : null}
            <AvatarFallback>{initials(subject.name, subject.email)}</AvatarFallback>
          </Avatar>
          <div className="min-w-0">
            <p className="truncate font-medium">{subject.name || "—"}</p>
            <p className="truncate text-sm text-muted-foreground">{subject.email}</p>
          </div>
        </div>

        <Field>
          <FieldLabel htmlFor="email-confirm">Type email to confirm</FieldLabel>
          <Input
            id="email-confirm"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={confirming}
            autoComplete="off"
          />
        </Field>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={confirming}
          >
            Cancel
          </Button>
          <Button
            variant={confirmVariant}
            onClick={handleConfirm}
            disabled={!matches || confirming}
          >
            {confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
