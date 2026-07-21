FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY packages/connector-sdk/pom.xml packages/connector-sdk/
COPY services/api/pom.xml services/api/
COPY services/worker/pom.xml services/worker/
COPY packages packages
COPY services/api services/api
RUN mvn -pl services/api -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/services/api/target/*.jar app.jar
# Connectors are not bundled in this image — install via the Marketplace at runtime
# (see marketplace/, PluginDirectoryService#ensureBundledPostgresql).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
