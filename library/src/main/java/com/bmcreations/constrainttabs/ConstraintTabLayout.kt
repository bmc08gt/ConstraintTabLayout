package com.bmcreations.constrainttabs

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.content.res.use

data class Tab(val label: String,
               val iconResId: Int? = null,
               val hideLabel: Boolean = false) {
    var index = 0
}

data class TabView (val tab: Tab, val tv: AppCompatTextView)

var View.selected: Boolean by FieldProperty { false }

@SuppressLint("Recycle")
class ConstraintTabLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {

    val providedTabs: MutableList<Tab> = mutableListOf()
    val tabs: MutableList<TabView> = mutableListOf()

    private var unselectedBackground: Drawable = context.getDrawable(R.drawable.default_unselected_bg)!!
    private var selectedBackground: Drawable = context.getDrawable(R.drawable.default_selected_bg)!!
    private var unselectedTextColor: Int = context.getColorResCompat(android.R.attr.textColorPrimary)
    private var selectedTextColor: Int = context.getColorResCompat(android.R.attr.textColorPrimaryInverse)

    interface OnTabSelectedListener {
        fun onTabSelected(tab: Tab)
        fun onTabReselected(tab: Tab)
    }

    private var group: Group

    private var laidOut = false

    var callback: OnTabSelectedListener? = null

    var selection: TabView? = null

    init {
        var initialSelectionIndex = 0

        attrs?.let { attributes ->
            context.obtainStyledAttributes(attributes, R.styleable.ConstraintTabLayout).use {
                if (it.hasValue(R.styleable.ConstraintTabLayout_tabs_unselectedBg)) {
                    val unselected = it.getDrawable(R.styleable.ConstraintTabLayout_tabs_unselectedBg)
                    unselectedBackground = unselected ?: context.getDrawable(R.drawable.default_unselected_bg)!!
                }

                if (it.hasValue(R.styleable.ConstraintTabLayout_tabs_unselectedTextColor)) {
                    unselectedTextColor = it.getColor(R.styleable.ConstraintTabLayout_tabs_unselectedTextColor, android.R.attr.textColorPrimary)
                }

                if (it.hasValue(R.styleable.ConstraintTabLayout_tabs_selectedBg)) {
                    val selected = it.getDrawable(R.styleable.ConstraintTabLayout_tabs_selectedBg)
                    selectedBackground = selected ?: context.getDrawable(R.drawable.default_selected_bg)!!
                }

                if (it.hasValue(R.styleable.ConstraintTabLayout_tabs_selectedTextColor)) {
                    selectedTextColor = it.getColor(R.styleable.ConstraintTabLayout_tabs_selectedTextColor, android.R.attr.textColorPrimaryInverse)
                }

                if (it.hasValue(R.styleable.ConstraintTabLayout_tabs_selection)) {
                    initialSelectionIndex = it.getInt(R.styleable.ConstraintTabLayout_tabs_selection, 0)
                }
            }
        }

        group = Group(context, attrs, defStyleAttr)
        ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT).also { group.layoutParams = it }

        // TODO: Add tabs in xml
        updateTabs(initialSelectionIndex)
    }

    private fun updateTabs(initialIndex: Int = 0) {
        tabs.clear()
        providedTabs.mapIndexed { index, tab ->
            TabView(tab.apply { this.index = index }, AppCompatTextView(context))
        }.forEach {
            it.tv.id = View.generateViewId()
            it.tv.tag = "constraint_tab_${it.tab.label}--${it.tab.index}"
            it.tv.background = unselectedBackground
            it.tv.text = it.tab.label
            it.tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
            it.tv.gravity = Gravity.CENTER_VERTICAL
            it.tv.setTextColor(unselectedTextColor)
            it.tv.setOnClickListener { v ->
                reset()
                select(it, v.selected)
            }

            tabs.add(it)
            addView(it.tv, it.tab.index)

            if (initialIndex == it.tab.index) {
                selection = it
            }

            setConstraintsFor(it)
        }

        // Map tabs to group references
        tabs.map { it.tv.id }.toIntArray().also { group.referencedIds = it }

        if (tabs.size > 1) {
            // Setup horizontal chain
            val constraints = ConstraintSet()
            constraints.clone(this)
            constraints.createHorizontalChain(
                ConstraintSet.PARENT_ID, ConstraintSet.LEFT,
                ConstraintSet.PARENT_ID, ConstraintSet.RIGHT,
                group.referencedIds, null, ConstraintSet.CHAIN_SPREAD_INSIDE
            )
            // Apply chain
            constraints.applyTo(this)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!laidOut) {
            laidOut = true
            select(selection ?: tabs.first(), notify = false)
        }
    }

    private fun setConstraintsFor(v: TabView) {
        val constraints = ConstraintSet()
        constraints.clone(this)

        constraints.constrainWidth(v.tv.id, 0.dp)
        constraints.constrainHeight(v.tv.id, 32.dp)

        when {
            v.tab.index == 0 -> {
                constraints.setMargin(v.tv.id, ConstraintSet.START, 0.dp)
                constraints.setMargin(v.tv.id, ConstraintSet.END, 16.dp)
            }
            v.tab.index in 1 until providedTabs.lastIndex -> {
                constraints.setMargin(v.tv.id, ConstraintSet.START, 16.dp)
                constraints.setMargin(v.tv.id, ConstraintSet.END, 16.dp)
            }
            v.tab.index == providedTabs.lastIndex -> {
                constraints.setMargin(v.tv.id, ConstraintSet.START, 16.dp)
                constraints.setMargin(v.tv.id, ConstraintSet.END, 0.dp)
            }
        }
        constraints.applyTo(this)
    }

    fun addTabs(vararg tabs: Tab) {
        providedTabs.addAll(tabs)
        updateTabs()
    }

    fun addTab(tab: Tab) {
        providedTabs.add(tab)
        updateTabs()
    }

    fun select(position: Int) {
        tabs.find { it.tab.index == position }?.let { select(it) }
    }

    private fun reset() {
        group.referencedIds.forEach { id -> tabs.find { it.tv.id == id }?.let {
            it.tv.selected = false
            it.tv.setTextColor(unselectedTextColor)
            it.tv.background = unselectedBackground
        } }
    }

    private fun select(test: TabView, reselect: Boolean = false, notify: Boolean = true) {
        val tag = test.tv.tag as String

        rootView.findViewWithTag<View>(tag)?.let {
            this.selection = test
            it.background = selectedBackground
            test.tv.setTextColor(selectedTextColor)
            it.selected = true
            if (notify) {
                if (reselect) {
                    callback?.onTabReselected(test.tab)
                } else {
                    callback?.onTabSelected(test.tab)
                }
            }
            runTabSelectionAnimation(it)
        }
    }

    private fun runTabSelectionAnimation(view: View) {
        val selectXAnimator = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.8f, 1f)
        val selectYAnimator = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.8f, 1f)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(selectXAnimator, selectYAnimator)
        animatorSet.interpolator = OvershootInterpolator(1.3f)
        animatorSet.duration = 300
        animatorSet.start()
    }

    val Number.dp: Int
        get() = context.resources.displayMetrics.run {
            Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this@dp.toFloat(), this))
        }

}

@ColorInt
fun Context.getColorResCompat(@AttrRes id: Int): Int {
    val resolvedAttr = TypedValue()
    this.theme.resolveAttribute(id, resolvedAttr, true)
    val colorRes = resolvedAttr.run { if (resourceId != 0) resourceId else data }
    return ContextCompat.getColor(this, colorRes)
}