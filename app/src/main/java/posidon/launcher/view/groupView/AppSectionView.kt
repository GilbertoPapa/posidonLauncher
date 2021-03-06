package posidon.launcher.view.groupView

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import posidon.launcher.R
import posidon.launcher.items.App
import posidon.launcher.items.LauncherItem
import posidon.launcher.items.users.ItemLongPress
import posidon.launcher.storage.Settings
import posidon.launcher.tools.Tools
import posidon.launcher.tools.dp

class AppSectionView(context: Context) : ItemGroupView(context) {

    private val appSize = when (Settings["icsize", 1]) {
        0 -> 64.dp.toInt()
        2 -> 84.dp.toInt()
        else -> 74.dp.toInt()
    }

    init {
        when (Settings["drawer:sec_name_pos", 0]) {
            0 -> {
                orientation = VERTICAL
            }
            1 -> {
                orientation = HORIZONTAL
                textView.layoutParams.run {
                    width = WRAP_CONTENT
                }
                textView.setPaddingRelative(12.dp.toInt(), 0, 0, 0)
            }
        }

        gridLayout.run {
            columnCount = Settings["drawer:columns", 4]
            if (Settings["drawer:columns", 4] > 2) {
                setPaddingRelative(12.dp.toInt(), 0, 0, 0)
            }
        }
        textView.run {
            setTextColor(Settings["labelColor", -0x11111112])
        }
    }

    override fun getItemView (item: LauncherItem): View {
        item as App
        return (if (Settings["drawer:columns", 4] > 2) LayoutInflater.from(context).inflate(R.layout.drawer_item, gridLayout, false)
        else LayoutInflater.from(context).inflate(R.layout.list_item, gridLayout, false).apply {
            if (Settings["drawer:columns", 4] == 2) findViewById<TextView>(R.id.icontxt).textSize = 18f
        }).apply {
            findViewById<ImageView>(R.id.iconimg).setImageDrawable(item.icon)
            findViewById<View>(R.id.iconFrame).run {
                layoutParams.height = appSize
                layoutParams.width = appSize
            }
            findViewById<TextView>(R.id.icontxt).run {
                if (Settings["labelsenabled", false]) {
                    text = item.label
                    visibility = View.VISIBLE
                    setTextColor(Settings["labelColor", -0x11111112])
                } else visibility = View.GONE
            }
            findViewById<TextView>(R.id.notificationBadge).run {
                if (Settings["notif:badges", true] && item.notificationCount != 0) {
                    visibility = View.VISIBLE
                    text = if (Settings["notif:badges:show_num", true]) item.notificationCount.toString() else ""
                    Tools.generateNotificationBadgeBGnFG(item.icon!!) { bg, fg ->
                        background = bg
                        setTextColor(fg)
                    }
                } else { visibility = View.GONE }
            }
            setOnClickListener { item.open(context, it) }
            setOnLongClickListener(ItemLongPress.drawer(context, item))
            (layoutParams as GridLayout.LayoutParams).bottomMargin = Settings["verticalspacing", 12].dp.toInt()
        }
    }
}