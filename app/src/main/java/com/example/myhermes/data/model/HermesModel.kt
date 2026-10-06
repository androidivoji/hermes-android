package com.example.myhermes.data.model

data class HermesModel(
    val id: String,
    val name: String,
    val description: String,
    val tag: String = "Server",
    val isDefault: Boolean = false
)

object HermesModelDefaults {
    val DEFAULT_MODEL = HermesModel(
        id = "gpt-4o",
        name = "GPT-4o (Default)",
        description = "Model OpenAI default pada proxy server.",
        tag = "Recommended",
        isDefault = true
    )

    val COMMON_MODELS = listOf(
        DEFAULT_MODEL,
        HermesModel(
            id = "gpt-4o-mini",
            name = "GPT-4o Mini",
            description = "Model cepat dan ringan.",
            tag = "Fast"
        ),
        HermesModel(
            id = "gpt-3.5-turbo",
            name = "GPT-3.5 Turbo",
            description = "Model standar OpenAI.",
            tag = "Standard"
        ),
        HermesModel(
            id = "hermes",
            name = "Hermes Model",
            description = "Model Hermes spesifik.",
            tag = "Hermes"
        ),
        HermesModel(
            id = "claude-3-5-sonnet",
            name = "Claude 3.5 Sonnet",
            description = "Model Anthropic Claude.",
            tag = "Claude"
        ),
        HermesModel(
            id = "deepseek-chat",
            name = "DeepSeek Chat",
            description = "Model DeepSeek V3/R1.",
            tag = "DeepSeek"
        )
    )
}
