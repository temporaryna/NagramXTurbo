package tw.nekomimi.nekogram.ui

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.View
import org.telegram.ui.ActionBar.ActionBarMenuItem
import org.telegram.ui.ActionBar.Theme

@SuppressLint("ViewConstructor")
class PopupBuilder @JvmOverloads constructor(anchor: View, dialog: Boolean = false) : ActionBarMenuItem(anchor.context, null, Theme.ACTION_BAR_WHITE_SELECTOR_COLOR, -0x4c4c4d) {

    init {

        setAnchor(anchor)

        isShowOnTop = dialog

        isVerticalScrollBarEnabled = true

    }

    fun setItems(items: Array<CharSequence?>, listener: (Int, CharSequence) -> Unit) {
        setItems(items, arrayOfNulls(0), listener)
    }

    fun setItems(items: List<CharSequence?>, listener: (Int, CharSequence) -> Unit) {

        removeAllSubItems()

        for (item in items) {
            if (item == null) continue
            addSubItem(items.indexOf(item), item)
        }

        setDelegate {

            listener(it, items[it]!!)

        }

    }

    fun setItems(items: Array<CharSequence?>, icons: Array<Drawable?>, listener: (Int, CharSequence) -> Unit) {

        removeAllSubItems()

        for (i in items.indices) {
            val item = items[i] ?: continue
            if (i < icons.size && icons[i] != null) {
                addSubItem(i, icons[i], item, null)
            } else {
                addSubItem(i, item)
            }
        }

        setDelegate {

            listener(it, items[it]!!)

        }

    }

    fun show() {

        toggleSubMenu()

    }

}
