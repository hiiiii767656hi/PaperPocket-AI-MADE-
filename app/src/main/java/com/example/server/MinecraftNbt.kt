package com.example.server

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

sealed class NbtTag {
    abstract val id: Byte
    abstract fun writePayload(dos: DataOutputStream)
}

class NbtByte(val value: Byte) : NbtTag() {
    constructor(v: Int) : this(v.toByte())
    override val id: Byte = 1
    override fun writePayload(dos: DataOutputStream) = dos.writeByte(value.toInt())
}

class NbtShort(val value: Short) : NbtTag() {
    constructor(v: Int) : this(v.toShort())
    override val id: Byte = 2
    override fun writePayload(dos: DataOutputStream) = dos.writeShort(value.toInt())
}

class NbtInt(val value: Int) : NbtTag() {
    override val id: Byte = 3
    override fun writePayload(dos: DataOutputStream) = dos.writeInt(value)
}

class NbtLong(val value: Long) : NbtTag() {
    override val id: Byte = 4
    override fun writePayload(dos: DataOutputStream) = dos.writeLong(value)
}

class NbtFloat(val value: Float) : NbtTag() {
    override val id: Byte = 5
    override fun writePayload(dos: DataOutputStream) = dos.writeFloat(value)
}

class NbtDouble(val value: Double) : NbtTag() {
    override val id: Byte = 6
    override fun writePayload(dos: DataOutputStream) = dos.writeDouble(value)
}

class NbtString(val value: String) : NbtTag() {
    override val id: Byte = 8
    override fun writePayload(dos: DataOutputStream) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        dos.writeShort(bytes.size)
        dos.write(bytes)
    }
}

class NbtList(val elementTagId: Byte, val elements: List<NbtTag>) : NbtTag() {
    override val id: Byte = 9
    override fun writePayload(dos: DataOutputStream) {
        dos.writeByte(elementTagId.toInt())
        dos.writeInt(elements.size)
        for (el in elements) {
            el.writePayload(dos)
        }
    }
}

class NbtCompound(val tags: Map<String, NbtTag> = emptyMap()) : NbtTag() {
    override val id: Byte = 10

    override fun writePayload(dos: DataOutputStream) {
        for ((name, tag) in tags) {
            dos.writeByte(tag.id.toInt())
            val nameBytes = name.toByteArray(Charsets.UTF_8)
            dos.writeShort(nameBytes.size)
            dos.write(nameBytes)
            tag.writePayload(dos)
        }
        dos.writeByte(0) // TAG_End
    }

    fun toByteArray(rootName: String = ""): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        dos.writeByte(10) // TAG_Compound
        val nameBytes = rootName.toByteArray(Charsets.UTF_8)
        dos.writeShort(nameBytes.size)
        dos.write(nameBytes)
        writePayload(dos)
        dos.flush()
        return baos.toByteArray()
    }

    companion object {
        val EMPTY = NbtCompound()

        fun buildDimensionCodec(): NbtCompound {
            val overworldElement = NbtCompound(
                mapOf(
                    "piglin_safe" to NbtByte(0),
                    "natural" to NbtByte(1),
                    "ambient_light" to NbtFloat(0.0f),
                    "infiniburn" to NbtString("#minecraft:infiniburn_overworld"),
                    "respawn_anchor_works" to NbtByte(0),
                    "has_skylight" to NbtByte(1),
                    "bed_works" to NbtByte(1),
                    "effects" to NbtString("minecraft:overworld"),
                    "has_raids" to NbtByte(1),
                    "min_y" to NbtInt(-64),
                    "height" to NbtInt(384),
                    "logical_height" to NbtInt(384),
                    "coordinate_scale" to NbtDouble(1.0),
                    "ultrawarm" to NbtByte(0),
                    "has_ceiling" to NbtByte(0)
                )
            )

            val overworldDimension = NbtCompound(
                mapOf(
                    "name" to NbtString("minecraft:overworld"),
                    "id" to NbtInt(0),
                    "element" to overworldElement
                )
            )

            val dimensionTypeCompound = NbtCompound(
                mapOf(
                    "type" to NbtString("minecraft:dimension_type"),
                    "value" to NbtList(10, listOf(overworldDimension))
                )
            )

            val plainsEffects = NbtCompound(
                mapOf(
                    "sky_color" to NbtInt(7907327),
                    "water_fog_color" to NbtInt(329011),
                    "fog_color" to NbtInt(12638463),
                    "water_color" to NbtInt(4159204)
                )
            )

            val plainsElement = NbtCompound(
                mapOf(
                    "has_precipitation" to NbtByte(1),
                    "temperature" to NbtFloat(0.8f),
                    "downfall" to NbtFloat(0.4f),
                    "effects" to plainsEffects
                )
            )

            val plainsBiome = NbtCompound(
                mapOf(
                    "name" to NbtString("minecraft:plains"),
                    "id" to NbtInt(0),
                    "element" to plainsElement
                )
            )

            val biomeCompound = NbtCompound(
                mapOf(
                    "type" to NbtString("minecraft:worldgen/biome"),
                    "value" to NbtList(10, listOf(plainsBiome))
                )
            )

            val chatElement = NbtCompound(
                mapOf(
                    "chat" to NbtCompound(
                        mapOf(
                            "translation_key" to NbtString("chat.type.text"),
                            "parameters" to NbtList(8, listOf(NbtString("sender"), NbtString("content")))
                        )
                    ),
                    "narration" to NbtCompound(
                        mapOf(
                            "translation_key" to NbtString("chat.type.text.narrate"),
                            "parameters" to NbtList(8, listOf(NbtString("sender"), NbtString("content")))
                        )
                    )
                )
            )

            val chatCompound = NbtCompound(
                mapOf(
                    "type" to NbtString("minecraft:chat_type"),
                    "value" to NbtList(
                        10,
                        listOf(
                            NbtCompound(
                                mapOf(
                                    "name" to NbtString("minecraft:chat"),
                                    "id" to NbtInt(0),
                                    "element" to chatElement
                                )
                            )
                        )
                    )
                )
            )

            val damageTypeCompound = NbtCompound(
                mapOf(
                    "type" to NbtString("minecraft:damage_type"),
                    "value" to NbtList(
                        10,
                        listOf(
                            NbtCompound(
                                mapOf(
                                    "name" to NbtString("minecraft:generic"),
                                    "id" to NbtInt(0),
                                    "element" to NbtCompound(
                                        mapOf(
                                            "message_id" to NbtString("generic"),
                                            "scaling" to NbtString("when_caused_by_living_non_player"),
                                            "exhaustion" to NbtFloat(0.0f)
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )

            return NbtCompound(
                mapOf(
                    "minecraft:dimension_type" to dimensionTypeCompound,
                    "minecraft:worldgen/biome" to biomeCompound,
                    "minecraft:chat_type" to chatCompound,
                    "minecraft:damage_type" to damageTypeCompound
                )
            )
        }
    }
}
