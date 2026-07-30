FROM maven:3.9-eclipse-temurin-21-alpine AS build
ARG SERVICE_NAME
WORKDIR /build
COPY pom.xml .
COPY ${SERVICE_NAME}/pom.xml ${SERVICE_NAME}/pom.xml
COPY ${SERVICE_NAME}/src ${SERVICE_NAME}/src
RUN mvn install -N -q
RUN mvn package -f ${SERVICE_NAME}/pom.xml -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
ARG SERVICE_NAME
ARG SERVICE_PORT=8080
WORKDIR /app
COPY --from=build /build/${SERVICE_NAME}/target/${SERVICE_NAME}-1.0.0.jar app.jar
EXPOSE ${SERVICE_PORT}
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
