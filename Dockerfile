FROM maven:3.9-eclipse-temurin-21-alpine AS build
ARG SERVICE_NAME
WORKDIR /build
COPY pom.xml .
COPY ${SERVICE_NAME}/pom.xml ${SERVICE_NAME}/
RUN mvn dependency:go-offline -pl ${SERVICE_NAME} -am -q 2>/dev/null || true
COPY ${SERVICE_NAME}/src ${SERVICE_NAME}/src
RUN mvn package -pl ${SERVICE_NAME} -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
ARG SERVICE_NAME
ARG SERVICE_PORT=8080
WORKDIR /app
COPY --from=build /build/${SERVICE_NAME}/target/${SERVICE_NAME}-1.0.0.jar app.jar
EXPOSE ${SERVICE_PORT}
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
