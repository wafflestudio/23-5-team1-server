FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 임시: 행사 sync를 위함
COPY tools/extra-snu-crawler/events.json /app/tools/extra-snu-crawler/events.json

ENTRYPOINT ["java", "-jar", "/app/app.jar"]