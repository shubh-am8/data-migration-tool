# Build API + Worker
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY packages packages
COPY services services
RUN mvn -pl services/api,services/worker -am package -DskipTests -q

# Build Web
FROM node:22-alpine AS build-web
WORKDIR /app
COPY apps/web/package*.json ./
RUN npm install
COPY apps/web .
ENV NEXT_PUBLIC_API_URL=
RUN npm run build

# Runtime
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends supervisor debian-keyring debian-archive-keyring apt-transport-https curl \
    && curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg \
    && curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | tee /etc/apt/sources.list.d/caddy-stable.list \
    && apt-get update && apt-get install -y --no-install-recommends caddy \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /var/log/supervisor /tmp /etc/caddy

WORKDIR /app
COPY --from=build /build/services/api/target/migration-api-*.jar /app/api.jar
COPY --from=build /build/services/worker/target/migration-worker-*.jar /app/worker.jar
# Connectors are not bundled in this image — install via the Marketplace at runtime
# (see marketplace/, PluginDirectoryService#ensureBundledPostgresql).
COPY --from=build-web /app/.next/standalone /app/web
COPY --from=build-web /app/.next/static /app/web/.next/static
COPY --from=build-web /app/public /app/web/public
COPY infra/Caddyfile /etc/caddy/Caddyfile
COPY scripts/supervisord.conf /app/supervisord.conf
COPY scripts/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

EXPOSE 80
ENTRYPOINT ["/app/docker-entrypoint.sh"]
