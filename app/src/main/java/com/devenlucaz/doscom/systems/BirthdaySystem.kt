package com.devenlucaz.doscom.systems

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

enum class BirthdayPhase { MIDNIGHT_UNLOCK, MORNING, AFTERNOON, EVENING }

object BirthdaySystem {
    private const val PREFS_NAME = "doscom_birthday_prefs"

    fun getUserBirthday(context: Context): Pair<Int, Int>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val month = prefs.getInt("birthday_month", -1)
        val day = prefs.getInt("birthday_day", -1)
        if (month == -1 || day == -1) return null
        return Pair(month, day)
    }

    fun getInstallDate(context: Context): LocalDate {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dateStr = prefs.getString("install_date", null)
        if (dateStr == null) {
            val now = LocalDate.now()
            prefs.edit().putString("install_date", now.format(DateTimeFormatter.ISO_LOCAL_DATE)).apply()
            return now
        }
        return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
    }

    fun isUserBirthday(context: Context): Boolean {
        val bday = getUserBirthday(context) ?: return false
        val cal = Calendar.getInstance()
        // Calendar month is 0-indexed, but let's assume stored month is 1-12
        return cal.get(Calendar.MONTH) + 1 == bday.first && cal.get(Calendar.DAY_OF_MONTH) == bday.second
    }

    fun isDosCombBirthday(context: Context): Boolean {
        val installDate = getInstallDate(context)
        val now = LocalDate.now()
        // Same month and day, but not the exact same date (needs to be > 0 years)
        return installDate.monthValue == now.monthValue && 
               installDate.dayOfMonth == now.dayOfMonth &&
               installDate.year < now.year
    }

    fun getBirthdayDayPhase(): BirthdayPhase {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 0..9 -> BirthdayPhase.MORNING // Treating 0-9 as MORNING (or MIDNIGHT_UNLOCK if it's the unlock event)
            hour in 10..17 -> BirthdayPhase.AFTERNOON
            else -> BirthdayPhase.EVENING
        }
    }
}
