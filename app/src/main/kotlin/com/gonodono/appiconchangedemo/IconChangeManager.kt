package com.gonodono.appiconchangedemo

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import android.content.pm.PackageManager.GET_ACTIVITIES
import android.content.pm.PackageManager.GET_META_DATA
import android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS
import android.os.Build
import android.os.Bundle

internal class IconChangeManager(private val activity: Activity, private val launchApp: Boolean) {

    private val currentInit: ActivityAlias
    private val initialAlias: ActivityAlias
    private val cloneInitialAlias: ActivityAlias

    var selectableAliases: List<ActivityAlias>
        private set

    init {
        val aliases = activity.getActivitiesFromManifest()
            .filter { activityInfo -> activityInfo.isIconChangeAlias }
            .map { activityInfo -> ActivityAlias(activityInfo) }

        val aliasesEnabled = activity.getActivitiesFromManifest()
            .filter { activityInfo -> activityInfo.enabled }
            .map { activityInfo -> ActivityAlias(activityInfo) }

        val listAlias = activity.getActivitiesFromManifest()
            .map { activityInfo -> ActivityAlias(activityInfo) }

        println("aliases $aliases")
        println("aliasesEnabled $aliasesEnabled")
        println("listAlias $listAlias")
        val componentEnabled = aliases.filter { it.isComponentEnabled }
        val aliasEnabled = aliases.filter { it.isEnabledInManifest }

        println("Component Size ${componentEnabled.size}")
        println("aliasEnabled Size ${aliasEnabled.size}")
        currentInit = when (componentEnabled.size) {
            1 -> componentEnabled[0]
            0 -> if (launchApp) aliasEnabled[0] else error("No alias component currently enabled")
            else -> error("Multiple alias components currently enabled")
        }

        val (enabled, disabled) = if (launchApp) aliasEnabled.partition { it.isEnabledInManifest }
            else aliases.partition { it.isEnabledInManifest }
        println("enabled.size ${enabled.size}")
        initialAlias = when (enabled.size) {
            1 -> enabled[0]
            0 -> error("No initial alias enabled in manifest")
            else -> error("Multiple aliases enabled in manifest")
        }
        println("disabled.size ${disabled.size}")

        if (launchApp) {
            selectableAliases = enabled
            cloneInitialAlias = enabled[0]
        } else {
            val sameIcon = disabled.filter { it.icon == initialAlias.icon }
            println("sameIcon.size ${sameIcon.size}")
            cloneInitialAlias = when (sameIcon.size) {
                1 -> sameIcon[0]
                0 -> error("No clone initial alias found")
                else -> error("Multiple clone initial aliases found")
            }

            selectableAliases = disabled
        }
    }

    var currentAlias: ActivityAlias = currentInit
        set(alias) {
            if (field == alias) return
            field.isComponentEnabled = false
            alias.isComponentEnabled = true
            field = alias
            isChangingIcon = true
            activity.recreate()
        }

    private var isChangingIcon = false

    fun onSaveInstanceState(outState: Bundle) {
        if (isChangingIcon) outState.putBoolean(EXTRA_IS_CHANGING_ICON, true)
    }

    var isIconHide: Boolean = false

    var isIconChangeActivated: Boolean = !initialAlias.isComponentEnabled
        set(activated) {
            if (field == activated) return
            field = activated
            if (activated) {
                initialAlias.isComponentEnabled = false
                cloneInitialAlias.isComponentEnabled = true

                // Originally, the demo just finished the Activity and required
                // the user to relaunch. While updating, I found that a restart
                // works too, at least with this simple setup. If you have any
                // issues, you might need to change back to a manual relaunch.
                activity.restart()
            } else {
                initialAlias.isComponentEnabled = true
                currentAlias.isComponentEnabled = false

                // Activity#recreate() seems to suffice here as well, since the
                // aliases are being reset to their default values, but the demo
                // does a restart to keep things symmetrical. You can pull this
                // out of the if-else, obviously, if you keep the restarts.
                activity.restart()
            }
        }

    enum class StartMode { Normal, ActivationChange, IconChange }

    fun determineStartMode(
        intent: Intent,
        savedInstanceState: Bundle?
    ): StartMode = when {
        // Order matters, unless you'd prefer to reset the Intent extra below.
        savedInstanceState?.getBoolean(EXTRA_IS_CHANGING_ICON) ?: false -> {
            StartMode.IconChange
        }
        intent.getBooleanExtra(EXTRA_IS_CHANGING_ACTIVATION, false) -> {
            StartMode.ActivationChange
        }
        else -> StartMode.Normal
    }

    private var ActivityAlias.isComponentEnabled: Boolean
        get() {
            val manager = activity.packageManager
            val component = ComponentName(activity, name)
            val state = manager.getComponentEnabledSetting(component)
            return if (state == COMPONENT_ENABLED_STATE_DEFAULT) {
                isEnabledInManifest
            } else {
                state == COMPONENT_ENABLED_STATE_ENABLED
            }
        }
        set(enabled) {
            val manager = activity.packageManager
            val component = ComponentName(activity, name)
            val state = if (this == initialAlias) {
                if (enabled) COMPONENT_ENABLED_STATE_DEFAULT
                else COMPONENT_ENABLED_STATE_DISABLED
            } else {
                if (enabled) COMPONENT_ENABLED_STATE_ENABLED
                else COMPONENT_ENABLED_STATE_DEFAULT
            }
            manager.setComponentEnabledSetting(component, state, DONT_KILL_APP)
        }

    fun hideUnHideIcon(boolean: Boolean, context: Context) {
        val manager = activity.packageManager
        val component = ComponentName(activity.applicationContext, currentAlias.name)
        val components = ComponentName.createRelative(activity.applicationContext,
            currentAlias.name)
        val state : Int = if (boolean) COMPONENT_ENABLED_STATE_DISABLED else
            COMPONENT_ENABLED_STATE_ENABLED
        println("hideUnHideIcon state : $state")
        println("hideUnHideIcon components : $component")
        manager.setComponentEnabledSetting(
            component,
            state,
            DONT_KILL_APP
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Thread {
                Thread.sleep(2000)
                restartApp(context, activity.packageName)
            }.start()
        }
    }
}

private fun Context.getActivitiesFromManifest(): Array<out ActivityInfo> {
    val flags = GET_ACTIVITIES or GET_META_DATA or MATCH_DISABLED_COMPONENTS
    return packageManager.getPackageInfo(packageName, flags).activities!!
}

private val ActivityInfo.isIconChangeAlias: Boolean
    get() = metaData?.containsKey(ActivityAlias.META_DATA_TITLE) == true

private fun Activity.restart() {
    finish() // <- Must come first, else it cancels the startActivity().
    val intent = packageManager.getLaunchIntentForPackage(packageName)!!
    startActivity(intent.putExtra(EXTRA_IS_CHANGING_ACTIVATION, true))
}

private const val EXTRA_IS_CHANGING_ACTIVATION: String =
    "${BuildConfig.APPLICATION_ID}.extra.IS_CHANGING_ACTIVATION"

private const val EXTRA_IS_CHANGING_ICON: String =
    "${BuildConfig.APPLICATION_ID}.extra.IS_CHANGING_ICON"