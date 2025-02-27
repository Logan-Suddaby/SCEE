package de.westnordost.streetcomplete.quests.shop_type

import de.westnordost.osmfeatures.Feature
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.filter
import de.westnordost.streetcomplete.data.osm.osmquests.OsmFilterQuestType
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement.CITIZEN
import de.westnordost.streetcomplete.osm.IS_SHOP_OR_DISUSED_SHOP_EXPRESSION
import de.westnordost.streetcomplete.osm.LAST_CHECK_DATE_KEYS
import de.westnordost.streetcomplete.osm.Tags
import de.westnordost.streetcomplete.osm.isShopExpressionFragment
import de.westnordost.streetcomplete.osm.updateCheckDate

class CheckShopExistence(
    private val getFeature: (tags: Map<String, String>) -> Feature?
) : OsmFilterQuestType<Unit>() {
    // opening hours quest acts as a de facto checker of shop existence, but some people disabled it.
    // separate from CheckExistence as very old shop with opening hours should show
    // opening hours resurvey quest rather than this one (which would cause edit date to be changed
    // and silence all resurvey quests)
    override val elementFilter by lazy { ("""
        nodes, ways with (
             ${isShopExpressionFragment()}
             and !man_made
             and !historic
             and !military
             and !power
             and !attraction
             and !aeroway
             and !railway
        ) and (
          older today -2 years
          or ${LAST_CHECK_DATE_KEYS.joinToString(" or ") { "$it < today -2 years" }}
        )
        and (name or brand or noname = yes or name:signed = no)
    """) }

    override val changesetComment = "Survey if places (shops and other shop-like) still exist"
    override val wikiLink = "Key:disused:"
    override val icon = R.drawable.ic_quest_check_shop
    override val achievements = listOf(CITIZEN)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_existence_title2

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().filter(IS_SHOP_OR_DISUSED_SHOP_EXPRESSION)

    override fun createForm() = CheckShopExistenceForm()

    override fun applyAnswerTo(answer: Unit, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        tags.updateCheckDate()
    }
}
