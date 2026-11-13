# syntax=docker/dockerfile:1

# Builds the application from source and packages it as a small runtime image.
# Nothing but Docker is needed on the machine that builds it.
#
#   docker build -t petstore .
#   docker run --rm -p 8080:8080 petstore                      embedded database
#   docker compose --profile app up --build                    with PostgreSQL
#
# Tests are not run here. They are the job of `./mvnw verify`, locally and in
# CI; some of them start database containers, which an image build cannot do.

# ---- Stage 1: build the jar and split it into layers -------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src

# The Maven repository is kept in a build cache, so only the first build
# downloads the dependencies.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress -Dmaven.test.skip=true package \
    && cp target/petstore-*.jar petstore.jar

# Dependencies change far less often than the application's own classes.
# Extracting them into separate layers means a code change rebuilds and
# ships only the last, small layer.
RUN java -Djarmode=tools -jar petstore.jar extract --layers --destination extracted

# ---- Stage 2: the image that runs --------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Runs as an unprivileged user that owns nothing in the image.
RUN groupadd --system petstore \
    && useradd --system --gid petstore --home-dir /app --no-create-home petstore

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER petstore
EXPOSE 8080

# Size the heap from the container's memory limit instead of the host's.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["java", "-jar", "petstore.jar"]
