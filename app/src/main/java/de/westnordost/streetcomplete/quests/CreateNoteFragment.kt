package de.westnordost.streetcomplete.quests

import android.content.res.Configuration
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.BounceInterpolator
import android.view.animation.TranslateAnimation

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.ktx.getLocationInWindow
import de.westnordost.streetcomplete.ktx.hideKeyboard
import kotlinx.android.synthetic.main.form_leave_note.*
import kotlinx.android.synthetic.main.fragment_quest_answer.*
import kotlinx.android.synthetic.main.marker_create_note.*

/** Bottom sheet fragment with which the user can create a new note, including moving the note */
class CreateNoteFragment : AbstractCreateNoteFragment() {

    interface Listener {
        /** Called when the user wants to leave a note which is not related to a quest  */
        fun onCreatedNote(note: String, imagePaths: List<String>, screenPosition: Point)
        fun onCreatedNote(note: String, screenPosition: Point)
    }
    private val listener: Listener? get() = parentFragment as? Listener
            ?: activity as? Listener

    override val layoutResId = R.layout.fragment_create_note

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            markerLayoutContainer?.startAnimation(createFallDownAnimation())
        }

        titleLabel.text = getString(R.string.map_btn_create_note)
        descriptionLabel.text = getString(R.string.create_new_note_description)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        centeredMarkerLayout.setPadding(
            resources.getDimensionPixelSize(R.dimen.quest_form_leftOffset),
            resources.getDimensionPixelSize(R.dimen.quest_form_topOffset),
            resources.getDimensionPixelSize(R.dimen.quest_form_rightOffset),
            resources.getDimensionPixelSize(R.dimen.quest_form_bottomOffset)
        )
    }

    private fun createFallDownAnimation(): Animation {
        val a = AnimationSet(false)
        a.startOffset = 200

        val ta = TranslateAnimation(0, 0f, 0, 0f, 1, -0.2f, 0, 0f)
        ta.interpolator = BounceInterpolator()
        ta.duration = 400
        a.addAnimation(ta)

        val aa = AlphaAnimation(0f, 1f)
        aa.interpolator = AccelerateInterpolator()
        aa.duration = 200
        a.addAnimation(aa)

        return a
    }

    override fun onDiscard() {
        super.onDiscard()
        markerLayoutContainer?.visibility = View.INVISIBLE
    }

    override fun onComposedNote(text: String, imagePaths: List<String>) {
        /* pressing once on "OK" should first only close the keyboard, so that the user can review
           the position of the note he placed */
        if (noteInput.hideKeyboard() == true) return

        val screenPos = createNoteMarker.getLocationInWindow()
        screenPos.offset(createNoteMarker.width / 2, createNoteMarker.height / 2)

        markerLayoutContainer?.visibility = View.INVISIBLE

        listener?.onCreatedNote(text, imagePaths, screenPos)
    }

    override fun onComposedNote(text: String) {
        val screenPos = createNoteMarker.getLocationInWindow()
        screenPos.offset(createNoteMarker.width / 2, createNoteMarker.height / 2)

        markerLayoutContainer?.visibility = View.INVISIBLE

        listener?.onCreatedNote(text, screenPos)
    }
}
