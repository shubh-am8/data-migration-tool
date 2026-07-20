import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";

interface ConnectorCardProps {
  id: string;
  name: string;
  description: string;
  version: string;
  installed?: boolean;
  onClasspath?: boolean;
  onInstall?: () => void;
  onUninstall?: () => void;
  onAdd: () => void;
}

export function ConnectorCard({
  id,
  name,
  description,
  version,
  installed,
  onClasspath,
  onInstall,
  onUninstall,
  onAdd,
}: ConnectorCardProps) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between gap-2">
          <CardTitle>{name}</CardTitle>
          <div className="flex gap-1">
            <Badge variant="secondary">{version}</Badge>
            {installed ? <Badge>Installed</Badge> : <Badge variant="outline">Available</Badge>}
          </div>
        </div>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-1">
        <p className="text-xs text-muted-foreground">Plugin ID: {id}</p>
        {onClasspath === false && !installed && (
          <p className="text-xs text-muted-foreground">Install to load the connector JAR</p>
        )}
      </CardContent>
      <CardFooter className="flex flex-wrap gap-2">
        {!installed && onInstall && (
          <Button variant="success" onClick={onInstall}>
            Install
          </Button>
        )}
        {installed && onUninstall && (
          <Button variant="warning" onClick={onUninstall}>
            Uninstall
          </Button>
        )}
        {installed && (
          <Button onClick={onAdd}>Add Connection</Button>
        )}
      </CardFooter>
    </Card>
  );
}
