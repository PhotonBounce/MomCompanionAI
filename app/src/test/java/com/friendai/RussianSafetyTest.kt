package com.friendai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the Russian-language SAFETY behaviour of the offline engine.
 *
 * Regression cover for the bug where a Russian bank-scam phrase ("the bank is asking
 * for my code and money") was greeted as happy family news, because isPossibleScam()'s
 * Russian keyword list lacked bare "код" / "деньги" / "банк".
 */
class RussianSafetyTest {
    private fun blank() = CaregiverSettings(
        rules = "", profileNotes = "", vocabularyNotes = "", promptTopics = "",
        pin = "", contactName = "", contactPhone = "", backendUrl = "", backendToken = ""
    )
    private fun reply(m: String) = CompanionEngine().replyTo(m, blank(), context = null).text

    private fun warnsAboutScam(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("не говорите") || t.contains("не сообщайте") ||
            t.contains("остановимся") || t.contains("не отправляйте") ||
            t.contains("do not share") || t.contains("do not send")
    }

    @Test fun russianScams_areCaught() {
        val scams = listOf(
            "Мне позвонили из банка и просят код и деньги.",
            "Мужчина по телефону просит перевести деньги на карту.",
            "Просят продиктовать код из смс.",
            "Незнакомец звонит и требует деньги."
        )
        for (m in scams) {
            val r = reply(m)
            assertTrue("Russian scam not caught for: $m -> $r", warnsAboutScam(r))
            assertFalse("Scam greeted as a happy call: $m -> $r", r.contains("Звонок близких"))
        }
    }

    @Test fun innocentMoneyTalk_isNotFlagged() {
        // Sensitive word present but NO pressure/coercion — must stay a normal, warm chat.
        val innocent = listOf(
            "Внук подарил мне подарочную карту на день рождения.",
            "Я заплатила картой в магазине сегодня."
        )
        for (m in innocent) {
            val r = reply(m)
            assertFalse("Innocent talk wrongly flagged as scam: $m -> $r", warnsAboutScam(r))
        }
    }

    @Test fun russianInput_getsRussianReply() {
        val r = reply("Здравствуй, как твои дела?")
        assertTrue("Russian input should get a Russian reply: $r", r.any { it in 'Ѐ'..'ӿ' })
    }
}
