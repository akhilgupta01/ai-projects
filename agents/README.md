# agents

Collection of AI agentic workflow modules for the ai-projects repository.

## Sub-modules

| Module | Description |
|--------|-------------|
| [qa-agent](qa-agent/README.md) | Quality Assurance agent – extracts reportable attributes from financial regulatory reporting specification PDFs |

## Tech stack

All agents are implemented in **Kotlin** and use:
* [koog](https://github.com/JetBrains/koog) – JetBrains Kotlin-native AI agent framework
* [Google GenAI SDK](https://github.com/googleapis/java-genai) – unified Gemini SDK (`com.google.genai:google-genai`)
* **Vertex AI Gemini** as the LLM backend
