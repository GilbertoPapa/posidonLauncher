package posidon.launcher.items.users

import android.content.Context
import android.content.pm.LauncherApps
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.*
import androidx.palette.graphics.Palette
import posidon.launcher.Main
import posidon.launcher.items.App
import posidon.launcher.storage.Settings
import posidon.launcher.tools.ThemeTools
import posidon.launcher.tools.Tools
import posidon.launcher.tools.dp
import posidon.launcher.tools.toBitmap
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class AppLoader (
    context: Context,
    private val onEnd: () -> Unit
) : AsyncTask<Unit?, Unit?, Unit?>() {

    class Callback (val context: Context, val onAppLoaderEnd: () -> Unit) : LauncherApps.Callback() {

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle?, replacing: Boolean) {
            AppLoader(context, onAppLoaderEnd).execute()
        }

        override fun onPackageChanged(packageName: String, user: UserHandle?) {
            AppLoader(context, onAppLoaderEnd).execute()
        }

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle?, replacing: Boolean) {
            AppLoader(context, onAppLoaderEnd).execute()
        }

        override fun onPackageAdded(packageName: String, user: UserHandle?) {
            AppLoader(context, onAppLoaderEnd).execute()
        }

        override fun onPackageRemoved(packageName: String, user: UserHandle?) {
            Main.apps.removeAll { it.packageName == packageName }
            val iter = Main.appSections.iterator()
            for (section in iter) {
                section.removeAll {
                    it.packageName == packageName
                }
                if (section.isEmpty()) {
                    iter.remove()
                }
            }
            App.removePackage(packageName)
            onAppLoaderEnd()
        }
    }

    private var tmpApps = ArrayList<App>()
    private val tmpAppSections = ArrayList<ArrayList<App>>()
    private val context: WeakReference<Context> = WeakReference(context)

    override fun doInBackground(objects: Array<Unit?>): Unit? {
        App.hidden.clear()
        val packageManager = context.get()!!.packageManager
        val ICONSIZE = 65.dp.toInt()
        val iconPackName = Settings["iconpack", "system"]
        val p = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
        }
        val maskp = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        var iconPackInfo = ThemeTools.IconPackInfo()
        var themeRes: Resources? = null
        val uniformOptions = BitmapFactory.Options().apply {
            inScaled = false
        }
        var back: Bitmap? = null
        var mask: Bitmap? = null
        var front: Bitmap? = null
        var areUnthemedIconsChanged = false
        try {
            themeRes = packageManager.getResourcesForApplication(iconPackName)
            iconPackInfo = ThemeTools.getIconPackInfo(themeRes, iconPackName)
            if (iconPackInfo.iconBack != null) {
                val intresiconback = themeRes.getIdentifier(iconPackInfo.iconBack, "drawable", iconPackName)
                if (intresiconback != 0) {
                    back = BitmapFactory.decodeResource(themeRes, intresiconback, uniformOptions)
                    areUnthemedIconsChanged = true
                }
            }
            if (iconPackInfo.iconMask != null) {
                val intresiconmask = themeRes.getIdentifier(iconPackInfo.iconMask, "drawable", iconPackName)
                if (intresiconmask != 0) {
                    mask = BitmapFactory.decodeResource(themeRes, intresiconmask, uniformOptions)
                    areUnthemedIconsChanged = true
                }
            }
            if (iconPackInfo.iconFront != null) {
                val intresiconfront = themeRes.getIdentifier(iconPackInfo.iconFront, "drawable", iconPackName)
                if (intresiconfront != 0) {
                    front = BitmapFactory.decodeResource(themeRes, intresiconfront, uniformOptions)
                    areUnthemedIconsChanged = true
                }
            }
        } catch (e: Exception) {}

        val userManager = Main.instance.getSystemService(Context.USER_SERVICE) as UserManager
        var lastThread: Thread? = null

        for (profile in userManager.userProfiles) {

            val appList = Main.launcherApps.getActivityList(null, profile)

            for (i in appList.indices) {

                val app = App(appList[i].applicationInfo.packageName, appList[i].name, profile)

                val thread = thread (isDaemon = true) {
                    val customIcon = Settings["app:$app:icon", ""]
                    if (customIcon != "") {
                        try {
                            val data = customIcon.split(':').toTypedArray()[1].split('|').toTypedArray()
                            val t = packageManager.getResourcesForApplication(data[0])
                            val intRes = t.getIdentifier(data[1], "drawable", data[0])
                            app.icon = t.getDrawable(intRes)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            app.icon = appList[i].getIcon(0)
                        }
                    } else {
                        var intres = 0
                        val iconResource = iconPackInfo.iconResourceNames["ComponentInfo{" + app.packageName + "/" + app.name + "}"]
                        if (iconResource != null) {
                            intres = themeRes!!.getIdentifier(iconResource, "drawable", iconPackName)
                        }
                        if (intres != 0) {
                            try {
                                app.icon = themeRes!!.getDrawable(intres)
                            } catch (e: Exception) {
                                app.icon = appList[i].getIcon(0)
                            }
                        } else {
                            app.icon = appList[i].getIcon(0)
                            if (areUnthemedIconsChanged) {
                                try {
                                    var orig = Bitmap.createBitmap(app.icon!!.intrinsicWidth, app.icon!!.intrinsicHeight, Bitmap.Config.ARGB_8888)
                                    app.icon!!.setBounds(0, 0, app.icon!!.intrinsicWidth, app.icon!!.intrinsicHeight)
                                    app.icon!!.draw(Canvas(orig))
                                    val scaledOrig = Bitmap.createBitmap(ICONSIZE, ICONSIZE, Bitmap.Config.ARGB_8888)
                                    val scaledBitmap = Bitmap.createBitmap(ICONSIZE, ICONSIZE, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(scaledBitmap)
                                    if (back != null) {
                                        canvas.drawBitmap(back, Tools.getResizedMatrix(back, ICONSIZE, ICONSIZE), p)
                                    }
                                    val origCanv = Canvas(scaledOrig)
                                    orig = Tools.getResizedBitmap(orig, (ICONSIZE * iconPackInfo.scaleFactor).toInt(), (ICONSIZE * iconPackInfo.scaleFactor).toInt())
                                    origCanv.drawBitmap(orig, scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f, scaledOrig.width - orig.width / 2f - scaledOrig.width / 2f, p)
                                    if (mask != null) {
                                        origCanv.drawBitmap(mask, Tools.getResizedMatrix(mask, ICONSIZE, ICONSIZE), maskp)
                                    }
                                    canvas.drawBitmap(Tools.getResizedBitmap(scaledOrig, ICONSIZE, ICONSIZE), 0f, 0f, p)
                                    if (front != null) {
                                        canvas.drawBitmap(front, Tools.getResizedMatrix(front, ICONSIZE, ICONSIZE), p)
                                    }
                                    app.icon = BitmapDrawable(context.get()!!.resources, scaledBitmap)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        app.icon = Tools.generateAdaptiveIcon(app.icon!!)
                    }
                    app.icon = Tools.badgeMaybe(app.icon!!, appList[i].user != Process.myUserHandle())
                    if (!(context.get()!!.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode &&
                        Settings["animatedicons", true]) {
                        try {
                            Tools.tryAnimate(app.icon!!)
                        } catch (e: Exception) {}
                    }
                }

                app.label = Settings[app.packageName + "/" + app.name + "?label", appList[i].label.toString()]
                if (app.label!!.isEmpty()) {
                    Settings[app.packageName + "/" + app.name + "?label"] = appList[i].label.toString()
                    app.label = appList[i].label.toString()
                    if (app.label!!.isEmpty()) {
                        app.label = app.packageName
                    }
                }

                App.putInSecondMap(app)
                if (Settings["app:$app:hidden", false]) {
                    App.hidden.add(app)
                } else {
                    tmpApps.add(app)
                }
                lastThread?.join()
                lastThread = thread
            }
        }
        lastThread?.join()
        if (Settings["drawer:sorting", 0] == 1) tmpApps.sortWith { o1, o2 ->
            val iHsv = floatArrayOf(0f, 0f, 0f)
            val jHsv = floatArrayOf(0f, 0f, 0f)
            Color.colorToHSV(Palette.from(o1.icon!!.toBitmap()).generate().getVibrantColor(0xff252627.toInt()), iHsv)
            Color.colorToHSV(Palette.from(o2.icon!!.toBitmap()).generate().getVibrantColor(0xff252627.toInt()), jHsv)
            (iHsv[0] - jHsv[0]).toInt()
        }
        else tmpApps.sortWith { o1, o2 ->
            o1.label!!.compareTo(o2.label!!, ignoreCase = true)
        }

        var currentChar = tmpApps[0].label!![0].toUpperCase()
        var currentSection = ArrayList<App>().also { tmpAppSections.add(it) }
        for (app in tmpApps) {
            if (app.label!!.startsWith(currentChar, ignoreCase = true)) {
                currentSection.add(app)
            }
            else currentSection = ArrayList<App>().apply {
                add(app)
                tmpAppSections.add(this)
                currentChar = app.label!![0].toUpperCase()
            }
        }
        return null
    }

    override fun onPostExecute(v: Unit?) {
        run {
            val tmp = Main.apps
            Main.apps = tmpApps
            tmp.clear()
        }
        run {
            val tmp = Main.appSections
            Main.appSections = tmpAppSections
            tmp.clear()
        }
        App.swapMaps()
        App.clearSecondMap()
        onEnd()
    }
}