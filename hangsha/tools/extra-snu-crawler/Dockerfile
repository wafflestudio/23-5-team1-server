# 도커파일 예시 by GPT
# TODO: 나중에 수정해야 함.
# Playwright라서 런타임 라이브러리 + 브라우저 설치 단계는 Dockerfile에 꼭 넣어야 컨테이너에서 실행됨. << 라고 함.

# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle wrapper + build scripts 먼저 복사 (캐시 잘 먹게)
COPY gradlew gradlew
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
RUN chmod +x gradlew

# 소스 복사
COPY src/ src/

# 빌드 (테스트 생략)
RUN ./gradlew clean build -x test

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Playwright 의존성(리눅스 런타임 라이브러리) 설치
# (Ubuntu/Debian 계열 기준)
RUN apt-get update && apt-get install -y \
    libnss3 libnspr4 libatk1.0-0 libatk-bridge2.0-0 libcups2 \
    libdrm2 libxkbcommon0 libxcomposite1 libxdamage1 libxfixes3 \
    libxrandr2 libgbm1 libasound2 libpangocairo-1.0-0 libpango-1.0-0 \
    libcairo2 libgtk-3-0 libx11-6 libx11-xcb1 libxcb1 libxext6 \
    ca-certificates fonts-liberation \
 && rm -rf /var/lib/apt/lists/*

# jar 복사
COPY --from=build /app/build/libs/*.jar /app/app.jar

# (중요) Playwright 브라우저 설치:
# Java Playwright는 CLI로 브라우저를 내려받아야 함.
# 프로젝트에 playwright 의존성이 들어있다는 전제.
RUN java -cp /app/app.jar com.microsoft.playwright.CLI install chromium

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
