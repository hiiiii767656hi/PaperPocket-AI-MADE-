package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.server.MinecraftColorFormatter
import com.example.server.ServerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PaperHost", appName)
    }

    @Test
    fun `minecraft color code stripping works correctly`() {
        val colored = "§aPaperCraft §6Server §f[1.21.1]"
        val stripped = MinecraftColorFormatter.stripColor(colored)
        assertEquals("PaperCraft Server [1.21.1]", stripped)
    }

    @Test
    fun `server status values are defined`() {
        val statuses = ServerStatus.values()
        assertTrue(statuses.contains(ServerStatus.RUNNING))
        assertTrue(statuses.contains(ServerStatus.STOPPED))
    }
}
