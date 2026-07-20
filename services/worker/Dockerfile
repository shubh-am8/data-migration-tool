FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY packages/connector-sdk/pom.xml packages/connector-sdk/
COPY connectors/postgresql/pom.xml connectors/postgresql/
COPY services/api/pom.xml services/api/
COPY services/worker/pom.xml services/worker/
COPY packages packages
COPY connectors connectors
COPY services/worker services/worker
RUN mvn -pl services/worker -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/services/worker/target/*.jar app.jar
# Worker only reads app.plugins.dir/installed (API seeds bundled/); this seed dir is kept for
# parity in case the worker's plugins dir isn't a shared volume with the API's.
COPY --from=build /build/connectors/postgresql/target/postgresql-connector-*.jar /app/plugins-seed/
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
