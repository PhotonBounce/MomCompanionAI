package com.friendai

import android.content.Context

/**
 * Persistent memory of things the user tells the companion — their name and free-form facts
 * ("remember that my anniversary is in June"). This is what lets the app actually REMEMBER
 * across turns and across restarts, instead of treating every utterance in isolation.
 *
 * Backed by SharedPreferences when a [Context] is available (the real app), so it survives the
 * hands-free service creating a fresh [CompanionEngine] every turn AND survives app restarts.
 * When no Context is available (e.g. JVM unit tests) it falls back to per-instance memory, so a
 * single reused engine still remembers within a conversation.
 */
class UserMemory(private val context: Context?) {

    // Per-instance fallback used only when there is no Context (unit tests).
    private var fallbackName: String? = null
    private val fallbackFacts = mutableListOf<String>()

    private fun prefs() =
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var name: String?
        get() = prefs()?.getString(KEY_NAME, null) ?: fallbackName
        set(value) {
            fallbackName = value
            prefs()?.edit()?.putString(KEY_NAME, value)?.apply()
        }

    fun facts(): List<String> {
        val p = prefs()
        return if (p != null) {
            p.getString(KEY_FACTS, "").orEmpty().split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            fallbackFacts.toList()
        }
    }

    fun addFact(fact: String) {
        val f = fact.trim().trimEnd('.', '!', ',', ' ')
        if (f.isBlank()) return
        val current = facts().toMutableList()
        if (current.any { it.equals(f, ignoreCase = true) }) return
        current.add(f)
        while (current.size > MAX_FACTS) current.removeAt(0)
        val p = prefs()
        if (p != null) {
            p.edit().putString(KEY_FACTS, current.joinToString("\n")).apply()
        } else {
            fallbackFacts.clear(); fallbackFacts.addAll(current)
        }
    }

    fun hasAnything(): Boolean = name != null || facts().isNotEmpty()

    fun clear() {
        fallbackName = null
        fallbackFacts.clear()
        prefs()?.edit()?.remove(KEY_NAME)?.remove(KEY_FACTS)?.apply()
    }

    companion object {
        private const val PREFS = "user_memory"
        private const val KEY_NAME = "user_name"
        private const val KEY_FACTS = "user_facts"
        private const val MAX_FACTS = 25
    }
}
