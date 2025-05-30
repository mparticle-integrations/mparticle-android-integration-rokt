package com.mparticle.kits

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build.VERSION
import com.mparticle.AttributionError
import com.mparticle.AttributionResult
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticleOptions
import com.mparticle.MParticleOptions.DataplanOptions
import com.mparticle.identity.IdentityApi
import com.mparticle.internal.CoreCallbacks
import com.mparticle.internal.CoreCallbacks.KitListener
import com.mparticle.kits.mocks.MockKitConfiguration
import io.mockk.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*


class RoktKitTests {
    private val context = mockk<Context>(relaxed = true)
    private lateinit var roktKit: RoktKit
    private val settings = HashMap<String, String>()
    private lateinit var kitManager: TestKitManager


    @Before
    @Throws(Exception::class)
    fun setUp() {
        settings["application_id"] = TEST_APPLICATION_ID
        every { context.applicationContext } returns context
        roktKit = RoktKit()
        kitManager = TestKitManager()
        MParticle.setInstance(Mockito.mock(MParticle::class.java))
        Mockito.`when`(MParticle.getInstance()?.Identity()).thenReturn(
            Mockito.mock(
                IdentityApi::class.java
            )
        )
        val kitManager = KitManagerImpl(
            Mockito.mock(
                Context::class.java
            ), null, emptyCoreCallbacks, mock(MParticleOptions::class.java)
        )
        roktKit.kitManager = kitManager/*
        roktKit.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("id", "-1"))*/

    }



    private inner class TestKitManager internal constructor() :
        KitManagerImpl(context, null, TestCoreCallbacks(), mock(MParticleOptions::class.java)) {
        var attributes = HashMap<String, String>()
        var result: AttributionResult? = null
        private var error: AttributionError? = null
        public override fun getIntegrationAttributes(kitIntegration: KitIntegration): Map<String, String> {
            return attributes
        }

        public override fun setIntegrationAttributes(
            kitIntegration: KitIntegration,
            integrationAttributes: Map<String, String>
        ) {
            attributes = integrationAttributes as HashMap<String, String>
        }

        override fun onResult(result: AttributionResult) {
            this.result = result
        }

        override fun onError(error: AttributionError) {
            this.error = error
        }
    }

    private inner class TestKitConfiguration : KitConfiguration() {
        override fun getKitId(): Int = TEST_KIT_ID
    }

    private inner class TestMParticle : MParticle() {
        override fun Identity(): IdentityApi = mock(IdentityApi::class.java)

    }

    @Test
    fun test_addIdentityAttributes_When_userIdentities_IS_Null(){
        val mockFilterUser = Mockito.mock(FilteredMParticleUser::class.java)
        val userIdentities = HashMap<IdentityType, String>()
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(userIdentities)
        val attributes: Map<String, String> = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3"
        )
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java
        )
        method.isAccessible = true
        val result = method.invoke(roktKit, attributes, mockFilterUser) as Map<String, String>
        assertEquals(3, result.size)

        assertTrue(result.containsKey("key1"))
        assertTrue(result.containsKey("key2"))
        assertTrue(result.containsKey("key3"))

    }

    @Test
    fun test_addIdentityAttributes_When_userIdentities_Contain_value(){
        val mockFilterUser = Mockito.mock(FilteredMParticleUser::class.java)
        val userIdentities = HashMap<IdentityType, String>()
        userIdentities.put(IdentityType.Email,"TestEmail@gamil.com")
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(userIdentities)
        val attributes: Map<String, String> = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3"
        )
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java
        )
        method.isAccessible = true
        val result = method.invoke(roktKit, attributes, mockFilterUser) as Map<String, String>
        assertEquals(4, result.size)

        assertTrue(result.containsKey("key1"))
        assertTrue(result.containsKey("key2"))
        assertTrue(result.containsKey("key3"))
        assertTrue(result.containsKey("email"))

    }

    @Test
    fun test_addIdentityAttributes_When_userIdentities_And_attributes_contains_same_key(){
        val mockFilterUser = Mockito.mock(FilteredMParticleUser::class.java)
        val userIdentities = HashMap<IdentityType, String>()
        userIdentities.put(IdentityType.Email,"TestEmail@gamil.com")
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(userIdentities)
        val attributes: Map<String, String> = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3",
            "email" to "abc@gmail.com"
        )
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java
        )
        method.isAccessible = true
        val result = method.invoke(roktKit, attributes, mockFilterUser) as Map<String, String>
        assertEquals(4, result.size)

        assertTrue(result.containsKey("key1"))
        assertTrue(result.containsKey("key2"))
        assertTrue(result.containsKey("key3"))
        assertTrue(result.containsKey("email"))
        assertEquals(
            mapOf(
                "key1" to "value1",
                "key2" to "value2",
                "key3" to "value3",
                "email" to "TestEmail@gamil.com"
            ),
            result
        )
    }

    @Test
    fun testAddIdentityAttributes_bothNull() {
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java
        )
        method.isAccessible = true
        val result =  method.invoke(roktKit, null, null) as Map<String, String>
        assertTrue(result.isEmpty())
    }

    @Test
    fun testAddIdentityAttributes_nullAttributes_validUser() {
        val mockFilterUser = Mockito.mock(FilteredMParticleUser::class.java)
        val userIdentities = HashMap<IdentityType, String>()
        userIdentities.put(IdentityType.Email,"TestEmail@gamil.com")
        Mockito.`when`(mockFilterUser.userIdentities).thenReturn(userIdentities)
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java
        )
        method.isAccessible = true
        val result = method.invoke(roktKit, null, mockFilterUser) as Map<String, String>
        assertEquals(1, result.size)
        assertEquals(mapOf("email" to "TestEmail@gamil.com"), result)
    }

    @Test
    fun testAddIdentityAttributes_nullUser_returnsSameAttributes() {
        val attributes: Map<String, String> = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3"
        )
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "addIdentityAttributes",
            Map::class.java,
            FilteredMParticleUser::class.java
        )
        method.isAccessible = true
        val result = method.invoke(roktKit, attributes, null) as Map<String, String>
        assertEquals(3, result.size)
        assertTrue(result.containsKey("key1"))
        assertTrue(result.containsKey("key2"))
        assertTrue(result.containsKey("key3"))
    }



    internal inner class TestCoreCallbacks : CoreCallbacks {
        override fun isBackgrounded(): Boolean = false
        override fun getUserBucket(): Int = 0
        override fun isEnabled(): Boolean = false
        override fun setIntegrationAttributes(i: Int, map: Map<String, String>) {}
        override fun getIntegrationAttributes(i: Int): Map<String, String>? = null
        override fun getCurrentActivity(): WeakReference<Activity>? = null
        override fun getLatestKitConfiguration(): JSONArray? = null
        override fun getDataplanOptions(): DataplanOptions? = null
        override fun isPushEnabled(): Boolean = false
        override fun getPushSenderId(): String? = null
        override fun getPushInstanceId(): String? = null
        override fun getLaunchUri(): Uri? = null
        override fun getLaunchAction(): String? = null
        override fun getKitListener(): KitListener {
            return object : KitListener {
                override fun kitFound(kitId: Int) {}
                override fun kitConfigReceived(kitId: Int, configuration: String?) {}
                override fun kitExcluded(kitId: Int, reason: String?) {}
                override fun kitStarted(kitId: Int) {}
                override fun onKitApiCalled(kitId: Int, used: Boolean?, vararg objects: Any?) {
                }

                override fun onKitApiCalled(methodName: String?, kitId: Int, used: Boolean?, vararg objects: Any?) {
                }
            }
        }
    }

    companion object {
        private const val TEST_APPLICATION_ID = "app-abcdef1234567890"
        private const val TEST_ATTRIBUTION_TOKEN = "srctok-abcdef1234567890"
        private const val TEST_DEEP_LINK = "https://www.example.com/product/abc123"
        private const val TEST_KIT_ID = 0x01

        /*
     * Test Helpers
     */
        @Throws(Exception::class)
        private fun setTestSdkVersion(sdkVersion: Int) {
            setFinalStatic(VERSION::class.java.getField("SDK_INT"), sdkVersion)
        }

        @Throws(Exception::class)
        private fun setFinalStatic(field: Field, newValue: Int) {
            field.isAccessible = true
            val getDeclaredFields0 =
                Class::class.java.getDeclaredMethod(
                    "getDeclaredFields0",
                    Boolean::class.javaPrimitiveType
                )
            getDeclaredFields0.isAccessible = true
            val fields = getDeclaredFields0.invoke(Field::class.java, false) as Array<Field>
            var modifiersField: Field? = null
            for (each in fields) {
                if ("modifiers" == each.name) {
                    modifiersField = each
                    break
                }
            }
            modifiersField!!.isAccessible = true
            modifiersField!!.setInt(field, field.modifiers and Modifier.FINAL.inv())
            field[null] = newValue

        }
    }

    private var emptyCoreCallbacks: CoreCallbacks = object : CoreCallbacks {
        var activity = Activity()
        override fun isBackgrounded(): Boolean = false

        override fun getUserBucket(): Int = 0

        override fun isEnabled(): Boolean = false

        override fun setIntegrationAttributes(i: Int, map: Map<String, String>) {}

        override fun getIntegrationAttributes(i: Int): Map<String, String>? = null

        override fun getCurrentActivity(): WeakReference<Activity> = WeakReference(activity)

        override fun getLatestKitConfiguration(): JSONArray? = null

        override fun getDataplanOptions(): DataplanOptions? = null

        override fun isPushEnabled(): Boolean = false

        override fun getPushSenderId(): String? = null

        override fun getPushInstanceId(): String? = null

        override fun getLaunchUri(): Uri? = null

        override fun getLaunchAction(): String? = null

        override fun getKitListener(): KitListener = KitListener.EMPTY

    }
}