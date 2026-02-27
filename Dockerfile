FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew
COPY src src
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

# healthcheck 용 curl 설치
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/*.jar app.jar
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
