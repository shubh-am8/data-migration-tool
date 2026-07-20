import { AlertTriangle } from "lucide-react";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { HttpRouteStat, HttpSnapshot } from "@/lib/http-types";

interface RouteLatencyPanelProps {
  http?: HttpSnapshot;
}

function RouteList({ routes }: { routes: HttpRouteStat[] }) {
  if (routes.length === 0) {
    return <p className="text-sm text-muted-foreground">No data yet</p>;
  }
  return (
    <div className="flex flex-col gap-2">
      {routes.map((r) => (
        <div
          key={`${r.method}\0${r.uri}`}
          className="flex items-center justify-between gap-2 rounded-md border p-2 text-sm"
        >
          <div className="flex min-w-0 items-center gap-2">
            <Badge variant="outline">{r.method}</Badge>
            <span className="truncate font-mono">{r.uri}</span>
          </div>
          <span className="shrink-0 text-muted-foreground">
            {r.meanMs.toFixed(0)}ms avg · {r.maxMs.toFixed(0)}ms max
          </span>
        </div>
      ))}
    </div>
  );
}

export function RouteLatencyPanel({ http }: RouteLatencyPanelProps) {
  const slowest = http?.slowest ?? [];
  const fastest = http?.fastest ?? [];
  const choking = http?.choking ?? [];

  return (
    <div className="flex flex-col gap-4">
      {choking.length > 0 && (
        <Alert variant="destructive">
          <AlertTriangle />
          <AlertTitle>{choking.length} route{choking.length > 1 ? "s" : ""} choking</AlertTitle>
          <AlertDescription>
            <RouteList routes={choking} />
          </AlertDescription>
        </Alert>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Slowest routes</CardTitle>
          </CardHeader>
          <CardContent>
            <RouteList routes={slowest} />
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Fastest routes</CardTitle>
          </CardHeader>
          <CardContent>
            <RouteList routes={fastest} />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
