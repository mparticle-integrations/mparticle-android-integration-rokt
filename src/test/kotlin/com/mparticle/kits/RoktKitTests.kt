package com.mparticle.kits

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build.VERSION
import com.mparticle.AttributionError
import com.mparticle.AttributionResult
import com.mparticle.MParticle
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
    fun testFilterAttributes() {

        // Create test attributes
        val attributes: Map<String, String> = mapOf(
            "ShouldFilter" to "shoudl_filter_value",
            "ShouldFilter_key_2" to "ShouldFilter_value",
            "allowed_key" to "allowed_value"
        )

        // Get the private filterAttributes method using reflection
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "filterAttributes",
            Map::class.java,
            KitConfiguration::class.java
        )
        method.isAccessible = true

        // Set up the configuration with our test filters
        val jsonObject = JSONObject()
        try {
            val filteredKey:String =KitUtils.hashForFiltering("ShouldFilter").toString()
            val allowedKey:String = KitUtils.hashForFiltering("ShouldFilter_key_2").toString()
            jsonObject.put(filteredKey, 0)
            jsonObject.put(allowedKey, 1)
        } catch (e: Exception) {
            println("Exception occurred: ${e.message}")
        }

        val json = JSONObject()
        json.put("ea", jsonObject)


        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs",json))

        // Invoke the method and get the result
        val result = method.invoke(roktKit, attributes, roktKit.configuration) as Map<String, String>

        // Verify the results
        assertEquals(1, result.size)

        assertFalse(result.containsKey("ShouldFilter"))
        assertFalse(result.containsKey("ShouldFilter_key_2"))
        assertTrue(result.containsKey("allowed_key"))
        assertEquals("allowed_value", result["allowed_key"])
    }

    @Test
    fun testFilterAttributes_When_kitConfig_Attributes_IS_NULL() {

        // Create test attributes
        val attributes: Map<String, String> = mapOf(
            "filtered_key" to "filtered_value",
            "allowed_key" to "allowed_value",
            "another_allowed_key" to "another_allowed_value"
        )

        // Get the private filterAttributes method using reflection
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "filterAttributes",
            Map::class.java,
            KitConfiguration::class.java
        )
        method.isAccessible = true

        // Set up the configuration with our test filters
        val jsonObject = JSONObject()
        try {
            val filteredKey:String =KitUtils.hashForFiltering("filtered_key").toString()
            val allowedKey:String = KitUtils.hashForFiltering("allowed_key").toString()
            jsonObject.put(filteredKey, 0)
            jsonObject.put(allowedKey, 1)
        } catch (e: Exception) {
            println("Exception occurred: ${e.message}")
        }

        val json = JSONObject()
        //here is invalid json key for filtering
        json.put("aaa", jsonObject)


        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs",json))

        // Invoke the method and get the result
        val result = method.invoke(roktKit, attributes, roktKit.configuration) as Map<String, String>

        assertEquals(3, result.size)

        assertTrue(result.containsKey("allowed_key"))
        assertTrue(result.containsKey("filtered_key"))
        assertTrue(result.containsKey("another_allowed_key"))
        assertEquals("another_allowed_value", result["another_allowed_key"])
    }

    @Test
    fun testFilterAttributes_When_Attributes_IS_Empty() {

        // Create test attributes
        val emptyAttributes: Map<String, String> = emptyMap()


        // Get the private filterAttributes method using reflection
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "filterAttributes",
            Map::class.java,
            KitConfiguration::class.java
        )
        method.isAccessible = true

        // Set up the configuration with our test filters
        val jsonObject = JSONObject()
        try {
            val filteredKey:String =KitUtils.hashForFiltering("filtered_key").toString()
            val allowedKey:String = KitUtils.hashForFiltering("allowed_key").toString()
            jsonObject.put(filteredKey, 0)
            jsonObject.put(allowedKey, 1)
        } catch (e: Exception) {
            println("Exception occurred: ${e.message}")
        }

        val json = JSONObject()
        json.put("aaa", jsonObject)


        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs",json))

        // Invoke the method and get the result
        val result = method.invoke(roktKit, emptyAttributes, roktKit.configuration) as Map<String, String>

        assertEquals(0, result.size)
    }

    @Test
    fun testFilterAttributes_When_attribute_different_value() {

        // Create test attributes
        val attributes: Map<String, String> = mapOf(
            "filtered_key" to "filtered_value",
            "allowed_key" to "allowed_value",
            "another_allowed_key" to "another_allowed_value"
        )

        // Get the private filterAttributes method using reflection
        val method: Method = RoktKit::class.java.getDeclaredMethod(
            "filterAttributes",
            Map::class.java,
            KitConfiguration::class.java
        )
        method.isAccessible = true

        // Set up the configuration with our test filters
        val jsonObject = JSONObject()
        try {
            val filteredKey:String =KitUtils.hashForFiltering("Test1").toString()
            val allowedKey:String = KitUtils.hashForFiltering("Test2").toString()
            jsonObject.put(filteredKey, 0)
            jsonObject.put(allowedKey, 1)
        } catch (e: Exception) {
            println("Exception occurred: ${e.message}")
        }

        val json = JSONObject()
        json.put("ea", jsonObject)


        roktKit.configuration = MockKitConfiguration.createKitConfiguration(JSONObject().put("hs",json))

        // Invoke the method and get the result
        val result = method.invoke(roktKit, attributes, roktKit.configuration) as Map<String, String>

        // Verify the results
        assertEquals(3, result.size)

        assertTrue(result.containsKey("filtered_key"))
        assertTrue(result.containsKey("allowed_key"))
        assertTrue(result.containsKey("another_allowed_key"))
        assertEquals("another_allowed_value", result["another_allowed_key"])
        assertEquals("filtered_value", result["filtered_key"])
        assertEquals("allowed_value", result["allowed_key"])
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