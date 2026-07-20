import { Suspense } from "react";
import MarketplaceClient from "./client";

export default function MarketplacePage() {
  return (
    <Suspense fallback={<div className="p-6">Loading…</div>}>
      <MarketplaceClient />
    </Suspense>
  );
}
