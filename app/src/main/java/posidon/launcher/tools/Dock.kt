package posidon.launcher.tools

import android.os.Build
import posidon.launcher.items.App
import posidon.launcher.items.Folder
import posidon.launcher.items.LauncherItem
import posidon.launcher.items.Shortcut
import posidon.launcher.storage.Settings

object Dock {

    fun add(item: LauncherItem, i: Int) {
        val data = Settings.getString("dock:icon:$i")
        when {
            data == null || data == "" || data == "null" -> {
                Settings["dock:icon:$i"] = item.toString()
            }
            item is App || item is Shortcut -> {
                if (data.startsWith("folder:"))
                    Settings["dock:icon:$i"] = "folder:" + data.substring(7, data.length) + "\t" + item.toString()
                else Settings["dock:icon:$i"] = "folder:${Tools.generateFolderUid()}\t$data\t$item"
            }
            item is Folder -> {
                var folderContent = item.toString().substring(7, item.toString().length)
                if (data.startsWith("folder:")) {
                    folderContent = folderContent.substring(folderContent.indexOf('\t') + 1)
                    Settings["dock:icon:$i"] = "folder:" + data.substring(7, data.length) + "\t" + folderContent
                } else Settings["dock:icon:$i"] = "folder:$folderContent\t$data"
            }
        }
    }

    fun convert() {
        val data: Array<String?> = Settings["dock", ""].split("\n").toTypedArray()
        for (i in data.indices) {
            val string = data[i] ?: continue
            Settings["dock:icon:$i"] = when {
                string.startsWith("folder(") && string.endsWith(')') -> string.substring(0, string.length - 1).replaceFirst('(', ':').replace('¬', '\t')
                else -> string
            }
        }
    }

    operator fun get(i: Int) = Settings.getString("dock:icon:$i")?.let { LauncherItem(it).apply {
        if (this is Folder && uid.length != 8) {
            val label = uid
            uid = Tools.generateFolderUid()
            Settings["folder:$uid:label"] = label
            Dock[i] = this
        }
    }}

    operator fun iterator() = object : Iterator<LauncherItem?> {

        var i = 0
        val iconCount = Settings["dock:columns", 5] * Settings["dock:rows", 1]

        override fun hasNext() = i < iconCount

        override fun next(): LauncherItem? {
            val string = Settings.getString("dock:icon:${i++}") ?: return null
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && string.startsWith("shortcut:") -> Shortcut(string)
                string.startsWith("folder:") -> Folder(string)
                else -> App[string]
            }
        }
    }

    operator fun set(i: Int, item: LauncherItem?) {
        Settings["dock:icon:$i"] = item?.toString()
    }
}