package de.westnordost.streetcomplete.quests.max_speed

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.widget.doAfterTextChanged
import de.westnordost.streetcomplete.Prefs
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.meta.SpeedMeasurementUnit
import de.westnordost.streetcomplete.data.meta.SpeedMeasurementUnit.KILOMETERS_PER_HOUR
import de.westnordost.streetcomplete.data.meta.SpeedMeasurementUnit.MILES_PER_HOUR
import de.westnordost.streetcomplete.databinding.QuestMaxspeedBinding
import de.westnordost.streetcomplete.databinding.QuestMaxspeedNoSignNoSlowZoneConfirmationBinding
import de.westnordost.streetcomplete.quests.AbstractOsmQuestForm
import de.westnordost.streetcomplete.quests.AnswerItem
import de.westnordost.streetcomplete.quests.max_speed.SpeedType.ADVISORY
import de.westnordost.streetcomplete.quests.max_speed.SpeedType.LIVING_STREET
import de.westnordost.streetcomplete.quests.max_speed.SpeedType.NO_SIGN
import de.westnordost.streetcomplete.quests.max_speed.SpeedType.NSL
import de.westnordost.streetcomplete.quests.max_speed.SpeedType.SIGN
import de.westnordost.streetcomplete.quests.max_speed.SpeedType.ZONE
import de.westnordost.streetcomplete.util.ktx.advisorySpeedLimitSignLayoutResId
import de.westnordost.streetcomplete.util.ktx.intOrNull
import de.westnordost.streetcomplete.util.ktx.livingStreetSignDrawableResId
import de.westnordost.streetcomplete.util.ktx.showKeyboard
import de.westnordost.streetcomplete.util.showAddConditionalDialog

class AddMaxSpeedForm : AbstractOsmQuestForm<Pair<MaxSpeedAnswer, Pair<String, String>?>>() {

    override val contentLayoutResId = R.layout.quest_maxspeed
    private val binding by contentViewBinding(QuestMaxspeedBinding::bind)

    override val otherAnswers: List<AnswerItem> get() {
        val result = mutableListOf<AnswerItem>()
        if (countryInfo.hasAdvisorySpeedLimitSign) {
            result.add(AnswerItem(R.string.quest_maxspeed_answer_advisory_speed_limit) { switchToAdvisorySpeedLimit() })
        }
        if (prefs.getBoolean(Prefs.EXPERT_MODE, false))
            result.add(AnswerItem(R.string.quest_maxspeed_conditional) { addConditional() })
        return result
    }

    private var speedInput: EditText? = null
    private var speedUnitSelect: Spinner? = null
    private var speedType: SpeedType? = null

    private val speedUnits get() = countryInfo.speedUnits

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val highwayTag = element.tags["highway"]!!

        val couldBeSlowZone = countryInfo.hasSlowZone && POSSIBLY_SLOWZONE_ROADS.contains(highwayTag)
        binding.zone.isGone = !couldBeSlowZone

        val couldBeLivingStreet = countryInfo.hasLivingStreet && MAYBE_LIVING_STREET.contains(highwayTag)
        binding.livingStreet.isGone = !couldBeLivingStreet

        val couldBeNSL = countryInfo.countryCode == "GB"
        binding.nsl.isGone = !couldBeNSL

        binding.speedTypeSelect.setOnCheckedChangeListener { _, checkedId -> setSpeedType(getSpeedType(checkedId)) }
    }

    private fun addConditional() {
        // first require selecting sth for normal limit
        if (!isFormComplete()) {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.quest_maxspeed_conditional_limited_enter_default)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        // then copy whatever answering is doing:

        // if nsl: ask dual carriageway
        if (speedType == NSL) {
            askIsDualCarriageway(
                onYes = { showConditionalDialog("nsl_dual") },
                onNo = { showConditionalDialog("nsl_single") }
            )
            return
        }
        // if no sign: ask urban/rural
        if (speedType == NO_SIGN) {
            val highwayTag = element.tags["highway"]!!
            if (countryInfo.countryCode == "GB") {
                if (ROADS_WITH_DEFINITE_SPEED_LIMIT_GB.contains(highwayTag)) {
                    showConditionalDialog(highwayTag)
                } else {
                    askIsDualCarriageway(
                        onYes = { showConditionalDialog("nsl_dual") },
                        onNo = {
                            determineLit(
                                onYes = { showConditionalDialog("nsl_restricted", true) },
                                onNo = { showConditionalDialog("nsl_single", false) }
                            )
                        }
                    )
                }
            } else if (ROADS_WITH_DEFINITE_SPEED_LIMIT.contains(highwayTag)) {
                showConditionalDialog(highwayTag)
            } else {
                askUrbanOrRural(
                    onUrban = { showConditionalDialog("urban") },
                    onRural = { showConditionalDialog("rural") }
                )
            }
            return
        }
        showConditionalDialog()
    }

    private fun showConditionalDialog(noSignAnswer: String? = null, lit: Boolean? = null) {
        showAddConditionalDialog(requireContext(), listOf("maxspeed", "maxspeed:hgv", "maxspeed:psv", "maxspeed:bus"), null, InputType.TYPE_CLASS_NUMBER) { k, v ->
            val speedText = v.substringBefore("@").trim()
            val speedNumber = speedText.toIntOrNull() ?: return@showAddConditionalDialog
            val speed = when (speedUnitSelect?.selectedItem as SpeedMeasurementUnit? ?: speedUnits.first()) {
                KILOMETERS_PER_HOUR -> Kmh(speedNumber)
                MILES_PER_HOUR -> Mph(speedNumber)
            }
            val value = v.replaceFirst(speedText, speed.toString())
            when (speedType) {
                NO_SIGN -> applyNoSignAnswer(noSignAnswer!!, null, k to value)
                NSL -> applyNoSignAnswer(noSignAnswer!!, lit, k to value)
                LIVING_STREET -> applyAnswer(IsLivingStreet to (k to value))
                else -> applySpeedLimitFormAnswer(k to value)
            }
        }
        return
    }

    override fun onClickOk() {
        if (speedType == NO_SIGN) {
            val couldBeSlowZone = countryInfo.hasSlowZone && POSSIBLY_SLOWZONE_ROADS.contains(element.tags["highway"])

            if (couldBeSlowZone) {
                confirmNoSignSlowZone { determineImplicitMaxspeedType() }
            } else {
                confirmNoSign { determineImplicitMaxspeedType() }
            }
        } else if (speedType == LIVING_STREET) {
            applyAnswer(IsLivingStreet to null)
        } else if (speedType == NSL) {
            askIsDualCarriageway(
                onYes = { applyNoSignAnswer("nsl_dual") },
                onNo = { applyNoSignAnswer("nsl_single") }
            )
        } else if (userSelectedUnusualSpeed()) {
            confirmUnusualInput { applySpeedLimitFormAnswer() }
        } else {
            applySpeedLimitFormAnswer()
        }
    }

    override fun isFormComplete() =
        speedType == NO_SIGN || speedType == LIVING_STREET || speedType == NSL || getSpeedFromInput() != null

    /* ---------------------------------------- With sign --------------------------------------- */

    private fun setSpeedType(speedType: SpeedType?) {
        this.speedType = speedType

        binding.rightSideContainer.removeAllViews()
        speedType?.layoutResId?.let { layoutInflater.inflate(it, binding.rightSideContainer, true) }

        speedInput = binding.rightSideContainer.findViewById(R.id.maxSpeedInput)
        speedInput?.doAfterTextChanged { checkIsFormComplete() }

        speedUnitSelect = binding.rightSideContainer.findViewById(R.id.speedUnitSelect)
        speedUnitSelect?.isGone = speedUnits.size == 1
        speedUnitSelect?.adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_centered, speedUnits)
        speedUnitSelect?.setSelection(0)

        when (speedType) {
            ZONE -> {
                enableAppropriateLabelsForSlowZone(binding.rightSideContainer)
            }
            LIVING_STREET -> {
                val drawableResId = countryInfo.livingStreetSignDrawableResId
                val livingStreetImageView = binding.rightSideContainer.findViewById<ImageView>(R.id.livingStreetImage)
                if (drawableResId != null) livingStreetImageView.setImageResource(drawableResId)
            }
            else -> {}
        }

        if (speedType == ZONE && LAST_INPUT_SLOW_ZONE != null) {
            speedInput?.setText(LAST_INPUT_SLOW_ZONE.toString())
        } else {
            speedInput?.requestFocus()
            speedInput?.showKeyboard()
        }

        checkIsFormComplete()
    }

    private fun getSpeedType(@IdRes checkedId: Int) = when (checkedId) {
        R.id.sign          -> SIGN
        R.id.zone          -> ZONE
        R.id.living_street -> LIVING_STREET
        R.id.nsl           -> NSL
        R.id.no_sign       -> NO_SIGN
        else -> null
    }

    private fun enableAppropriateLabelsForSlowZone(layoutWithSign: FrameLayout) {
        val position = countryInfo.slowZoneLabelPosition
        val text = countryInfo.slowZoneLabelText ?: return

        val label = layoutWithSign.findViewById<TextView>(when (position) {
            "bottom" -> R.id.slowZoneLabelBottom
            "top" -> R.id.slowZoneLabelTop
            else -> return // should never happen
        })
        label.visibility = View.VISIBLE
        label.text = text
    }

    private val SpeedType.layoutResId get() = when (this) {
        SIGN          -> getMaxSpeedSignLayoutResId(countryInfo.countryCode)
        ZONE          -> getMaxSpeedZoneSignLayoutResId(countryInfo.countryCode)
        LIVING_STREET -> R.layout.quest_maxspeed_living_street_sign
        NSL           -> R.layout.quest_maxspeed_national_speed_limit_sign
        ADVISORY      -> countryInfo.advisorySpeedLimitSignLayoutResId ?: R.layout.quest_maxspeed_advisory_blue
        else -> null
    }

    private fun userSelectedUnusualSpeed(): Boolean {
        val speed = getSpeedFromInput() ?: return false
        val kmh = speed.toKmh()
        return kmh > 140 || kmh > 20 && speed.toValue() % 5 != 0 || kmh < 10
    }

    private fun switchToAdvisorySpeedLimit() {
        binding.speedTypeSelect.clearCheck()
        for (child in binding.speedTypeSelect.children) {
            child.isEnabled = false
        }
        setSpeedType(ADVISORY)
    }

    private fun confirmUnusualInput(onConfirmed: () -> Unit) {
        activity?.let { AlertDialog.Builder(it)
            .setTitle(R.string.quest_generic_confirmation_title)
            .setMessage(R.string.quest_maxspeed_unusualInput_confirmation_description)
            .setPositiveButton(R.string.quest_generic_confirmation_yes) { _, _ -> onConfirmed() }
            .setNegativeButton(R.string.quest_generic_confirmation_no, null)
            .show()
        }
    }

    private fun applySpeedLimitFormAnswer(conditional: Pair<String, String>? = null) {
        val speed = getSpeedFromInput()!!
        when (speedType) {
            ADVISORY      -> applyAnswer(AdvisorySpeedSign(speed) to conditional)
            ZONE          -> {
                val zoneX = speed.toValue()
                LAST_INPUT_SLOW_ZONE = zoneX
                applyAnswer(MaxSpeedZone(speed, countryInfo.countryCode, "zone$zoneX") to conditional)
            }
            SIGN          -> applyAnswer(MaxSpeedSign(speed) to conditional)
            else          -> throw IllegalStateException()
        }
    }

    private fun getSpeedFromInput(): Speed? {
        val value = speedInput?.intOrNull ?: return null
        val unit = speedUnitSelect?.selectedItem as SpeedMeasurementUnit? ?: speedUnits.first()
        return when (unit) {
            KILOMETERS_PER_HOUR -> Kmh(value)
            MILES_PER_HOUR -> Mph(value)
        }
    }

    /* ----------------------------------------- No sign ---------------------------------------- */

    private fun confirmNoSign(onConfirmed: () -> Unit) {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle(R.string.quest_maxspeed_answer_noSign_confirmation_title)
                .setMessage(R.string.quest_maxspeed_answer_noSign_confirmation)
                .setPositiveButton(R.string.quest_maxspeed_answer_noSign_confirmation_positive) { _, _ -> onConfirmed() }
                .setNegativeButton(R.string.quest_generic_confirmation_no, null)
                .show()
        }
    }

    private fun confirmNoSignSlowZone(onConfirmed: () -> Unit) {
        activity?.let {
            val dialogBinding = QuestMaxspeedNoSignNoSlowZoneConfirmationBinding.inflate(layoutInflater)
            enableAppropriateLabelsForSlowZone(dialogBinding.slowZoneImage)
            dialogBinding.slowZoneImage.removeAllViews()
            layoutInflater.inflate(
                getMaxSpeedZoneSignLayoutResId(countryInfo.countryCode),
                dialogBinding.slowZoneImage,
                true,
            )
            val dialogSpeedInput: EditText = dialogBinding.slowZoneImage.findViewById(R.id.maxSpeedInput)
            dialogSpeedInput.setText("××")
            dialogSpeedInput.inputType = EditorInfo.TYPE_NULL

            AlertDialog.Builder(it)
                .setTitle(R.string.quest_maxspeed_answer_noSign_confirmation_title)
                .setView(dialogBinding.root)
                .setPositiveButton(R.string.quest_maxspeed_answer_noSign_confirmation_positive) { _, _ -> onConfirmed() }
                .setNegativeButton(R.string.quest_generic_confirmation_no, null)
                .show()
        }
    }

    private fun determineImplicitMaxspeedType() {
        val highwayTag = element.tags["highway"]!!
        if (ROADS_WITH_DEFINITE_SPEED_LIMIT.contains(highwayTag)) {
            applyNoSignAnswer(highwayTag)
        } else if (countryInfo.countryCode == "GB") {
            askIsDualCarriageway(
                onYes = { applyNoSignAnswer("nsl_dual") },
                onNo = {
                    determineLit(
                        onYes = { applyNoSignAnswer("nsl_restricted", true) },
                        onNo = { applyNoSignAnswer("nsl_single", false) }
                    )
                }
            )
        } else {
            askUrbanOrRural(
                onUrban = { applyNoSignAnswer("urban") },
                onRural = { applyNoSignAnswer("rural") }
            )
        }
    }

    private fun askUrbanOrRural(onUrban: () -> Unit, onRural: () -> Unit) {
        activity?.let {
            AlertDialog.Builder(it)
                .setTitle(R.string.quest_maxspeed_answer_noSign_info_urbanOrRural)
                .setMessage(R.string.quest_maxspeed_answer_noSign_urbanOrRural_description)
                .setPositiveButton(R.string.quest_maxspeed_answer_noSign_urbanOk) { _, _ -> onUrban() }
                .setNegativeButton(R.string.quest_maxspeed_answer_noSign_ruralOk) { _, _ -> onRural() }
                .show()
        }
    }

    private fun determineLit(onYes: () -> Unit, onNo: () -> Unit) {
        val lit = element.tags["lit"]
        when (lit) {
            "yes" -> onYes()
            "no" -> onNo()
            else -> askLit(onYes, onNo)
        }
    }

    private fun askLit(onYes: () -> Unit, onNo: () -> Unit) {
        activity?.let {
            AlertDialog.Builder(it)
                .setMessage(R.string.quest_lit_title)
                .setPositiveButton(R.string.quest_generic_hasFeature_yes) { _, _ -> onYes() }
                .setNegativeButton(R.string.quest_generic_hasFeature_no) { _, _ -> onNo() }
                .show()
        }
    }

    private fun askIsDualCarriageway(onYes: () -> Unit, onNo: () -> Unit) {
        activity?.let {
            AlertDialog.Builder(it)
                .setMessage(R.string.quest_maxspeed_answer_noSign_singleOrDualCarriageway_description)
                .setPositiveButton(R.string.quest_generic_hasFeature_yes) { _, _ -> onYes() }
                .setNegativeButton(R.string.quest_generic_hasFeature_no) { _, _ -> onNo() }
                .show()
        }
    }

    private fun applyNoSignAnswer(roadType: String, lit: Boolean? = null, conditional: Pair<String, String>? = null) {
        applyAnswer(ImplicitMaxSpeed(countryInfo.countryCode, roadType, lit) to conditional)
    }

    companion object {
        private val POSSIBLY_SLOWZONE_ROADS = listOf("residential", "unclassified", "tertiary" /*#1133*/)
        private val MAYBE_LIVING_STREET = listOf("residential", "unclassified")
        private val ROADS_WITH_DEFINITE_SPEED_LIMIT = listOf("motorway", "living_street")

        private var LAST_INPUT_SLOW_ZONE: Int? = null
    }
}

private enum class SpeedType {
    SIGN, ZONE, LIVING_STREET, ADVISORY, NO_SIGN, NSL
}

private fun getMaxSpeedSignLayoutResId(countryCode: String): Int = when (countryCode) {
    "FI", "IS", "SE" -> R.layout.quest_maxspeed_sign_fi
    "CA" ->             R.layout.quest_maxspeed_sign_ca
    "US" ->             R.layout.quest_maxspeed_sign_us
    else ->             R.layout.quest_maxspeed_sign
}

private fun getMaxSpeedZoneSignLayoutResId(countryCode: String): Int = when (countryCode) {
    "FI", "IS", "SE" -> R.layout.quest_maxspeed_zone_sign_fi
    "IL" ->             R.layout.quest_maxspeed_zone_sign_il
    else ->             R.layout.quest_maxspeed_zone_sign
}
