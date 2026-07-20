import { Suspense } from "react";
import NewConnectionClient from "./client";

export default function NewConnectionPage() {
  return (
    <Suspense fallback={<div className="p-6">Loading…</div>}>
      <NewConnectionClient />
    </Suspense>
  );
}
