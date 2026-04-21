#
# Multi-stage Dockerfile for Java Spring Boot application
#

# Stage 1: Build stage
# Install the application and its dependencies
# Create a custom minimal JRE
FROM amazoncorretto:25-alpine-full AS build

WORKDIR /usr/src/project

ENV JAVA_VERSION=25
ENV APP_NAME=app.jar
ENV DEPS_FILE=deps.info

# Change this when there is an update
ENV COMMONS_NAME=spring-base-commons
ENV COMMONS_GROUP_ID=com.vulinh
ENV COMMONS_VERSION=2.5.0
ENV GITHUB_USER=vulinh64

# Copy Maven wrapper files
COPY mvnw ./
COPY .mvn/ .mvn/

RUN chmod +x mvnw

# Download the pre-built JAR from GitHub releases
RUN wget -O ${COMMONS_NAME}.jar \
    https://github.com/${GITHUB_USER}/${COMMONS_NAME}/releases/download/${COMMONS_VERSION}/${COMMONS_NAME}-${COMMONS_VERSION}.jar

# Install the JAR to local Maven repository
RUN ./mvnw install:install-file \
    -Dfile=${COMMONS_NAME}.jar \
    -DgroupId=${COMMONS_GROUP_ID} \
    -DartifactId=${COMMONS_NAME} \
    -Dversion=${COMMONS_VERSION} \
    -Dpackaging=jar

# Copy main application Maven configuration files
COPY pom.xml ./

# Copy source code
COPY src/ src/

# Build the application using Maven wrapper
RUN ./mvnw clean package -DskipTests

# Extract the JAR to analyze its dependencies
RUN jar xf target/${APP_NAME}

# Use jdeps to identify necessary Java modules for a minimal JRE
RUN jdeps  \
    --ignore-missing-deps  \
    -q --recursive  \
    --multi-release ${JAVA_VERSION} \
    --print-module-deps  \
    --class-path 'BOOT-INF/lib/*' \
    target/${APP_NAME} > ${DEPS_FILE}

# Append modules that jdeps cannot detect (runtime service lookup, not bytecode references)
# jdk.crypto.ec: ECDHE/ECDSA during HTTPS handshakes
# jdk.compiler: runs HealthCheck.java compact source file via the source launcher
RUN echo "$(cat ${DEPS_FILE}),jdk.crypto.ec,jdk.compiler" > ${DEPS_FILE}

# Debug: print the full module list so it appears in the build log
RUN echo "=== ${DEPS_FILE} ===" && cat ${DEPS_FILE} && echo "==================="

# Create a custom JRE with only the required modules
RUN jlink \
    --add-modules $(cat ${DEPS_FILE}) \
    --strip-java-debug-attributes  \
    --compress zip-6  \
    --no-header-files  \
    --no-man-pages \
    --output /jre-minimalist

# Stage 2: Production stage - Minimal Alpine image with custom JRE
FROM alpine:3.23.3 AS final

ENV JAVA_HOME=/opt/java/jre-minimalist
ENV PATH=$JAVA_HOME/bin:$JAVA_HOME/lib:$PATH
ENV USER=spring-user
ENV GROUP=spring-group
ENV WORKDIR=app
ENV APP_NAME=app.jar
ENV DEPS_FILE=deps.info

# Copy the custom JRE from the build stage
COPY --from=build /jre-minimalist $JAVA_HOME

# Create a non-root user for security
RUN addgroup -S ${GROUP} \
    && adduser -S ${USER} -G ${GROUP} \
    && mkdir -p /${WORKDIR} \
    && chown -R ${USER}:${GROUP} /${WORKDIR}

# Copy application artifacts from build stage
COPY --from=build /usr/src/project/target/${APP_NAME} /${WORKDIR}/

# Copy the Docker health check companion
COPY HealthCheck.java /${WORKDIR}/HealthCheck.java

# Copy the detected module list for post-build inspection (survives build-cache hits)
COPY --from=build /usr/src/project/${DEPS_FILE} /${WORKDIR}/${DEPS_FILE}

WORKDIR /${WORKDIR}

USER ${USER}

# Docker health check
# Runs `java HealthCheck.java` which internally retries up to 30 seconds
# timeout must exceed the internal retry window
HEALTHCHECK --interval=30s --timeout=35s --start-period=60s --retries=3 \
    CMD ["java", "HealthCheck.java"]

#
# Run the application with optimized JVM settings
#

# - UseCompactObjectHeaders: See JEP 519 (https://openjdk.org/jeps/519)
# - MaxRAMPercentage: Limit max heap to 75% of container memory
# - InitialRAMPercentage: Start with 50% of container memory
# - MaxMetaspaceSize: Limit metaspace to 512MB
ENTRYPOINT ["java", \
    "-XX:+UseCompactObjectHeaders", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-XX:MaxMetaspaceSize=512m", \
    "-jar", \
    "app.jar"]