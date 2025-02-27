package de.westnordost.streetcomplete.data.visiblequests

import android.content.SharedPreferences
import de.westnordost.streetcomplete.Prefs
import de.westnordost.streetcomplete.data.quest.DayNightCycle.*
import de.westnordost.streetcomplete.data.quest.Quest
import de.westnordost.streetcomplete.util.isDay
import org.koin.java.KoinJavaComponent.inject

class DayNightQuestFilter internal constructor(
    private val prefs: SharedPreferences
) {
    var isEnabled = false
        private set

    fun reload() {
        isEnabled = Prefs.DayNightBehavior.valueOf(prefs.getString(Prefs.DAY_NIGHT_BEHAVIOR, "IGNORE")!!) == Prefs.DayNightBehavior.VISIBILITY
    }

    /*
    Might be an idea to add a listener so this is reevaluated occasionally, or something like that.
    However, I think it's reevaluated everytime the displayed quests are updated?
     */
    fun isVisible(quest: Quest): Boolean {
        if (!isEnabled) return true
        return when (quest.type.dayNightCycle) {
            DAY_AND_NIGHT -> true
            ONLY_DAY -> isDay(quest.position)
            ONLY_NIGHT -> !isDay(quest.position)
        }
    }
}
