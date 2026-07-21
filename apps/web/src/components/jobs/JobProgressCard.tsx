import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface JobProgressCardProps {
  status: Record<string, unknown>;
}

export function JobProgressCard({ status }: JobProgressCardProps) {
  const phases = (status.phases as Array<{ phase: string; status: string; rowsProcessed: number; rowsTotal: number }>) || [];
  const events = (status.events as Array<{ type: string; createdAt: string }>) || [];
  const runMode = status.runMode as string | undefined;
  const destSchema = status.destSchemaName as string | undefined;
  const destTable = status.destTable as string | undefined;
  const schemaName = status.schemaName as string | undefined;
  const sourceTable = status.sourceTable as string | undefined;

  return (
    <div className="flex flex-col gap-4">
      {runMode === "TEST" && schemaName && sourceTable && (
        <Card>
          <CardHeader><CardTitle>Lab paths</CardTitle></CardHeader>
          <CardContent className="text-sm space-y-1">
            <p>
              Source: <code>{schemaName}.{sourceTable}</code>
            </p>
            {destSchema && destTable ? (
              <p>
                Destination: <code>{destSchema}.{destTable}</code>
              </p>
            ) : (
              <p className="text-muted-foreground">Destination table is provisioned when the job is saved.</p>
            )}
          </CardContent>
        </Card>
      )}
      <div className="grid gap-4 md:grid-cols-2">
        {phases.map((p) => (
          <Card key={p.phase}>
            <CardHeader><CardTitle>{p.phase} Phase</CardTitle></CardHeader>
            <CardContent>
              <p className="text-sm">Status: {p.status}</p>
              <p className="text-sm">Progress: {p.rowsProcessed} / {p.rowsTotal || "—"}</p>
            </CardContent>
          </Card>
        ))}
      </div>
      <Card>
        <CardHeader><CardTitle>Event Log</CardTitle></CardHeader>
        <CardContent className="flex flex-col gap-1">
          {events.map((e, i) => (
            <p key={i} className="text-sm text-muted-foreground">{e.type} — {String(e.createdAt)}</p>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
