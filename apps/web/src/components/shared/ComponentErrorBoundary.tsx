"use client";

import * as React from "react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";

export class ComponentErrorBoundary extends React.Component<
  { children: React.ReactNode; fallbackTitle?: string },
  { error: Error | null }
> {
  state = { error: null as Error | null };

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  render() {
    if (this.state.error) {
      return (
        <Alert variant="destructive">
          <AlertTitle>{this.props.fallbackTitle ?? "Something went wrong"}</AlertTitle>
          <AlertDescription>{this.state.error.message}</AlertDescription>
          <Button size="sm" variant="outline" onClick={() => this.setState({ error: null })}>
            Try again
          </Button>
        </Alert>
      );
    }
    return this.props.children;
  }
}
