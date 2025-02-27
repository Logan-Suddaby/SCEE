package de.westnordost.streetcomplete.quests.guidepost

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.databinding.QuestGuidepostRefBinding
import de.westnordost.streetcomplete.quests.AbstractOsmQuestForm
import de.westnordost.streetcomplete.quests.AnswerItem
import de.westnordost.streetcomplete.util.ktx.nonBlankTextOrNull

class AddGuidepostNameForm : AbstractOsmQuestForm<GuidepostNameAnswer>() {

    override val contentLayoutResId = R.layout.quest_guidepost_ref
    private val binding by contentViewBinding(QuestGuidepostRefBinding::bind)

    override val otherAnswers = listOf(
        AnswerItem(R.string.quest_placeName_no_name_answer) { confirmNoRef() }
    )

    private val name get() = binding.refInput.nonBlankTextOrNull

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.refInput.doAfterTextChanged { checkIsFormComplete() }
        binding.tvHint.setText(R.string.quest_guidepostName_hint)
        binding.refInput.inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
    }

    override fun onClickOk() {
        applyAnswer(GuidepostName(name!!))
    }

    private fun confirmNoRef() {
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle(R.string.quest_generic_confirmation_title)
            .setPositiveButton(R.string.quest_generic_confirmation_yes) { _, _ -> applyAnswer(NoVisibleGuidepostName) }
            .setNegativeButton(R.string.quest_generic_confirmation_no, null)
            .show()
    }

    override fun isFormComplete() = name != null
}
