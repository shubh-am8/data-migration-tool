import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

interface WorkerThreadTableProps {
  workers: Array<Record<string, unknown>>;
}

export function WorkerThreadTable({ workers }: WorkerThreadTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Worker ID</TableHead>
          <TableHead>Active Threads</TableHead>
          <TableHead>Current Job</TableHead>
          <TableHead>Last Seen</TableHead>
          <TableHead>Status</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {workers.map((w) => (
          <TableRow key={String(w.workerId)}>
            <TableCell>{String(w.workerId)}</TableCell>
            <TableCell>{String(w.activeThreads)}</TableCell>
            <TableCell>{String(w.currentJobId || "—")}</TableCell>
            <TableCell>{String(w.lastSeen)}</TableCell>
            <TableCell>
              <Badge variant={w.online ? "default" : "secondary"}>
                {w.online ? "online" : "offline"}
              </Badge>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
