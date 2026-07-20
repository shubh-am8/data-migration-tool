import type { LucideIcon } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StatCard } from "@/components/dashboard/StatCard";

interface ServiceHealthCardProps {
  name: string;
  status: string;
  icon?: LucideIcon;
  stats: Array<{ title: string; value: string | number }>;
  note?: string;
}

export function ServiceHealthCard({ name, status, icon: Icon, stats, note }: ServiceHealthCardProps) {
  const isDown = status.toUpperCase() === "DOWN";
  return (
    <Card className="min-w-0">
      <CardHeader className="flex flex-row items-center justify-between gap-2">
        <CardTitle className="flex min-w-0 items-center gap-2 truncate">
          {Icon && <Icon className="size-4 shrink-0 text-muted-foreground" aria-hidden />}
          <span className="truncate">{name}</span>
        </CardTitle>
        <Badge variant={isDown ? "danger" : "success"} className="max-w-[8rem] shrink-0 truncate rounded-full">
          {status}
        </Badge>
      </CardHeader>
      <CardContent className="grid gap-2">
        {stats.map((s) => (
          <StatCard key={s.title} title={s.title} value={s.value} />
        ))}
        {note && <p className="text-sm text-muted-foreground">{note}</p>}
      </CardContent>
    </Card>
  );
}
