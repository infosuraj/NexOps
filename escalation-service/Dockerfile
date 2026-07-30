FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY escalation-service/pom.xml escalation-service/
RUN mvn dependency:go-offline -pl escalation-service -am -q 2>/dev/null || true
COPY escalation-service/src escalation-service/src
RUN mvn package -pl escalation-service -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/escalation-service/target/escalation-service-1.0.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
