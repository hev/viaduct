# Stage 1: Build (context is the viaduct root)
# The composite build resolves plugins + viaduct libraries from local projects.
# We publish them to Maven Local first, then build amazon-reviews standalone.
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew \
    && ./gradlew publishToMavenLocal --no-daemon -x test -x check \
    && cd demoapps/amazon-reviews \
    && chmod +x gradlew \
    && USE_MAVEN_LOCAL=true ./gradlew shadowJar :loader:shadowJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/demoapps/amazon-reviews/build/libs/*-all.jar /app/app.jar
COPY --from=builder /app/demoapps/amazon-reviews/loader/build/libs/loader.jar /app/loader.jar
ENV JAVA_OPTS="-Xmx12g -Xms8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
