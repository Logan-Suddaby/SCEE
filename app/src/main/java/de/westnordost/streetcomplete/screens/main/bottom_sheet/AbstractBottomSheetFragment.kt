package de.westnordost.streetcomplete.screens.main.bottom_sheet

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.quests.TagEditor
import de.westnordost.streetcomplete.util.ktx.dpToPx
import de.westnordost.streetcomplete.util.ktx.updateMargins
import de.westnordost.streetcomplete.util.ktx.viewLifecycleScope
import de.westnordost.streetcomplete.util.prefs.Preferences
import de.westnordost.streetcomplete.view.RoundRectOutlineProvider
import de.westnordost.streetcomplete.view.SlidingRelativeLayout
import de.westnordost.streetcomplete.view.insets_animation.respectSystemInsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.math.min

/** Abstract base class for expandable and closeable bottom sheets. In detail, it does manage the
 *  behavior of the...
 *
 *  - bottom sheet
 *  - title view that acts like a button to expand/retract the bottom sheet
 *  - close/back button that is only shown when the bottom sheet is fully expanded
 * */
abstract class AbstractBottomSheetFragment : Fragment(), IsCloseableBottomSheet {

    protected abstract val bottomSheetContainer: SlidingRelativeLayout
    protected abstract val bottomSheet: ViewGroup
    protected abstract val scrollViewChild: View
    protected val prefs: Preferences by inject()

    /** Title view of the bottom sheet. Tapping on it expands / retracts the bottom sheet */
    protected abstract val bottomSheetTitle: View?
    /** Content view of the bottom sheet */
    protected abstract val bottomSheetContent: View?

    /** View that floats at the bottom on top of any retracted/expanded bottom sheet */
    protected abstract val floatingBottomView: View?
    protected abstract val floatingBottomView2: View?
    /** View that is only shown when the bottom sheet is expanded and acts like a back-button */
    protected abstract val backButton: View?

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {}

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            updateCloseButtonVisibility()
        }
    }

    private var minBottomInset = Int.MAX_VALUE

    // overridable by child classes
    open val defaultExpanded = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomSheet.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            viewLifecycleScope.launch {
                // not immediately because this is called during layout change (view.getTop() == 0)
                delay(1)
                updateCloseButtonVisibility()
            }
        }

        backButton?.setOnClickListener { activity?.onBackPressed() }

        minBottomInset = Int.MAX_VALUE
        view.respectSystemInsets {
            scrollViewChild.updatePadding(bottom = it.bottom)
            bottomSheetContainer.updateMargins(top = it.top, left = it.left, right = it.right)
            floatingBottomView?.updateMargins(bottom = it.bottom)
            floatingBottomView2?.updateMargins(bottom = it.bottom + context.dpToPx(8).toInt())

            // expanding bottom sheet when keyboard is opened
            if (minBottomInset < it.bottom) expand()
            minBottomInset = min(it.bottom, minBottomInset)
        }

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        val cornerRadius = resources.getDimension(R.dimen.speech_bubble_rounded_corner_radius)
        val margin = resources.getDimensionPixelSize(R.dimen.horizontal_speech_bubble_margin)
        val topMargin = -resources.getDimensionPixelSize(R.dimen.quest_form_speech_bubble_top_margin)
        bottomSheetTitle?.outlineProvider = RoundRectOutlineProvider(
            cornerRadius, margin, topMargin, margin, margin
        )

        bottomSheetContent?.outlineProvider = RoundRectOutlineProvider(
            cornerRadius, margin, margin, margin, margin
        )

        bottomSheetTitle?.setOnClickListener {
            bottomSheetBehavior.apply {
                if (state == STATE_EXPANDED) {
                    state = STATE_COLLAPSED
                } else if (state == STATE_COLLAPSED) {
                    state = STATE_EXPANDED
                }
            }
        }

        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || defaultExpanded) {
            expand()
        }

        if (savedInstanceState == null) {
            bottomSheetTitle?.startAnimation(
                AnimationUtils.loadAnimation(context, R.anim.inflate_title_bubble)
            )

            bottomSheetContent?.startAnimation(
                AnimationUtils.loadAnimation(context, R.anim.inflate_answer_bubble)
            )
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // I need to do everything myself... (AppCompactActivity only does this after calling this
        // method. Genius!)
        resources.updateConfiguration(newConfig, resources.displayMetrics)

        bottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(R.dimen.quest_form_peekHeight)
        bottomSheetContainer.updateLayoutParams { width = resources.getDimensionPixelSize(R.dimen.quest_form_width) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback)
    }

    fun expand() {
        bottomSheetBehavior.state = STATE_EXPANDED
    }

    private fun updateCloseButtonVisibility() {
        backButton?.isGone = (bottomSheet.top) > 0
    }

    @UiThread
    override fun onClickMapAt(position: LatLon, clickAreaSizeInMeters: Double): Boolean {
        return false
    }

    /** Request to close the form through user interaction (back button, clicked other quest,..),
     * requires user confirmation if any changes have been made  */
    @UiThread
    override fun onClickClose(onConfirmed: () -> Unit) {
        // changes != null means we just answered a quest inside tag editor
        if (TagEditor.changes != null || !isRejectingClose()) {
            onDiscard()
            onConfirmed()
        } else {
            activity?.let {
                AlertDialog.Builder(it)
                    .setMessage(R.string.confirmation_discard_title)
                    .setPositiveButton(R.string.confirmation_discard_positive) { _, _ ->
                        onDiscard()
                        onConfirmed()
                    }
                    .setNegativeButton(R.string.short_no_answer_on_button, null)
                    .show()
            }
        }
    }

    /** returns whether this form should not be closeable without confirmation */
    open fun isRejectingClose(): Boolean = false

    protected open fun onDiscard() {}
}
