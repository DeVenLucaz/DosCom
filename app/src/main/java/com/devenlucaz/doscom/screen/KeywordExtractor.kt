package com.devenlucaz.doscom.screen

object KeywordExtractor {
    private val fillerWords = setOf(
        "how", "do", "i", "can", "where", "is", "the", "a", "an", "to",
        "open", "find", "show", "me", "please", "tap", "click", "press",
        "button", "icon", "app", "on", "at", "in", "it"
    )

    fun extractKeywords(query: String): Pair<String, String> {
        val lowerCaseQuery = query.lowercase().trim()
        val words = lowerCaseQuery.split("\\s+".toRegex())
        val keywords = words.filter { it.isNotBlank() && !fillerWords.contains(it) }
        val extracted = keywords.joinToString(" ").trim()
        return Pair(query, extracted)
    }
}
