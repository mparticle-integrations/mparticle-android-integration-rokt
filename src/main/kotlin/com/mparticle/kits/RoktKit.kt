package com.mparticle.kits

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import com.mparticle.BuildConfig
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.MpRoktEventCallback
import com.mparticle.UnloadReasons
import com.mparticle.WrapperSdk
import com.mparticle.WrapperSdkVersion
import com.mparticle.commerce.CommerceEvent
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Logger
import com.mparticle.kits.KitIntegration.CommerceListener
import com.mparticle.kits.KitIntegration.IdentityListener
import com.mparticle.kits.KitIntegration.RoktListener
import com.mparticle.rokt.RoktConfig
import com.mparticle.rokt.RoktEmbeddedView
import com.rokt.roktsdk.CacheConfig
import com.rokt.roktsdk.Rokt
import com.rokt.roktsdk.Rokt.SdkFrameworkType.Android
import com.rokt.roktsdk.Rokt.SdkFrameworkType.Cordova
import com.rokt.roktsdk.Rokt.SdkFrameworkType.Flutter
import com.rokt.roktsdk.Rokt.SdkFrameworkType.ReactNative
import com.rokt.roktsdk.RoktEvent
import com.rokt.roktsdk.RoktWidgetDimensionCallBack
import com.rokt.roktsdk.Widget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.lang.ref.WeakReference
import java.math.BigDecimal

/**
 * MParticle embedded implementation of the Rokt Library.
 *
 * Learn more at our [Developer Docs](https://docs.rokt.com/developers/integration-guides/android)
 */
@Suppress("unused")
class RoktKit : KitIntegration(), CommerceListener, IdentityListener, RoktListener,
    Rokt.RoktCallback {
    private var applicationContext: Context? = null
    private var mpRoktEventCallback: MpRoktEventCallback? = null
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
                    val mparticleVersion = BuildConfig.VERSION_NAME
                    
                    // Get RoktOptions from the kit manager
                    val roktOptions = kitManager?.roktOptions
                    val fontFilePathMap = roktOptions?.fontFilePathMap ?: emptyMap()
                    val fontPostScriptNames = roktOptions?.fontPostScriptNames ?: emptySet()

                    Rokt.init(
                        roktTagId = roktTagId,
                        appVersion = info.versionName,
                        application = application,
                        fontPostScriptNames = fontPostScriptNames,
                        fontFilePathMap = fontFilePathMap,
                        callback = object : Rokt.RoktInitCallback {
                            override fun onInitComplete(success: Boolean) {
                                Logger.verbose("Rokt Kit Initialization success: $success")
                            }
                        },
                        mParticleSdkVersion = mparticleVersion,
                        mParticleKitVersion = mparticleVersion
                    )

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
        attributes: Map<String, String>,
        mpRoktEventCallback: MpRoktEventCallback?,
        placeHolders: MutableMap<String, WeakReference<RoktEmbeddedView>>?,
        fontTypefaces: MutableMap<String, WeakReference<Typeface>>?,
        filterUser: FilteredMParticleUser?,
        mpRoktConfig: RoktConfig?
    ) {
        val placeholders: Map<String, WeakReference<Widget>>? = placeHolders?.mapNotNull { entry ->
            val widget = Widget(entry.value.get()?.context as Context)
            entry.value.get()?.removeAllViews()
            entry.value.get()?.addView(widget)
            entry.value.get()?.dimensionCallBack?.let {
                widget.registerDimensionListener(object: RoktWidgetDimensionCallBack {
                    override fun onHeightChanged(height: Int) {
                        it.onHeightChanged(height)
                    }

                    override fun onMarginChanged(
                        start: Int,
                        top: Int,
                        end: Int,
                        bottom: Int
                    ) {
                        it.onMarginChanged(start, top, end, bottom)
                    }

                })
            }
            entry.key to WeakReference(widget)
        }?.toMap()

        this.mpRoktEventCallback = mpRoktEventCallback
        val finalAttributes = mutableMapOf<String, String>()
        filterUser?.userAttributes?.let { userAttrs ->
            for ((key, value) in userAttrs) {
                finalAttributes[key] = value.toString()
            }
        }

        filterUser?.id?.toString()?.let { mpid ->
            finalAttributes[MPID] = mpid
        } ?: Logger.warning("RoktKit: No user ID available for placement")

        addIdentityAttributes(finalAttributes, filterUser)


        val SANDBOX_MODE_ROKT: String = "sandbox"
        attributes?.get(SANDBOX_MODE_ROKT)?.let { value ->
            finalAttributes.put(SANDBOX_MODE_ROKT, value)
        }
        verifyHashedEmail(finalAttributes)
        val roktConfig = mpRoktConfig?.let { mapToRoktConfig(it) }
        Rokt.execute(
            viewName,
            finalAttributes,
            this,
            // Pass placeholders and fontTypefaces only if they are not empty or null
            placeholders.takeIf { it?.isNotEmpty() == true },
            fontTypefaces.takeIf { it?.isNotEmpty() == true },
            roktConfig
        )
    }

    override fun events(identifier: String): Flow<com.mparticle.RoktEvent> {
        return Rokt.events(identifier)
            .map { event ->
                when (event) {
                    is RoktEvent.HideLoadingIndicator -> com.mparticle.RoktEvent.HideLoadingIndicator
                    is RoktEvent.ShowLoadingIndicator -> com.mparticle.RoktEvent.ShowLoadingIndicator
                    is RoktEvent.FirstPositiveEngagement -> com.mparticle.RoktEvent.FirstPositiveEngagement(
                        event.id
                    )
                    is RoktEvent.PositiveEngagement -> com.mparticle.RoktEvent.PositiveEngagement(
                        event.id
                    )
                    is RoktEvent.OfferEngagement -> com.mparticle.RoktEvent.OfferEngagement(event.id)
                    is RoktEvent.OpenUrl -> com.mparticle.RoktEvent.OpenUrl(event.id, event.url)
                    is RoktEvent.PlacementClosed -> com.mparticle.RoktEvent.PlacementClosed(event.id)
                    is RoktEvent.PlacementCompleted -> com.mparticle.RoktEvent.PlacementCompleted(
                        event.id
                    )
                    is RoktEvent.PlacementFailure -> com.mparticle.RoktEvent.PlacementFailure(event.id)
                    is RoktEvent.PlacementInteractive -> com.mparticle.RoktEvent.PlacementInteractive(
                        event.id
                    )
                    is RoktEvent.PlacementReady -> com.mparticle.RoktEvent.PlacementReady(event.id)
                    is RoktEvent.CartItemInstantPurchase -> com.mparticle.RoktEvent.CartItemInstantPurchase(
                        placementId = event.placementId,
                        cartItemId = event.cartItemId,
                        catalogItemId = event.catalogItemId,
                        currency = event.currency,
                        description = event.description,
                        linkedProductId = event.linkedProductId,
                        totalPrice = event.totalPrice,
                        quantity = event.quantity,
                        unitPrice = event.unitPrice
                    )
                    is RoktEvent.InitComplete -> com.mparticle.RoktEvent.InitComplete(event.success)
                }
            }
    }

    override fun setWrapperSdkVersion(wrapperSdkVersion: WrapperSdkVersion) {
        val sdkFrameworkType = when (wrapperSdkVersion.sdk) {
            WrapperSdk.WrapperFlutter -> Flutter
            WrapperSdk.WrapperSdkReactNative -> ReactNative
            WrapperSdk.WrapperSdkCordova -> Cordova
            else -> Android
        }
        Rokt.setFrameworkType(sdkFrameworkType)
    }

    override fun purchaseFinalized(placementId: String, catalogItemId: String, status: Boolean) {
        Rokt.purchaseFinalized(placementId, catalogItemId, status)
    }

    override fun close() {
        Rokt.close()
    }

    private fun mapToRoktConfig(config: RoktConfig): com.rokt.roktsdk.RoktConfig {
        val colorMode = when (config.colorMode) {
            RoktConfig.ColorMode.LIGHT -> com.rokt.roktsdk.RoktConfig.ColorMode.LIGHT
            RoktConfig.ColorMode.DARK -> com.rokt.roktsdk.RoktConfig.ColorMode.DARK
            RoktConfig.ColorMode.SYSTEM -> com.rokt.roktsdk.RoktConfig.ColorMode.SYSTEM
            else -> com.rokt.roktsdk.RoktConfig.ColorMode.SYSTEM
        }

        val cacheConfig = config.cacheConfig?.cacheDurationInSeconds?.let {
            CacheConfig(
                cacheDurationInSeconds = it,
                cacheAttributes = config.cacheConfig?.cacheAttributes
            )
        }

        val edgeToEdgeDisplay = config.edgeToEdgeDisplay

        val builder = com.rokt.roktsdk.RoktConfig.Builder()
            .colorMode(colorMode)
            .edgeToEdgeDisplay(edgeToEdgeDisplay)

        cacheConfig?.let {
            builder.cacheConfig(it)
        }
        return builder.build()
    }

    private fun addIdentityAttributes(attributes: MutableMap<String, String>?, filterUser: FilteredMParticleUser?): MutableMap<String, String> {
        val identityAttributes = mutableMapOf<String, String>()
        if (filterUser != null) {
            for ((identityNumberKey, identityValue) in filterUser.userIdentities) {
                val identityType = getStringForIdentity(identityNumberKey)
                identityAttributes[identityType] = identityValue
            }
        }
        if (attributes != null) {
            attributes.putAll(identityAttributes)
            return attributes
        } else {
            return identityAttributes
        }
    }

    private fun verifyHashedEmail(attributes: MutableMap<String, String>?) {
        if (attributes == null) return

        val emailKey = MParticle.IdentityType.Email.name.lowercase()
        val otherKey = MParticle.IdentityType.Other.name.lowercase()
        val emailShaKey = "emailsha256"

        val emailShaValue = attributes.entries.find { it.key.equals(emailShaKey, ignoreCase = true) }?.value
        val otherValue = attributes.entries.find { it.key.equals(otherKey, ignoreCase = true) }?.value

        when {
            !emailShaValue.isNullOrEmpty() -> {
                // If emailsha256 is already present, remove entries with email
                val iterator = attributes.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.key.equals(emailKey, ignoreCase = true)) {
                        iterator.remove()
                    }
                }
            }
        }
    }

    private fun getStringForIdentity(identityType: IdentityType): String {
        return when (identityType) {
            IdentityType.Other -> "emailsha256"
            IdentityType.CustomerId -> "customerid"
            IdentityType.Facebook -> "facebook"
            IdentityType.Twitter -> "twitter"
            IdentityType.Google -> "google"
            IdentityType.Microsoft -> "microsoft"
            IdentityType.Yahoo -> "yahoo"
            IdentityType.Email -> "email"
            IdentityType.Alias -> "alias"
            IdentityType.FacebookCustomAudienceId -> "facebookcustomaudienceid"
            IdentityType.Other2 -> "other2"
            IdentityType.Other3 -> "other3"
            IdentityType.Other4 -> "other4"
            IdentityType.Other5 -> "other5"
            IdentityType.Other6 -> "other6"
            IdentityType.Other7 -> "other7"
            IdentityType.Other8 -> "other8"
            IdentityType.Other9 -> "other9"
            IdentityType.Other10 -> "other10"
            IdentityType.MobileNumber -> "mobilenumber"
            IdentityType.PhoneNumber2 -> "phonenumber2"
            IdentityType.PhoneNumber3 -> "phonenumber3"
            else -> ""
        }
    }

    companion object {
        const val NAME = "Rokt"
        const val ROKT_ACCOUNT_ID = "accountId"
        const val MPID = "mpid"
        const val NO_ROKT_ACCOUNT_ID = "No Rokt account ID provided, can't initialize kit."
        const val NO_APP_VERSION_FOUND = "No App version found, can't initialize kit."
    }


    override fun onLoad() : Unit{
        mpRoktEventCallback?.onLoad()
    }

    override fun onShouldHideLoadingIndicator() : Unit {
        mpRoktEventCallback?.onShouldHideLoadingIndicator()
    }

    override fun onShouldShowLoadingIndicator() : Unit {
        mpRoktEventCallback?.onShouldShowLoadingIndicator()
    }

    override fun onUnload(reason: Rokt.UnloadReasons) : Unit {
        mpRoktEventCallback?.onUnload(
            when (reason) {
                Rokt.UnloadReasons.NO_OFFERS -> UnloadReasons.NO_OFFERS
                Rokt.UnloadReasons.FINISHED -> UnloadReasons.FINISHED
                Rokt.UnloadReasons.TIMEOUT -> UnloadReasons.TIMEOUT
                Rokt.UnloadReasons.NETWORK_ERROR -> UnloadReasons.NETWORK_ERROR
                Rokt.UnloadReasons.NO_WIDGET -> UnloadReasons.NO_WIDGET
                Rokt.UnloadReasons.INIT_FAILED -> UnloadReasons.INIT_FAILED
                Rokt.UnloadReasons.UNKNOWN_PLACEHOLDER -> UnloadReasons.UNKNOWN_PLACEHOLDER
                Rokt.UnloadReasons.UNKNOWN -> UnloadReasons.UNKNOWN
            }
        )
    }
}

fun PackageManager.getPackageInfoForApp(packageName: String, flags: Int = 0): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
    }