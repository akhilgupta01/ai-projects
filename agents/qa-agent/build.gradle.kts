import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
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

    // ── Kotlin ───────────────────────────────────────────────────────────────
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // ── koog – JetBrains Kotlin AI agent framework ────────────────────────────
    // Core agent DSL (tools, strategies, AIAgent)
    implementation("ai.koog:koog-agents:0.1.0")

    // ── Google GenAI SDK – Vertex AI Gemini connection ────────────────────────
    // Unified GenAI SDK that supports both Google AI Studio and Vertex AI
    implementation("com.google.genai:google-genai:1.0.0")
    // Google auth for Application Default Credentials (ADC)
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")

    // ── PDF parsing ──────────────────────────────────────────────────────────
    implementation("org.apache.pdfbox:pdfbox:3.0.1")

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.mockk:mockk:1.13.10")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
