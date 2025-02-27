package de.westnordost.streetcomplete.quests.place_name

import de.westnordost.osmfeatures.Feature
import de.westnordost.streetcomplete.osm.LocalizedName

sealed interface PlaceNameAnswer

data class PlaceName(val localizedNames: List<LocalizedName>) : PlaceNameAnswer
object NoPlaceNameSign : PlaceNameAnswer
data class FeatureName(val feature: Feature) : PlaceNameAnswer
data class BrandName(val name: String) : PlaceNameAnswer
