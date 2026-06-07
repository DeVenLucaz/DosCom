package com.devenlucaz.doscom.brain

import android.content.Context

enum class PersonalityType { EXPLORER, PLAYFUL, CURIOUS, TALKATIVE }
enum class PersonalityEvent { DISCOVERY_FOUND, TOY_PLAYED, IDLE_OBSERVED, CHAT_INITIATED }

object PersonalityGrowth {
    private var explorerWeight = 1.0f
    private var playfulWeight = 1.0f
    private var curiousWeight = 1.0f
    private var talkativeWeight = 1.0f

    fun getDominantType(): PersonalityType {
        var maxWeight = explorerWeight
        var dominant = PersonalityType.EXPLORER

        if (playfulWeight > maxWeight) { maxWeight = playfulWeight; dominant = PersonalityType.PLAYFUL }
        if (curiousWeight > maxWeight) { maxWeight = curiousWeight; dominant = PersonalityType.CURIOUS }
        if (talkativeWeight > maxWeight) { dominant = PersonalityType.TALKATIVE }

        return dominant
    }

    fun record(event: PersonalityEvent) {
        when (event) {
            PersonalityEvent.DISCOVERY_FOUND -> explorerWeight += 0.1f
            PersonalityEvent.TOY_PLAYED -> playfulWeight += 0.1f
            PersonalityEvent.IDLE_OBSERVED -> curiousWeight += 0.1f
            PersonalityEvent.CHAT_INITIATED -> talkativeWeight += 0.1f
        }
    }
}
