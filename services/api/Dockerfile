FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY packages/connector-sdk/pom.xml packages/connector-sdk/
COPY connectors/postgresql/pom.xml connectors/postgresql/
COPY services/api/pom.xml services/api/
COPY services/worker/pom.xml services/worker/
COPY packages packages
COPY connectors connectors
COPY services/api services/api
RUN mvn -pl services/api -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/services/api/target/*.jar app.jar
# Seed dir for bundled connector JARs; PluginDirectoryService copies postgresql*.jar into
# app.plugins.dir/bundled on first boot (see PluginDirectoryService#ensureBundledPostgresql).
COPY --from=build /build/connectors/postgresql/target/postgresql-connector-*.jar /app/plugins-seed/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
