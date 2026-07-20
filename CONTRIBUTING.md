# Contributing

Thank you for your interest in the Data Migration Platform.

## Prerequisites

- **Java 21** and Maven 3.9+
- **Node.js 22+** and npm
- **Docker Desktop** (PostgreSQL + Redis for local dev)

## Local development

```bash
cp .env.example .env
./run-local-dev.sh
```

Open http://localhost:3000

For auth-free local API access, set `AUTH_ENFORCED=false` in `.env`. See
[docs/development.md](docs/development.md) for script flags, ports, and
troubleshooting.

## Pull request checklist

- [ ] Changes build and tests pass (`mvn test`, `cd apps/web && npm test`)
- [ ] No secrets, `.env`, or `.pnpm-store` committed
- [ ] Backend changes follow [.cursor/rules/backend-patterns.mdc](.cursor/rules/backend-patterns.mdc)
- [ ] UI changes follow [.cursor/rules/ui-patterns.mdc](.cursor/rules/ui-patterns.mdc)
- [ ] Connector/marketplace changes follow [.cursor/rules/connectors-marketplace.mdc](.cursor/rules/connectors-marketplace.mdc)
- [ ] Docs updated when behavior or configuration changes

## Secrets and local files

Do **not** commit:

- `.env` or other credential files
- `.pnpm-store/` (local pnpm cache)
- `*.pem` keys or production env files (see `.gitignore`)

## License

This repository is licensed under [Apache-2.0](LICENSE). The Next.js app in
`apps/web/package.json` keeps `"private": true` so it is not published to npm;
the source remains open source under the same license.

## Code of conduct

Please read [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before participating.

## Security

Report vulnerabilities via [SECURITY.md](SECURITY.md) — do not open public issues
for security bugs.
