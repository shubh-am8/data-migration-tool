import { DocLink } from "@/components/shared/DocLink";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";

export function BuildYourOwnCard() {
  return (
    <Card className="flex h-full min-w-0 flex-col">
      <CardHeader className="gap-3">
        <CardTitle>Build your own</CardTitle>
        <CardDescription>
          Implement the ConnectorPlugin SPI, package a JAR, and upload it here.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex-1">
        <p className="text-sm text-muted-foreground">
          Open the guide in-app, or read the same file in the repo under{" "}
          <code className="break-all rounded bg-muted px-1 py-0.5 font-mono text-xs">docs/connectors/</code>.
        </p>
      </CardContent>
      <CardFooter className="mt-auto flex flex-wrap items-center gap-2 border-t bg-muted/50">
        <DocLink slug="adding-a-connector" />
        <DocLink slug="connector-sdk">SDK reference</DocLink>
      </CardFooter>
    </Card>
  );
}
