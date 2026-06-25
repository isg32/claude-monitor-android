package com.example.network

import androidx.compose.ui.graphics.Color

object ModelPricing {
    const val CLAUDE_3_5_SONNET = "claude-3-5-sonnet-20241022"
    const val CLAUDE_3_5_HAIKU = "claude-3-5-haiku-20241022"
    const val CLAUDE_3_OPUS = "claude-3-opus-20240229"
    const val CLAUDE_3_HAIKU = "claude-3-haiku-20240307"

    val MODELS = listOf(
        CLAUDE_3_5_SONNET,
        CLAUDE_3_5_HAIKU,
        CLAUDE_3_OPUS,
        CLAUDE_3_HAIKU
    )

    fun getDisplayName(model: String): String {
        return when {
            model.contains("sonnet") -> "Claude 3.5 Sonnet"
            model.contains("opus") -> "Claude 3 Opus"
            model.contains("3-5-haiku") -> "Claude 3.5 Haiku"
            model.contains("haiku") -> "Claude 3 Haiku"
            else -> model
        }
    }

    fun getColor(model: String): Color {
        return when {
            model.contains("sonnet") -> Color(0xFFD97706) // Deep Amber
            model.contains("opus") -> Color(0xFFDC2626) // Coral Crimson
            model.contains("3-5-haiku") -> Color(0xFF2563EB) // Cool Blue
            model.contains("haiku") -> Color(0xFF0D9488) // Teal
            else -> Color(0xFF6B7280) // Gray
        }
    }

    /**
     * Calculates the cost of a request in USD.
     */
    fun calculateCost(model: String, inputTokens: Int, outputTokens: Int): Double {
        val rates = when {
            model.contains("sonnet") -> Pair(3.0 / 1_000_000, 15.0 / 1_000_000)
            model.contains("opus") -> Pair(15.0 / 1_000_000, 75.0 / 1_000_000)
            model.contains("3-5-haiku") -> Pair(0.8 / 1_000_000, 4.0 / 1_000_000)
            model.contains("haiku") -> Pair(0.25 / 1_000_000, 1.25 / 1_000_000)
            else -> Pair(3.0 / 1_000_000, 15.0 / 1_000_000) // Fallback to Sonnet pricing
        }
        return (inputTokens * rates.first) + (outputTokens * rates.second)
    }
}
