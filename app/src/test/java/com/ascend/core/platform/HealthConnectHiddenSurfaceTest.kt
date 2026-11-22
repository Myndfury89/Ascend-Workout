package com.ascend.core.platform

import com.ascend.BuildConfig
import com.ascend.core.common.HealthConnectFeatures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The onboarding suggestion flag must not grow a Health Connect permission or client surface.
 * These checks read source manifests and the debug unit-test BuildConfig. They are independent
 * of whether a local override turns the suggestion back on.
 */
class HealthConnectHiddenSurfaceTest {
    @Test
    fun `release and debug manifests declare only INTERNET and no health permissions`() {
        val main = File("src/main/AndroidManifest.xml")
        val debug = File("src/debug/AndroidManifest.xml")
        assertTrue("main manifest missing from ${File(".").absolutePath}", main.exists())
        assertTrue("debug manifest missing from ${File(".").absolutePath}", debug.exists())

        assertEquals(listOf("android.permission.INTERNET"), permissionNames(main))
        assertTrue(permissionNames(debug).all { it == "android.permission.INTERNET" })

        listOf(main, debug).forEach { manifest ->
            val text = manifest.readText()
            FORBIDDEN_MANIFEST_NEEDLES.forEach { needle ->
                assertFalse("${manifest.path} must not mention $needle", text.contains(needle))
            }
        }
    }

    @Test
    fun `debug unit tests see the Health Connect onboarding suggestion disabled`() {
        assertFalse(BuildConfig.HEALTH_CONNECT_ONBOARDING_SUGGESTION_ENABLED)
        assertFalse(HealthConnectFeatures.isOnboardingSuggestionEnabled)
    }

    private fun permissionNames(manifest: File): List<String> =
        USES_PERMISSION.findAll(manifest.readText()).map { it.groupValues[1] }.toList()

    private companion object {
        val USES_PERMISSION = Regex("""<uses-permission\b[^>]*\bandroid:name="([^"]+)"""")

        val FORBIDDEN_MANIFEST_NEEDLES =
            listOf(
                "android.permission.health.",
                "androidx.health.connect",
                "HealthConnectClient",
                "com.google.android.apps.healthdata",
            )
    }
}
