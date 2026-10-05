package com.example

import com.example.server.MinecraftProtocolHelper
import com.example.server.PlayitTunnelManager
import com.example.server.PlayitTunnelStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PlayitAndCustomVersionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testProtocolVersionResolution() {
        assertEquals(769, MinecraftProtocolHelper.getProtocolForVersion("1.21.4"))
        assertEquals(767, MinecraftProtocolHelper.getProtocolForVersion("1.21.1"))
        assertEquals(767, MinecraftProtocolHelper.getProtocolForVersion("1.21"))
        assertEquals(765, MinecraftProtocolHelper.getProtocolForVersion("1.20.4"))
        assertEquals(763, MinecraftProtocolHelper.getProtocolForVersion("1.20.1"))
        assertEquals(754, MinecraftProtocolHelper.getProtocolForVersion("1.16.5"))
        assertEquals(340, MinecraftProtocolHelper.getProtocolForVersion("1.12.2"))
        assertEquals(47, MinecraftProtocolHelper.getProtocolForVersion("1.8.8"))
        // Fallback for custom version
        assertTrue(MinecraftProtocolHelper.getProtocolForVersion("26.3") > 0)
    }

    @Test
    fun testExtractVersionFromJarFileName() {
        assertEquals("1.20.4", MinecraftProtocolHelper.extractVersionFromFileName("paper-1.20.4-496.jar"))
        assertEquals("1.21.1", MinecraftProtocolHelper.extractVersionFromFileName("purpur-1.21.1-2290.jar"))
        assertEquals("1.16.5", MinecraftProtocolHelper.extractVersionFromFileName("spigot-1.16.5.jar"))
        assertEquals("1.12.2", MinecraftProtocolHelper.extractVersionFromFileName("forge-1.12.2.jar"))
    }

    @Test
    fun testPlayitPluginInstallation() {
        val serverDir = tempFolder.newFolder("minecraft_server")
        val success = PlayitTunnelManager.installPlayitPlugin(serverDir)
        assertTrue("Playit plugin should install successfully", success)

        val pluginFile = File(serverDir, "plugins/playit-spigot.jar")
        assertTrue("playit-spigot.jar should exist in plugins folder", pluginFile.exists())
        assertTrue("Jar should not be empty", pluginFile.length() > 0)
    }

    @Test
    fun testPlayitTunnelStartAndStop() {
        PlayitTunnelManager.startTunnel(25565, "my-server")
        val info = PlayitTunnelManager.tunnelInfo.value

        assertTrue(info.isEnabled)
        assertNotNull(info.publicAddress)
        assertTrue(info.publicAddress.contains("25565"))
        assertTrue(info.publicAddress.contains("playit.gg"))

        PlayitTunnelManager.stopTunnel()
        val stoppedInfo = PlayitTunnelManager.tunnelInfo.value
        assertEquals(PlayitTunnelStatus.OFFLINE, stoppedInfo.status)
    }

    @Test
    fun testHostResolutionDiagnosis() = kotlinx.coroutines.runBlocking {
        val localhostRes = PlayitTunnelManager.testHostResolution("127.0.0.1:25565")
        assertTrue("Localhost IP should be resolvable", localhostRes.isResolvable)
        assertEquals("127.0.0.1", localhostRes.host)
        assertEquals(25565, localhostRes.port)

        val invalidRes = PlayitTunnelManager.testHostResolution("nonexistent.invalid.fake.domain:25565")
        org.junit.Assert.assertFalse("Fake domain should not be resolvable", invalidRes.isResolvable)
        assertTrue(invalidRes.message.contains("Unknown Host"))
    }
}
