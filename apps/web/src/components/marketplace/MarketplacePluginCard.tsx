import { DocLink } from "@/components/shared/DocLink";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { pluginDocSlug } from "@/lib/plugin-docs";

export interface MarketplacePluginCardProps {
  id: string;
  name: string;
  description: string;
  version: string;
  kind?: string;
  installed?: boolean;
  onClasspath?: boolean;
  onInstall?: () => void;
  onUninstall?: () => void;
  onAdd?: () => void;
}

export function MarketplacePluginCard({
  id,
  name,
  description,
  version,
  kind = "CONNECTOR",
  installed,
  onClasspath,
  onInstall,
  onUninstall,
  onAdd,
}: MarketplacePluginCardProps) {
  const isTool = kind === "TOOL";
  const docSlug = pluginDocSlug(id);

  return (
    <Card className="flex h-full min-w-0 flex-col">
      <CardHeader className="gap-3">
        <div className="flex min-w-0 flex-col gap-2">
          <CardTitle className="truncate">{name}</CardTitle>
          <div className="flex flex-wrap gap-1">
            <Badge variant="outline" className="capitalize">
              {kind.toLowerCase()}
            </Badge>
            <Badge variant="secondary">{version}</Badge>
            {installed ? (
              <Badge>Installed</Badge>
            ) : (
              <Badge variant="outline">Available</Badge>
            )}
          </div>
        </div>
        <CardDescription className="line-clamp-3">{description}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-1 flex-col gap-1">
        <p className="truncate font-mono text-xs text-muted-foreground">Plugin ID: {id}</p>
        {onClasspath === false && !installed && (
          <p className="text-xs text-muted-foreground">Install to load the connector JAR</p>
        )}
      </CardContent>
      <CardFooter className="mt-auto flex flex-wrap items-center justify-between gap-2 border-t bg-muted/50">
        <DocLink slug={docSlug}>Docs</DocLink>
        <div className="flex flex-wrap gap-2">
          {!installed && onInstall && (
            <Button size="sm" variant="success" onClick={onInstall}>
              Install
            </Button>
          )}
          {installed && !isTool && onUninstall && (
            <Button size="sm" variant="warning" onClick={onUninstall}>
              Uninstall
            </Button>
          )}
          {installed && !isTool && onAdd && (
            <Button size="sm" onClick={onAdd}>
              Add Connection
            </Button>
          )}
        </div>
      </CardFooter>
    </Card>
  );
}
