import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.springframework.boot") version "3.4.7"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    kotlin("plugin.jpa") version "2.2.0"
}

group = "com.agents"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    // JetBrains koog package registry
    maven { url = uri("https://packages.jetbrains.team/maven/p/koog/public") }
}

dependencies {
    // ── Spring Boot ──────────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // ── Kotlin ───────────────────────────────────────────────────────────────
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // ── koog – JetBrains Kotlin AI agent framework ────────────────────────────
    // Core agent DSL (tools, strategies, AIAgent)
    implementation("ai.koog:koog-agents:0.1.0")

    // ── Google GenAI SDK – Vertex AI Gemini connection ────────────────────────
    // Unified GenAI SDK that supports both Google AI Studio and Vertex AI
    implementation("com.google.genai:google-genai:1.2.0")
    // Google auth for Application Default Credentials (ADC)
    implementation("com.google.auth:google-auth-library-oauth2-http:1.35.0")

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.14.3")

    // ── Database drivers ──────────────────────────────────────────────────────
    runtimeOnly("com.h2database:h2:2.3.232")
    runtimeOnly("org.postgresql:postgresql:42.7.7")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
