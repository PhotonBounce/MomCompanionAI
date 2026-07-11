package com.friendai

enum class EscalationLevel {
    NONE,
    EMERGENCY,
    SCAM,
    MEDICAL
}

data class CompanionReply(
    val text: String,
    val escalationLevel: EscalationLevel = EscalationLevel.NONE
)
