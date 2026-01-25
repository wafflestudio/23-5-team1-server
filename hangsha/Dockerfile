FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# 컨테이너 기본 타임존을 KST로 맞추기
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone

ENV TZ=Asia/Seoul

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 임시: 행사 sync를 위함
COPY tools/extra-snu-crawler/events.json /app/tools/extra-snu-crawler/events.json

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app/app.jar"]