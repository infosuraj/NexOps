FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/pricing-service-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
