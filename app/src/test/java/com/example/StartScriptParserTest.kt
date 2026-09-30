package com.example

import com.example.utils.ServerPropertiesParser
import com.example.utils.StartScriptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StartScriptParserTest {

    @Test
    fun testParseSimpleStartBat() {
        val script = """
            @echo off
            java -Xms1024M -Xmx2048M -jar server.jar nogui
            pause
        """.trimIndent()

        val result = StartScriptParser.parse("start.bat", script)
        assertEquals("start.bat", result.scriptFileName)
        assertEquals("server.jar", result.jarFileName)
        assertEquals(2048, result.allocatedRamMb)
        assertEquals(1024, result.minRamMb)
        assertTrue(result.nogui)
    }

    @Test
    fun testParseAikarFlagsAndPaperJarWithGB() {
        val script = """
            @echo off
            java -Xms4G -Xmx6G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 ^
              -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch ^
              -Dpaper.playerconnection.keepalive=45 ^
              -jar paper-1.21.1-120.jar --nogui
            pause
        """.trimIndent()

        val result = StartScriptParser.parse("run.bat", script)
        assertEquals("paper-1.21.1-120.jar", result.jarFileName)
        assertEquals("1.21.1", result.detectedMinecraftVersion)
        assertEquals(120, result.detectedPaperBuild)
        assertEquals(6144, result.allocatedRamMb)
        assertEquals(4096, result.minRamMb)
        assertTrue(result.useAikarFlags)
        assertTrue(result.customJvmArgs.contains("-Dpaper.playerconnection.keepalive=45"))
        assertTrue(result.nogui)
    }

    @Test
    fun testParseCustomPortAndPurpurJar() {
        val script = """
            java -Xmx3072M -jar purpur-1.20.4-2169.jar --port 25570
        """.trimIndent()

        val result = StartScriptParser.parse("start.cmd", script)
        assertEquals("purpur-1.20.4-2169.jar", result.jarFileName)
        assertEquals("1.20.4", result.detectedMinecraftVersion)
        assertEquals(2169, result.detectedPaperBuild)
        assertEquals(3072, result.allocatedRamMb)
        assertEquals(25570, result.port)
    }

    @Test
    fun testParseServerProperties() {
        val props = """
            #Minecraft server properties
            server-port=25580
            motd=My Awesome SMP Server
            gamemode=survival
            difficulty=hard
            pvp=true
            online-mode=false
            max-players=32
            level-name=custom_world
            view-distance=12
        """.trimIndent()

        val result = ServerPropertiesParser.parse(props)
        assertEquals(25580, result.port)
        assertEquals("My Awesome SMP Server", result.motd)
        assertEquals("survival", result.gamemode)
        assertEquals("hard", result.difficulty)
        assertEquals(true, result.pvp)
        assertEquals(false, result.onlineMode)
        assertEquals(32, result.maxPlayers)
        assertEquals("custom_world", result.levelName)
        assertEquals(12, result.viewDistance)
    }
}
