"use client";

import { useMemo, useState } from "react";
import { CartesianGrid, Line, LineChart, XAxis, YAxis } from "recharts";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart";
import { seriesKeys, type HttpStatusFilter } from "@/lib/http-status-filter";

export interface HttpStatusSample {
  ts?: string;
  http2xx?: number;
  http4xx?: number;
  http5xx?: number;
}

interface HttpStatusChartProps {
  samples: HttpStatusSample[];
}

const chartConfig = {
  http2xx: { label: "2xx", color: "#059669" },
  http4xx: { label: "4xx", color: "#f59e0b" },
  http5xx: { label: "5xx", color: "#dc2626" },
} satisfies ChartConfig;

const FILTERS: Array<{ value: HttpStatusFilter; label: string }> = [
  { value: "all", label: "All" },
  { value: "2xx", label: "2xx" },
  { value: "4xx", label: "4xx" },
  { value: "5xx", label: "5xx" },
];

export function HttpStatusChart({ samples }: HttpStatusChartProps) {
  const [filter, setFilter] = useState<HttpStatusFilter>("all");

  const chartData = useMemo(
    () =>
      samples.map((s) => ({
        t: s.ts ? new Date(s.ts).toLocaleTimeString() : "",
        http2xx: s.http2xx ?? 0,
        http4xx: s.http4xx ?? 0,
        http5xx: s.http5xx ?? 0,
      })),
    [samples]
  );

  const keys = seriesKeys(filter);

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-2">
        <CardTitle>HTTP status</CardTitle>
        <ToggleGroup
          variant="outline"
          size="sm"
          value={[filter]}
          onValueChange={(value) => {
            const next = value[0];
            if (next) setFilter(next as HttpStatusFilter);
          }}
        >
          {FILTERS.map((f) => (
            <ToggleGroupItem key={f.value} value={f.value} aria-label={f.label}>
              {f.label}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </CardHeader>
      <CardContent>
        {chartData.length === 0 ? (
          <p className="text-sm text-muted-foreground">No HTTP samples yet</p>
        ) : (
          <ChartContainer config={chartConfig} className="h-56 w-full">
            <LineChart data={chartData}>
              <CartesianGrid vertical={false} />
              <XAxis dataKey="t" tickLine={false} axisLine={false} minTickGap={32} />
              <YAxis tickLine={false} axisLine={false} width={40} />
              <ChartTooltip content={<ChartTooltipContent />} />
              {keys.map((key) => (
                <Line
                  key={key}
                  type="monotone"
                  dataKey={key}
                  stroke={`var(--color-${key})`}
                  strokeWidth={2}
                  dot={false}
                />
              ))}
            </LineChart>
          </ChartContainer>
        )}
      </CardContent>
    </Card>
  );
}
