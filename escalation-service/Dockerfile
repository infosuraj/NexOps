FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/escalation-service-1.0.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
