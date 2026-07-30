FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY pricing-service/pom.xml pricing-service/
RUN mvn dependency:go-offline -pl pricing-service -am -q 2>/dev/null || true
COPY pricing-service/src pricing-service/src
RUN mvn package -pl pricing-service -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/pricing-service/target/pricing-service-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
