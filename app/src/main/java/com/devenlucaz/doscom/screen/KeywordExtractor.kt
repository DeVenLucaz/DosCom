package com.devenlucaz.doscom.screen

object KeywordExtractor {
    private val fillerWords = setOf(
        "how", "do", "i", "can", "where", "is", "the", "a", "an", "to",
        "open", "find", "show", "me", "please", "tap", "click", "press",
        "button", "icon", "app"
    )

    fun extractKeywords(query: String): Pair<String, String> {
        val lowerCaseQuery = query.lowercase()
        val words = lowerCaseQuery.split("\\s+".toRegex())
        val keywords = words.filter { it.isNotBlank() && !fillerWords.contains(it) }
        val extracted = keywords.joinToString(" ")
        return Pair(query, extracted)
    }
}
