# ─── Java build stage (shared) ───────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS java-build
ARG SERVICE_NAME
WORKDIR /build
COPY pom.xml .
COPY ${SERVICE_NAME}/pom.xml ${SERVICE_NAME}/pom.xml
COPY ${SERVICE_NAME}/src     ${SERVICE_NAME}/src
RUN mvn install -N -q && \
    mvn package -f ${SERVICE_NAME}/pom.xml -DskipTests -q

# ─── Java run stage ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS java-service
ARG SERVICE_NAME
ARG SERVICE_PORT=8080
WORKDIR /app
COPY --from=java-build /build/${SERVICE_NAME}/target/${SERVICE_NAME}-1.0.0.jar app.jar
EXPOSE ${SERVICE_PORT}
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]

# ─── Frontend build stage ─────────────────────────────────────
FROM node:18-alpine AS frontend-build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm install
ARG VITE_API_URL
ENV VITE_API_URL=${VITE_API_URL}
COPY frontend/index.html    ./
COPY frontend/vite.config.js ./
COPY frontend/src           ./src
COPY frontend/public        ./public
RUN npm run build

# ─── Frontend run stage ───────────────────────────────────────
FROM nginx:alpine AS frontend
RUN apk add --no-cache gettext
COPY --from=frontend-build /app/dist /usr/share/nginx/html
COPY frontend/nginx.conf.template /etc/nginx/conf.d/default.conf.template
COPY frontend/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
EXPOSE 80
CMD ["/docker-entrypoint.sh"]
