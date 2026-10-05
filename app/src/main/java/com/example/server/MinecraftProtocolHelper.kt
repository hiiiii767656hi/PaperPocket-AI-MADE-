package com.example.server

object MinecraftProtocolHelper {

    // Common Java Edition protocol mappings (including current Minecraft 26.x series)
    private val VERSION_TO_PROTOCOL = mapOf(
        "26.3" to 770,
        "26.2" to 770,
        "26.1" to 770,
        "26.0" to 770,
        "26" to 770,
        "1.21.4" to 769,
        "1.21.3" to 768,
        "1.21.2" to 768,
        "1.21.1" to 767,
        "1.21" to 767,
        "1.20.6" to 766,
        "1.20.5" to 766,
        "1.20.4" to 765,
        "1.20.3" to 765,
        "1.20.2" to 764,
        "1.20.1" to 763,
        "1.20" to 763,
        "1.19.4" to 762,
        "1.19.3" to 761,
        "1.19.2" to 760,
        "1.19.1" to 760,
        "1.19" to 759,
        "1.18.2" to 758,
        "1.18.1" to 757,
        "1.18" to 757,
        "1.17.1" to 756,
        "1.17" to 755,
        "1.16.5" to 754,
        "1.16.4" to 754,
        "1.16.3" to 753,
        "1.16.2" to 751,
        "1.16.1" to 749,
        "1.16" to 748,
        "1.15.2" to 578,
        "1.14.4" to 498,
        "1.13.2" to 404,
        "1.12.2" to 340,
        "1.8.9" to 47,
        "1.8.8" to 47,
        "1.8" to 47
    )

    val POPULAR_VERSIONS = listOf(
        "26.3",
        "1.21.4",
        "1.21.1",
        "1.20.4",
        "1.20.1",
        "1.19.4",
        "1.18.2",
        "1.16.5",
        "1.12.2",
        "1.8.8"
    )

    /**
     * Resolves the Minecraft protocol version integer for a given version string.
     * Supports versions like "26.3", "1.21.4", "1.20.4", "Paper 26.3", etc.
     */
    fun getProtocolForVersion(version: String): Int {
        val clean = version.trim().lowercase()
            .removePrefix("v")
            .removePrefix("paper ")
            .removePrefix("purpur ")
            .removePrefix("spigot ")
            .removePrefix("minecraft ")
            .trim()

        VERSION_TO_PROTOCOL[clean]?.let { return it }

        if (clean.startsWith("26")) return 770

        // Partial matching
        for ((ver, proto) in VERSION_TO_PROTOCOL) {
            if (clean.startsWith(ver)) {
                return proto
            }
        }

        // If user typed an explicit numeric protocol (e.g. 767 or 770)
        clean.toIntOrNull()?.let {
            if (it in 4..999) return it
        }

        return 770 // modern default
    }

    /**
     * Finds the canonical version string for a protocol number, if known.
     */
    fun getVersionForProtocol(protocol: Int): String? {
        return VERSION_TO_PROTOCOL.entries.firstOrNull { it.value == protocol }?.key
    }

    /**
     * Attempts to parse a Minecraft version from a jar filename like:
     * - paper-26.3-12.jar -> "26.3"
     * - paper-1.20.4-496.jar -> "1.20.4"
     * - purpur-1.21.1-2290.jar -> "1.21.1"
     */
    fun extractVersionFromFileName(fileName: String): String? {
        val regex = Regex("""\b(\d{1,2}(?:\.\d{1,2}){1,2})\b""")
        val match = regex.find(fileName)
        return match?.value
    }
}
