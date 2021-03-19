package de.westnordost.streetcomplete.quests.show_poi

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.changes.StringMapChangesBuilder
import de.westnordost.streetcomplete.data.osm.osmquest.OsmFilterQuestType
import de.westnordost.streetcomplete.quests.NoAnswerFragment

class ShowBench : OsmFilterQuestType<Boolean>() {
    override val elementFilter = """
        nodes with amenity = bench
        or leisure = picnic_table
    """
    override val commitMessage = "I hope this does not get committed"
    override val wikiLink = "nope"
    override val icon = R.drawable.ic_quest_bench_poi // replace later, but need own icon...
    override val dotColor = "chocolate"

    override fun getTitle(tags: Map<String, String>) =
        if (tags.containsKey("leisure"))
            R.string.quest_thisIsPicnic_title
        else
            R.string.quest_thisIsBench_title

    override fun createForm() = NoAnswerFragment()

    override fun applyAnswerTo(answer: Boolean, changes: StringMapChangesBuilder) {
    }
}
