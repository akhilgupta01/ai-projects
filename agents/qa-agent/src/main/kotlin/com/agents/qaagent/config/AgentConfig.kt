package com.agents.qaagent.config

import com.google.genai.Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configures the Google GenAI [Client] in Vertex AI mode.
 *
 * Authentication relies on Application Default Credentials (ADC).
 * Set the environment variable GOOGLE_APPLICATION_CREDENTIALS or run
 * `gcloud auth application-default login` before starting the application.
 *
 * Required application.properties:
 *   vertex.ai.project   – GCP project ID
 *   vertex.ai.location  – Vertex AI region (e.g. us-central1)
 *   vertex.ai.model     – Gemini model name (e.g. gemini-2.0-flash)
 */
@Configuration
class AgentConfig(
    @Value("\${vertex.ai.project}") private val project: String,
    @Value("\${vertex.ai.location}") private val location: String
) {

    /**
     * Google GenAI client configured for Vertex AI.
     *
     * The [Client] is thread-safe and can be shared across requests.
     */
    @Bean
    fun genAiClient(): Client =
        Client.builder()
            .vertexAI(true)
            .project(project)
            .location(location)
            .build()
}
