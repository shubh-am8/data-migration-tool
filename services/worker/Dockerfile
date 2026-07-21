FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY packages/connector-sdk/pom.xml packages/connector-sdk/
COPY services/api/pom.xml services/api/
COPY services/worker/pom.xml services/worker/
COPY packages packages
COPY services/worker services/worker
RUN mvn -pl services/worker -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/services/worker/target/*.jar app.jar
# Connectors are not bundled in this image — install via the Marketplace at runtime
# (worker reads app.plugins.dir/installed, shared with the API's plugins volume).
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
