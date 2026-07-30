FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/inventory-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
