FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY platform/pom.xml platform/
COPY service/pom.xml service/
RUN mvn -q -pl service -am dependency:go-offline
COPY platform platform
COPY service service
RUN mvn -q -pl service -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /workspace/service/target/service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
