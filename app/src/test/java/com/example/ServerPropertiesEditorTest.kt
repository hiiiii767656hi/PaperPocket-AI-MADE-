package com.example

import com.example.utils.ServerPropertiesDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerPropertiesEditorTest {

    @Test
    fun testParseStandardServerProperties() {
        val sample = """
            # Minecraft server.properties
            # Wed Sep 30 2026
            server-port=25565
            motd=§aPaperMC SMP Server
            difficulty=hard
            gamemode=survival
            max-players=40
            pvp=true
            online-mode=false
            white-list=true
            hardcore=false
            allow-flight=true
            view-distance=12
            simulation-distance=8
            spawn-protection=0
            level-name=survival_world
            level-seed=123456789
            custom.plugin.flag=true
        """.trimIndent()

        val doc = ServerPropertiesDocument.fromText(sample)

        assertEquals(25565, doc.serverPort)
        assertEquals("§aPaperMC SMP Server", doc.motd)
        assertEquals("hard", doc.difficulty)
        assertEquals("survival", doc.gamemode)
        assertEquals(40, doc.maxPlayers)
        assertTrue(doc.pvp)
        assertFalse(doc.onlineMode)
        assertTrue(doc.whiteList)
        assertFalse(doc.hardcore)
        assertTrue(doc.allowFlight)
        assertEquals(12, doc.viewDistance)
        assertEquals(8, doc.simulationDistance)
        assertEquals(0, doc.spawnProtection)
        assertEquals("survival_world", doc.levelName)
        assertEquals("123456789", doc.levelSeed)
        assertEquals("true", doc.customProperties["custom.plugin.flag"])
    }

    @Test
    fun testModifyCommonValuesAndSerialize() {
        var doc = ServerPropertiesDocument()

        // Modify common values
        doc = doc.copy(
            serverPort = 25577,
            motd = "§b§lAwesome Network",
            difficulty = "peaceful",
            gamemode = "creative",
            maxPlayers = 100,
            pvp = false
        )

        val text = doc.toPropertiesFileText()

        assertTrue(text.contains("server-port=25577"))
        assertTrue(text.contains("motd=§b§lAwesome Network"))
        assertTrue(text.contains("difficulty=peaceful"))
        assertTrue(text.contains("gamemode=creative"))
        assertTrue(text.contains("max-players=100"))
        assertTrue(text.contains("pvp=false"))

        // Re-parse and verify roundtrip fidelity
        val roundtrip = ServerPropertiesDocument.fromText(text)
        assertEquals(25577, roundtrip.serverPort)
        assertEquals("§b§lAwesome Network", roundtrip.motd)
        assertEquals("peaceful", roundtrip.difficulty)
        assertEquals("creative", roundtrip.gamemode)
        assertEquals(100, roundtrip.maxPlayers)
        assertFalse(roundtrip.pvp)
    }
}
