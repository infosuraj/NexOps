FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY inventory-service/pom.xml inventory-service/
RUN mvn dependency:go-offline -pl inventory-service -am -q 2>/dev/null || true
COPY inventory-service/src inventory-service/src
RUN mvn package -pl inventory-service -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/inventory-service/target/inventory-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
