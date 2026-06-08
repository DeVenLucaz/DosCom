package com.devenlucaz.doscom.personality

class ConversationHistory {
    private val messages = mutableListOf<Map<String, String>>()
    private val maxExchanges = 10
    private val maxMessages = maxExchanges * 2

    fun addMessage(role: String, text: String) {
        messages.add(mapOf("role" to role, "text" to text))
        
        while (messages.size > maxMessages) {
            messages.removeAt(0)
            if (messages.isNotEmpty()) {
                messages.removeAt(0)
            }
        }
    }

    fun toApiMessages(): List<Map<String, String>> {
        return messages.toList()
    }

    fun clear() {
        messages.clear()
    }
}
