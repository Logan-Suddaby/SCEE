package de.westnordost.streetcomplete.quests.leaf_detail

import android.content.Context
import androidx.appcompat.app.AlertDialog
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.geometry.ElementPolygonsGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.data.osm.osmquests.OsmElementQuestType
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement.OUTDOORS
import de.westnordost.streetcomplete.osm.Tags
import de.westnordost.streetcomplete.quests.booleanQuestSettingsDialog
import de.westnordost.streetcomplete.util.math.measuredMultiPolygonArea

class AddForestLeafType : OsmElementQuestType<ForestLeafType> {
    private val areaFilter by lazy { """
        ways, relations with (landuse = forest or natural = wood) and !leaf_type
    """.toElementFilterExpression() }

    private val wayFilter by lazy { """
        ways with natural = tree_row and !leaf_type
    """.toElementFilterExpression() }

    private val nodeFilter by lazy { """
        nodes with natural = tree and !leaf_type and !species and !genus
    """.toElementFilterExpression() }

    override val changesetComment = "Specify leaf types"
    override val wikiLink = "Key:leaf_type"
    override val icon = R.drawable.ic_quest_leaf
    override val achievements = listOf(OUTDOORS)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_leafType_title

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> {
        val forests = mapData
            .filter { areaFilter.matches(it) }
            .filter {
                val geometry = mapData.getGeometry(it.type, it.id) as? ElementPolygonsGeometry
                val area = geometry?.polygons?.measuredMultiPolygonArea() ?: 0.0
                area > 0.0 && area < 10000
            }
        val treeRows = mapData.filter { wayFilter.matches(it) }
        return if (prefs.getBoolean(SINGLE_TREES_PREF, false)) forests + treeRows + mapData.filter { nodeFilter.matches(it) }
        else forests + treeRows
    }

    override fun isApplicableTo(element: Element): Boolean? {
        if (prefs.getBoolean(SINGLE_TREES_PREF, false) && nodeFilter.matches(element)) return true
        if (wayFilter.matches(element)) return true // tree rows
        // for areas, we don't want to show things larger than x m², we need the geometry for that
        if (!areaFilter.matches(element)) return false
        return null
    }

    override fun createForm() = AddForestLeafTypeForm()

    override fun applyAnswerTo(answer: ForestLeafType, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags["leaf_type"] = answer.osmValue
    }

    override val hasQuestSettings = true

    override fun getQuestSettingsDialog(context: Context) =
        booleanQuestSettingsDialog(context, prefs, SINGLE_TREES_PREF,
            R.string.quest_settings_leaf_type_single_tree_message, R.string.quest_settings_leaf_type_single_tree_yes,
            R.string.quest_settings_leaf_type_single_tree_no
        )
}

private const val SINGLE_TREES_PREF = "qs_AddForestLeafType_single_trees"
