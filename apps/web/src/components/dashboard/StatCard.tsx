import type { LucideIcon } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";

import { statToneClasses, type StatTone } from "./stat-card-tone";

interface StatCardProps {
  title: string;
  value: string | number;
  hint?: string;
  tone?: StatTone;
  icon?: LucideIcon;
}

const iconToneClasses: Record<StatTone, string> = {
  default: "text-muted-foreground",
  success: "text-emerald-600",
  warning: "text-amber-500",
  danger: "text-red-600",
  info: "text-sky-600",
};

export function StatCard({
  title,
  value,
  hint,
  tone = "default",
  icon: Icon,
}: StatCardProps) {
  return (
    <Card className={cn(statToneClasses(tone))}>
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
          {Icon && (
            <Icon
              className={cn("size-4 shrink-0", iconToneClasses[tone])}
              aria-hidden
            />
          )}
          {title}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-3xl font-bold">{value}</p>
        {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
      </CardContent>
    </Card>
  );
}
