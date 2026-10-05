package com.example

import com.example.server.NbtByte
import com.example.server.NbtCompound
import com.example.server.NbtInt
import com.example.server.NbtString
import com.example.server.RealMinecraftServerSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class PlayableServerSessionTest {

    @Test
    fun testVarIntSerialization() {
        val testValues = listOf(0, 1, 127, 128, 255, 25565, 763, 765, 2097151)
        for (v in testValues) {
            val baos = ByteArrayOutputStream()
            val dos = DataOutputStream(baos)
            RealMinecraftServerSession.writeVarInt(dos, v)
            val bytes = baos.toByteArray()

            val dis = DataInputStream(ByteArrayInputStream(bytes))
            val decoded = RealMinecraftServerSession.readVarInt(dis)
            assertEquals("VarInt mismatch for $v", v, decoded)
        }
    }

    @Test
    fun testNbtCompoundSerialization() {
        val compound = NbtCompound(
            mapOf(
                "testString" to NbtString("PaperHost"),
                "testInt" to NbtInt(25565),
                "testByte" to NbtByte(1)
            )
        )
        val bytes = compound.toByteArray("root")
        assertTrue(bytes.isNotEmpty())
        assertEquals(10, bytes[0].toInt()) // TAG_Compound
    }

    @Test
    fun testDimensionCodecGeneration() {
        val codec = NbtCompound.buildDimensionCodec()
        val bytes = codec.toByteArray()
        assertTrue(bytes.size > 100)
        assertNotNull(codec.tags["minecraft:dimension_type"])
        assertNotNull(codec.tags["minecraft:worldgen/biome"])
        assertNotNull(codec.tags["minecraft:chat_type"])
    }

    @Test
    fun testPojavClientStateDefaults() {
        val state = com.example.server.PojavClientState()
        assertEquals("1.21.1", state.minecraftVersion)
        assertEquals("Pojav_Steve", state.username)
        assertEquals(2048, state.allocatedRamMb)
        assertEquals(0, state.selectedHotbarSlot)
        assertEquals(20f, state.health, 0.01f)
    }
}
