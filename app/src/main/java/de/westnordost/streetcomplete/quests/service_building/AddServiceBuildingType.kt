package de.westnordost.streetcomplete.quests.service_building

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.edits.update_tags.StringMapChangesBuilder
import de.westnordost.streetcomplete.data.osm.osmquests.OsmFilterQuestType
import de.westnordost.streetcomplete.quests.YesNoQuestAnswerFragment

class AddServiceBuildingType : OsmFilterQuestType<Unit>() {

    // todo: only ask for energy operators for now, but they contain spaces -> how to escape?
    override val elementFilter = "nodes, ways, relations with building = service and operator and !power and !service"
    override val commitMessage = "Add service building type"
    override val wikiLink = "Tag:building=service"
    override val icon = R.drawable.ic_quest_power

    override fun getTitle(tags: Map<String, String>) = R.string.quest_service_building_type_title

    override fun getTitleArgs(tags: Map<String, String>, featureName: Lazy<String?>): Array<String> {
        return arrayOf(tags["operator"] ?: "")
    }

    override fun createForm() = AddServiceBuildingTypeForm()

    override fun applyAnswerTo(answer: Unit, changes: StringMapChangesBuilder) {
        changes.add("power", "substation")
        changes.add("substation", "minor_distribution")
    }

}
