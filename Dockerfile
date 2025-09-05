# ---------- build stage ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

# copy pom first to leverage caching
COPY pom.xml .
# copy mvn settings if you use or need (optional)
# COPY .m2/settings.xml /root/.m2/settings.xml

# run a build step to download dependencies
RUN mvn -B -DskipTests clean package || true

# copy project sources
COPY src ./src
COPY sql ./sql
COPY src/main/resources ./src/main/resources

# build final jar with dependencies
RUN mvn -B -DskipTests clean package dependency:copy-dependencies

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN mkdir -p /app/storage

COPY --from=build /build/target/*.jar /app/app.jar
COPY --from=build /build/target/dependency /app/lib

VOLUME ["/app/storage"]

ENV DB_URL=jdbc:postgresql://db:5432/telegramdb
ENV DB_USER=telegram_user
ENV DB_PASS=telegram_pass

ENTRYPOINT ["sh","-c","java -cp 'app.jar:lib/*' com.telegramapp.App"]
