package com.mparticle.kits

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import com.mparticle.commerce.CommerceEvent
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.kits.KitIntegration.CommerceListener
import com.mparticle.kits.KitIntegration.IdentityListener
import com.mparticle.kits.KitIntegration.RoktListener
import com.rokt.roktsdk.Rokt
import com.rokt.roktsdk.Widget
import java.lang.ref.WeakReference
import java.math.BigDecimal


/**
 * MParticle embedded implementation of the Rokt Library.
 *
 * Learn more at our [Developer Docs](https://docs.rokt.com/developers/integration-guides/android)
 */
class RoktKit : KitIntegration(), CommerceListener, IdentityListener, RoktListener, Rokt.RoktCallback {
    private var applicationContext: Context? = null
    private var onUnloadCallback: Runnable? = null
    private var onLoadCallback: Runnable? = null
    private var onShouldHideLoadingIndicatorCallback: Runnable? = null
    private var onShouldShowLoadingIndicatorCallback: Runnable? = null
    override fun getName(): String = NAME

    override fun getInstance(): RoktKit = this

    public override fun onKitCreate(
        settings: Map<String, String>,
        ctx: Context
    ): List<ReportingMessage> {
        applicationContext = ctx.applicationContext
        val roktTagId = settings[ROKT_ACCOUNT_ID]
        if (KitUtils.isEmpty(roktTagId)) {
            throwOnKitCreateError(NO_ROKT_ACCOUNT_ID)
        }
        applicationContext?.let {
            val manager = context.packageManager
            if (roktTagId != null) {
                try {
                    val info = manager.getPackageInfoForApp(context.packageName, 0)
                    val application = context.applicationContext as Application
                    Rokt.init(roktTagId, info.versionName, application)
                } catch (e: PackageManager.NameNotFoundException) {
                    throwOnKitCreateError(NO_APP_VERSION_FOUND)
                } catch (e: Exception) {
                    logError("Error initializing Rokt", e)
                }
            }
        }
        return emptyList()
    }

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> = emptyList()

    public override fun reset() {
        super.reset()
    }

    /*
     * Overrides for CommerceListener
     */
    override fun logLtvIncrease(
        bigDecimal: BigDecimal, bigDecimal1: BigDecimal,
        s: String, map: Map<String, String>
    ): List<ReportingMessage> = emptyList()

    override fun logEvent(commerceEvent: CommerceEvent): List<ReportingMessage> {
        return emptyList()
    }

    /*
     * Overrides for IdentityListener
     */
    override fun onIdentifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest
    ) {
    }

    override fun onLoginCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest
    ) {
    }

    override fun onLogoutCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest
    ) {
    }

    override fun onModifyCompleted(
        mParticleUser: MParticleUser,
        filteredIdentityApiRequest: FilteredIdentityApiRequest
    ) {
    }

    override fun onUserIdentified(mParticleUser: MParticleUser) {}

    private fun logError(message: String, t: Throwable) {
        Logger.error(t, "RoktKit: $message")
    }

    private fun throwOnKitCreateError(message: String) {
        throw IllegalArgumentException(message)
    }

    /*
      For more details, visit the official documentation:
     https://docs.rokt.com/developers/integration-guides/android/how-to/adding-a-placement/
    */
    override fun execute(
        viewName: String,
        attributes: Map<String, String>?,
        onUnload: Runnable?,
        onLoad: Runnable?,
        onShouldHideLoadingIndicator: Runnable?,
        onShouldShowLoadingIndicator: Runnable?,
        placeHolders: MutableMap<String, WeakReference<com.mparticle.rokt.RoktEmbeddedView>>?,
        fontTypefaces: MutableMap<String, WeakReference<Typeface>>?,
        filterUser: FilteredMParticleUser?
    ) {
        // Converting the placeholders to a Map<String, WeakReference<Widget>> by filtering and casting each entry
        val placeholders: Map<String, WeakReference<Widget>>? = placeHolders?.mapNotNull { entry ->
            (entry.value as? WeakReference<Widget>)?.let {
                entry.key to it
            }
        }?.toMap()
        onUnloadCallback = onUnload
        onLoadCallback = onLoad
        onShouldHideLoadingIndicatorCallback = onShouldHideLoadingIndicator
        onShouldShowLoadingIndicatorCallback = onShouldShowLoadingIndicator
        val finalAttributes: HashMap<String, String> = HashMap<String, String>()
        filterUser?.userAttributes?.forEach { (key, value) ->
            finalAttributes[key] = value.toString()
        }
        filterAttributes(finalAttributes, configuration).let {
            finalAttributes.putAll(it)
        }
        filterUser?.id?.let { mpid ->
            finalAttributes.put(MPID, mpid.toString())
        } ?: run {
            Logger.warning("RoktKit: No user ID available for placement")
        }

        Rokt.execute(
            viewName,
            finalAttributes,
            this,
            // Pass placeholders and fontTypefaces only if they are not empty or null
            placeholders.takeIf { it?.isNotEmpty() == true },
            fontTypefaces.takeIf { it?.isNotEmpty() == true }
        )
    }

    private fun filterAttributes(attributes: Map<String, String>, kitConfiguration: KitConfiguration): Map<String, String> {
        val userAttributes: MutableMap<String, String> = HashMap<String, String>()
        for ((key, value) in attributes) {
            val hashKey = KitUtils.hashForFiltering(key)
            if (!kitConfiguration.mAttributeFilters.get(hashKey)) {
                userAttributes[key] = value
            }
        }
        return userAttributes
    }

    companion object {
        const val NAME = "Rokt"
        const val ROKT_ACCOUNT_ID = "accountId"
        const val MPID = "mpid"
        const val NO_ROKT_ACCOUNT_ID = "No Rokt account ID provided, can't initialize kit."
        const val NO_APP_VERSION_FOUND = "No App version found, can't initialize kit."
    }

    override fun onLoad() {
        onLoadCallback?.run()
    }

    override fun onShouldHideLoadingIndicator() {
        onShouldHideLoadingIndicatorCallback?.run()
    }

    override fun onShouldShowLoadingIndicator() {
        onShouldShowLoadingIndicatorCallback?.run()
    }

    override fun onUnload(reason: Rokt.UnloadReasons) {
        onUnloadCallback?.run()
    }
}


fun PackageManager.getPackageInfoForApp(packageName: String, flags: Int = 0): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
    }