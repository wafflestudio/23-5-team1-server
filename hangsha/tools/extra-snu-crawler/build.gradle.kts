plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.microsoft.playwright:playwright:1.57.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // “가능성 보여주기용” DB 저장 (나중에 Spring에 붙이면 여기 빼도 됨)
    implementation("com.mysql:mysql-connector-j:8.4.0")

    // 로그(선택)
    implementation("org.slf4j:slf4j-simple:2.0.16")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    // 아래 Main.kt를 교체할 거라 이 값 유지
    mainClass.set("org.example.MainKt")
}