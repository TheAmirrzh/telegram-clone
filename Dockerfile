# ---------- build stage ----------
FROM maven:3.8.8-openjdk-17 AS build
WORKDIR /build

# copy only pom first for dependency caching
COPY pom.xml .
# copy mvn settings if you use or need (optional)
# COPY .m2/settings.xml /root/.m2/settings.xml

# download dependencies
RUN mvn -B -DskipTests dependency:go-offline

# copy sources and build
COPY src ./src
COPY sql ./sql
COPY src/main/resources ./src/main/resources
RUN mvn -B -DskipTests package dependency:copy-dependencies

# ---------- runtime stage ----------
FROM openjdk:17-jdk-slim
WORKDIR /app

# create runtime directories
RUN mkdir -p /app/storage

# copy app jar and dependencies from build stage
COPY --from=build /build/target/*.jar /app/app.jar
COPY --from=build /build/target/dependency /app/lib

# expose storage for attachments persist
VOLUME ["/app/storage"]

# default DB envs (overridable in docker-compose)
ENV DB_URL=jdbc:postgresql://db:5432/telegramdb
ENV DB_USER=telegram_user
ENV DB_PASS=telegram_pass

# Use a simple entrypoint that runs the app classpath with dependencies
# Note: com.telegram.App is the JavaFX Application main.
ENTRYPOINT ["sh","-c","java -cp 'app.jar:lib/*' com.telegramapp.App"]
