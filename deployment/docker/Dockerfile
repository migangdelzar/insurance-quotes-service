FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY platform/pom.xml platform/
COPY service/pom.xml service/
COPY platform platform
COPY service service
RUN mvn -q -pl service -am package -DskipTests

FROM eclipse-temurin:17-jre
RUN groupadd --system app && useradd --system --gid app app
USER app
WORKDIR /app
COPY --from=build /workspace/service/target/service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
