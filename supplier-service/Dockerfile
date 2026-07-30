FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY supplier-service/pom.xml supplier-service/
RUN mvn dependency:go-offline -pl supplier-service -am -q 2>/dev/null || true
COPY supplier-service/src supplier-service/src
RUN mvn package -pl supplier-service -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/supplier-service/target/supplier-service-1.0.0.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
