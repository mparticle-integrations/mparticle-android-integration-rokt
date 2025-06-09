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
import com.mparticle.commerce.CommerceEvent
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import com.mparticle.kits.KitIntegration.CommerceListener
import com.mparticle.kits.KitIntegration.IdentityListener
import com.mparticle.kits.KitIntegration.RoktListener
import com.mparticle.rokt.RoktEmbeddedView
import com.rokt.roktsdk.Rokt
import com.rokt.roktsdk.RoktWidgetDimensionCallBack
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
    private var mpRoktEventCallback: MParticle.MpRoktEventCallback? = null
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

                    Rokt.init(
                        roktTagId = roktTagId,
                        appVersion = info.versionName,
                        application = application,
                        fontPostScriptNames = emptySet(),
                        fontFilePathMap = emptyMap(),
                        callback = null,
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
    @Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")
    override fun execute(
        viewName: String,
        attributes: Map<String, String>,
        mpRoktEventCallback: MParticle.MpRoktEventCallback?,
        placeHolders: MutableMap<String, WeakReference<RoktEmbeddedView>>?,
        fontTypefaces: MutableMap<String, WeakReference<Typeface>>?,
        filterUser: FilteredMParticleUser?
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
        val finalAttributes: HashMap<String, String> = HashMap<String, String>()
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

        Rokt.execute(
            viewName,
            finalAttributes,
            this,
            // Pass placeholders and fontTypefaces only if they are not empty or null
            placeholders.takeIf { it?.isNotEmpty() == true },
            fontTypefaces.takeIf { it?.isNotEmpty() == true }
        )
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

    private fun getStringForIdentity(identityType: IdentityType): String {
        return when (identityType) {
            IdentityType.Other -> "other"
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
                Rokt.UnloadReasons.NO_OFFERS -> MParticle.UnloadReasons.NO_OFFERS
                Rokt.UnloadReasons.FINISHED -> MParticle.UnloadReasons.FINISHED
                Rokt.UnloadReasons.TIMEOUT -> MParticle.UnloadReasons.TIMEOUT
                Rokt.UnloadReasons.NETWORK_ERROR -> MParticle.UnloadReasons.NETWORK_ERROR
                Rokt.UnloadReasons.NO_WIDGET -> MParticle.UnloadReasons.NO_WIDGET
                Rokt.UnloadReasons.INIT_FAILED -> MParticle.UnloadReasons.INIT_FAILED
                Rokt.UnloadReasons.UNKNOWN_PLACEHOLDER -> MParticle.UnloadReasons.UNKNOWN_PLACEHOLDER
                Rokt.UnloadReasons.UNKNOWN -> MParticle.UnloadReasons.UNKNOWN
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