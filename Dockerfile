ARG MODULE
ARG APP_JAR

FROM maven:3.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /workspace
COPY . .
RUN mvn -f ${MODULE}/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
ARG MODULE
ARG APP_JAR
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/${APP_JAR} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
