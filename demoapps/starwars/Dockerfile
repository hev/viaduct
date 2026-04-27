# Stage 1: Build (full JDK + Gradle for compilation)
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy the entire viaduct repo (context is viaduct root)
COPY . .

# Build starwars classes + test classes (skip running tests)
WORKDIR /app/demoapps/starwars
RUN chmod +x gradlew && ./gradlew classes testClasses --no-daemon -x test

# Stage 2: Run benchmarks (still need JDK for Gradle test task)
FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY --from=builder /app /app

WORKDIR /app/demoapps/starwars

# Default JVM memory flags — override via JAVA_OPTS env var
ENV JAVA_OPTS="-Xmx384m"

ENTRYPOINT ["sh", "-c", \
    "exec ./gradlew test --tests '*ReadWriteBenchmarkTest*' --no-daemon \
    -Dorg.gradle.jvmargs=\"$JAVA_OPTS\""]
