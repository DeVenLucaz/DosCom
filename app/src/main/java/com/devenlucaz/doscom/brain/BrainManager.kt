package com.devenlucaz.doscom.brain

import android.content.Context

object BrainManager {
    val brain = DosCombrain()
    private var isInitialized = false
    
    fun init(context: Context) {
        if (!isInitialized) {
            brain.load(context)
            isInitialized = true
        }
    }
}
