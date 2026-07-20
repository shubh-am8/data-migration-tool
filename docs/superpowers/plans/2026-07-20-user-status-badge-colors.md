# User Status Badge Colors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Users-table presence badges communicate at a glance: Online = green pill, Offline = red pill, other flags (Revoked) = yellow pill.

**Architecture:** Extend the existing shadcn `Badge` CVA with `success` / `warning` variants that mirror the project's `Button` success/warning colors (emerald / amber). Map Users status cells to those variants. Keep Admin as the default primary badge (role, not presence).

**Tech Stack:** Next.js App Router, shadcn/ui Badge (base-nova), CVA, lucide (unchanged)

## Global Constraints

- Online → green pill (`variant="success"`).
- Offline → red pill (`variant="destructive"`).
- Revoked (and any non-presence status flag) → yellow pill (`variant="warning"`).
- Admin stays `variant="default"` (role badge, not a traffic-light status).
- Badge success/warning colors must match `Button` success/warning: emerald-600 / amber-500 solid pills.
- Prefer Badge `variant` props; do not paint colors via one-off `className` on the Users page.
- Pills stay `rounded-full` (existing Users table pattern).
- Do not change backend online/revoked semantics.
- Smallest diff: Badge variants + Users status column only (Workers page is out of scope unless a one-line reuse is free).

## Code review (pre-plan)

| Severity | Finding |
| --- | --- |
| Important | `apps/web/src/app/users/page.tsx` Online uses `secondary` (gray) and Offline uses `outline` — presence is not color-coded. |
| Important | Revoked already uses `destructive` (red). If Offline becomes red, Revoked and Offline collide; Revoked should move to warning/yellow. |
| Minor | `workers/page.tsx` and `WorkerThreadTable` use the same neutral Online/Offline pattern; leave alone unless this plan expands. |
| Note | `Button` already defines success/warning/danger; Badge should grow matching variants rather than inventing new tokens. |

---

## File structure

| File | Responsibility |
| --- | --- |
| `apps/web/src/components/ui/badge.tsx` | Add `success` + `warning` CVA variants |
| `apps/web/src/lib/user-status-badge.ts` | Pure mapping: online/offline/revoked → Badge variant |
| `apps/web/src/lib/user-status-badge.test.ts` | Assert mapping (Node test / vitest / whatever the web app already uses) |
| `apps/web/src/app/users/page.tsx` | Wire Status column to the mapper + new variants |

---

### Task 1: Badge success/warning variants + status mapper

**Files:**
- Modify: `apps/web/src/components/ui/badge.tsx`
- Create: `apps/web/src/lib/user-status-badge.ts`
- Create: `apps/web/src/lib/user-status-badge.test.ts`
- Modify: `apps/web/src/app/users/page.tsx` (status column only)

**Interfaces:**
- Consumes: existing `Badge` + `AdminUser.online` / `revoked` / `admin`
- Produces:
  - `badgeVariants` gains `success` | `warning`
  - `export type UserPresenceBadge = "online" | "offline"`
  - `export function presenceBadgeVariant(online: boolean): "success" | "destructive"`
  - `export function revokedBadgeVariant(): "warning"` (constant helper so call sites stay typed)

- [ ] **Step 1: Detect how web unit tests run**

Run from repo root:

```bash
cd apps/web && cat package.json | head -80
```

Expected: a `test` script (vitest, node:test, jest, etc.). Use that runner in later steps. If there is **no** test runner, use a self-check file:

```bash
node --experimental-strip-types --test src/lib/user-status-badge.test.ts
```

or add the smallest assert-based self-check the project already uses for libs.

- [ ] **Step 2: Write the failing mapper test**

Create `apps/web/src/lib/user-status-badge.test.ts`:

```ts
import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { presenceBadgeVariant, revokedBadgeVariant } from "./user-status-badge";

describe("user status badge variants", () => {
  it("maps online to success (green)", () => {
    assert.equal(presenceBadgeVariant(true), "success");
  });

  it("maps offline to destructive (red)", () => {
    assert.equal(presenceBadgeVariant(false), "destructive");
  });

  it("maps revoked to warning (yellow)", () => {
    assert.equal(revokedBadgeVariant(), "warning");
  });
});
```

If the project uses vitest instead of `node:test`, rewrite imports to `import { describe, it, expect } from "vitest"` and use `expect(...).toBe(...)`.

- [ ] **Step 3: Run test to verify it fails**

Run the project's test command targeting this file (or `node --test` as above).

Expected: FAIL — module `./user-status-badge` not found (or exports missing).

- [ ] **Step 4: Implement mapper**

Create `apps/web/src/lib/user-status-badge.ts`:

```ts
export function presenceBadgeVariant(online: boolean): "success" | "destructive" {
  return online ? "success" : "destructive";
}

export function revokedBadgeVariant(): "warning" {
  return "warning";
}
```

- [ ] **Step 5: Add Badge variants**

In `apps/web/src/components/ui/badge.tsx`, inside `badgeVariants` → `variants.variant`, add (next to existing variants, matching Button colors):

```ts
        success:
          "border-transparent bg-emerald-600 text-white [a]:hover:bg-emerald-600/90 dark:bg-emerald-500",
        warning:
          "border-transparent bg-amber-500 text-white [a]:hover:bg-amber-500/90",
```

Do not remove existing variants. Do not add custom `className` color overrides on the Users page.

- [ ] **Step 6: Wire Users Status column**

In `apps/web/src/app/users/page.tsx`, import the helpers and replace the status cell:

```tsx
import {
  presenceBadgeVariant,
  revokedBadgeVariant,
} from "@/lib/user-status-badge";
```

Status cell:

```tsx
    {
      id: "status",
      header: "Status",
      cell: (u) => (
        <div className="flex flex-wrap gap-1">
          <Badge variant={presenceBadgeVariant(u.online)} className="rounded-full">
            {u.online ? "Online" : "Offline"}
          </Badge>
          {u.revoked && (
            <Badge variant={revokedBadgeVariant()} className="rounded-full">
              Revoked
            </Badge>
          )}
          {u.admin && <Badge className="rounded-full">Admin</Badge>}
        </div>
      ),
    },
```

- [ ] **Step 7: Run mapper tests — expect PASS**

Re-run the same test command from Step 3.

Expected: PASS (3 tests).

- [ ] **Step 8: Commit**

```bash
git add apps/web/src/components/ui/badge.tsx \
  apps/web/src/lib/user-status-badge.ts \
  apps/web/src/lib/user-status-badge.test.ts \
  apps/web/src/app/users/page.tsx
git commit -m "$(cat <<'EOF'
feat(users): green/red/yellow status badges for presence and revoke

EOF
)"
```

---

## Self-review

1. **Spec coverage:** Online green, Offline red, Revoked yellow, Admin unchanged — Task 1.
2. **Placeholders:** none.
3. **Types:** `presenceBadgeVariant` / `revokedBadgeVariant` match Badge variant keys added in the same task.
