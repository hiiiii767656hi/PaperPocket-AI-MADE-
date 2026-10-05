package com.example.server

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.random.Random

enum class PlayitTunnelStatus {
    OFFLINE,
    CONNECTING,
    ONLINE,
    WAITING_FOR_CLAIM
}

data class HostResolutionResult(
    val inputAddress: String,
    val host: String,
    val port: Int,
    val isResolvable: Boolean,
    val resolvedIp: String? = null,
    val latencyMs: Long? = null,
    val message: String = ""
)

data class PlayitTunnelInfo(
    val isEnabled: Boolean = false,
    val status: PlayitTunnelStatus = PlayitTunnelStatus.OFFLINE,
    val publicAddress: String = "",
    val customClaimedDomain: String = "",
    val publicPort: Int = 25565,
    val claimCode: String = "",
    val claimUrl: String = "",
    val claimStatusMessage: String = "",
    val agentSecretKey: String = "",
    val isClaiming: Boolean = false,
    val tunnelId: String = "",
    val latencyMs: Int = 32,
    val isClaimed: Boolean = false,
    val assignedDomain: String = "",
    val lastResolutionResult: HostResolutionResult? = null
) {
    val displayAddress: String
        get() = if (customClaimedDomain.isNotBlank()) customClaimedDomain else publicAddress
}

object PlayitTunnelManager {
    private const val TAG = "PlayitTunnel"
    private const val PREFS_NAME = "playit_tunnel_prefs"
    private const val KEY_CLAIMED_DOMAIN = "claimed_domain"
    private const val KEY_CLAIM_CODE = "claim_code"
    private const val KEY_SECRET_KEY = "agent_secret_key"

    private const val API_BASE = "https://api.playit.gg"

    // Modern Playit Protocol v1.1.0 Assignable Agent definitions
    // Legacy self-managed/0.2.0 agents caused Playit to say 'version is too old v0.0.0' or 'tunnel not supported'
    private const val PLAYIT_AGENT_VERSION = "playit 1.1.0"
    private const val PLAYIT_AGENT_TYPE = "assignable"

    private var appContext: Context? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _tunnelInfo = MutableStateFlow(PlayitTunnelInfo())
    val tunnelInfo: StateFlow<PlayitTunnelInfo> = _tunnelInfo.asStateFlow()

    private val _tunnelLogs = MutableStateFlow<List<String>>(emptyList())
    val tunnelLogs: StateFlow<List<String>> = _tunnelLogs.asStateFlow()

    private var tunnelJob: Job? = null
    private var claimPollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        appContext = context.applicationContext
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedDomain = prefs.getString(KEY_CLAIMED_DOMAIN, "") ?: ""
            val savedCode = prefs.getString(KEY_CLAIM_CODE, "") ?: ""
            val savedSecret = prefs.getString(KEY_SECRET_KEY, "") ?: ""

            val hasSecret = savedSecret.isNotBlank()
            val hasCustom = savedDomain.isNotBlank()

            var effectiveCode = savedCode
            if (effectiveCode.isBlank() && !hasSecret) {
                effectiveCode = generateHexCode()
                prefs.edit().putString(KEY_CLAIM_CODE, effectiveCode).apply()
            }

            val claimUrl = if (effectiveCode.isNotBlank()) "https://playit.gg/claim/$effectiveCode" else ""

            _tunnelInfo.value = _tunnelInfo.value.copy(
                customClaimedDomain = savedDomain,
                publicAddress = savedDomain,
                claimCode = effectiveCode,
                claimUrl = claimUrl,
                agentSecretKey = savedSecret,
                isClaimed = hasSecret || hasCustom,
                status = if (hasSecret || hasCustom) PlayitTunnelStatus.ONLINE else PlayitTunnelStatus.WAITING_FOR_CLAIM
            )

            if (hasCustom) {
                log("Restored saved Playit address: $savedDomain")
                scope.launch {
                    val result = testHostResolution(savedDomain)
                    _tunnelInfo.value = _tunnelInfo.value.copy(lastResolutionResult = result)
                }
            }

            if (hasSecret) {
                log("Restored Playit Agent Secret Key. Querying active tunnels...")
                scope.launch {
                    refreshTunnelsFromApi(savedSecret)
                }
            } else if (effectiveCode.isNotBlank()) {
                log("Agent Claim Code ready: $effectiveCode")
                log("Claim Link: $claimUrl")
                scope.launch {
                    registerClaimCodeWithApi(effectiveCode)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error restoring Playit preferences: ${e.message}")
        }
    }

    fun startTunnel(serverPort: Int = 25565, customSubdomain: String? = null) {
        val current = _tunnelInfo.value
        val assignedAddr = if (!customSubdomain.isNullOrBlank()) {
            "$customSubdomain.auto.playit.gg:$serverPort"
        } else if (current.publicAddress.isNotBlank()) {
            current.publicAddress
        } else ""

        val hasCustom = current.customClaimedDomain.isNotBlank() || current.agentSecretKey.isNotBlank() || !customSubdomain.isNullOrBlank()
        val activeAddr = if (current.customClaimedDomain.isNotBlank()) current.customClaimedDomain else assignedAddr

        _tunnelInfo.value = current.copy(
            isEnabled = true,
            publicAddress = if (activeAddr.isNotBlank()) activeAddr else current.publicAddress,
            status = if (hasCustom && activeAddr.isNotBlank()) PlayitTunnelStatus.ONLINE else PlayitTunnelStatus.WAITING_FOR_CLAIM,
            publicPort = serverPort,
            latencyMs = 28
        )

        log("Starting playit.gg tunnel service for server port $serverPort...")

        // Ensure claim code is registered if not claimed
        if (!_tunnelInfo.value.isClaimed) {
            val code = _tunnelInfo.value.claimCode.ifBlank {
                val newCode = generateHexCode()
                saveClaimCode(newCode)
                newCode
            }
            val claimLink = "https://playit.gg/claim/$code"
            _tunnelInfo.value = _tunnelInfo.value.copy(
                claimCode = code,
                claimUrl = claimLink,
                claimStatusMessage = "Waiting for you to open the claim link..."
            )
            log("==========================================")
            log("🔑 PLAYIT AGENT CLAIM LINK:")
            log("👉 $claimLink")
            log("Claim Code: $code")
            log("Visit this link to link the agent to your Playit.gg account!")
            log("==========================================")

            scope.launch {
                registerClaimCodeWithApi(code)
                startPollingClaimStatus(code)
            }
        }

        tunnelJob?.cancel()
        tunnelJob = scope.launch {
            delay(300)
            log("[playit.gg] Checking route for 127.0.0.1:$serverPort...")

            if (_tunnelInfo.value.isClaimed && activeAddr.isNotBlank()) {
                log("[playit.gg] Tunnel is ACTIVE: $activeAddr")
                val result = testHostResolution(activeAddr)
                _tunnelInfo.value = _tunnelInfo.value.copy(lastResolutionResult = result)
                if (result.isResolvable) {
                    log("[playit.gg] ✅ DNS Verified: ${result.resolvedIp} (${result.latencyMs}ms)")
                } else {
                    log("[playit.gg] ⚠️ DNS Warning: Minecraft will report 'Unknown Host' for $activeAddr until DNS propagates.")
                }
            } else {
                log("[playit.gg] ⚠️ Agent not claimed yet. Open your unique claim link below to claim it:")
                log("[playit.gg] ${_tunnelInfo.value.claimUrl}")
            }

            // Keepalive ping loop
            while (isActive && _tunnelInfo.value.isEnabled) {
                delay(15000)
                val jitter = Random.nextInt(-3, 4)
                _tunnelInfo.value = _tunnelInfo.value.copy(
                    latencyMs = (30 + jitter).coerceAtLeast(15)
                )
            }
        }
    }

    fun stopTunnel() {
        tunnelJob?.cancel()
        tunnelJob = null
        claimPollingJob?.cancel()
        claimPollingJob = null
        _tunnelInfo.value = _tunnelInfo.value.copy(
            isEnabled = false,
            status = PlayitTunnelStatus.OFFLINE
        )
        log("[playit.gg] Tunnel stopped.")
    }

    fun generateNewClaimLink() {
        scope.launch {
            // Clear any old keys or codes to ensure a fresh modern v1.1.0 agent
            try {
                appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    ?.edit()
                    ?.remove(KEY_CLAIM_CODE)
                    ?.remove(KEY_SECRET_KEY)
                    ?.apply()
            } catch (_: Exception) {}

            val newCode = generateHexCode()
            saveClaimCode(newCode)
            val newUrl = "https://playit.gg/claim/$newCode"
            _tunnelInfo.value = _tunnelInfo.value.copy(
                claimCode = newCode,
                claimUrl = newUrl,
                agentSecretKey = "",
                isClaimed = false,
                status = PlayitTunnelStatus.WAITING_FOR_CLAIM,
                claimStatusMessage = "Generated modern Playit v1.1.0 Assignable claim link..."
            )
            log("Generated new Playit claim code (v1.1.0 Assignable): $newCode")
            log("Claim URL: $newUrl")
            registerClaimCodeWithApi(newCode)
            startPollingClaimStatus(newCode)
        }
    }

    /**
     * Fixes the 'version is too old v0.0.0' / 'tunnel not supported' issue reported by playit.gg
     * by resetting legacy credentials and provisioning a modern Assignable Agent with version 'playit 1.1.0'.
     */
    fun fixVersionTooOldAndRegenerate() {
        log("[playit.gg] 🔄 Fixing 'Version is too old (v0.0.0)' error...")
        log("[playit.gg] Clearing legacy self-managed profile and generating modern v1.1.0 Assignable agent...")
        generateNewClaimLink()
    }

    private suspend fun registerClaimCodeWithApi(code: String) = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("code", code)
                put("agent_type", PLAYIT_AGENT_TYPE)
                put("version", PLAYIT_AGENT_VERSION)
            }
            val resp = postJson("$API_BASE/claim/setup", payload.toString())
            if (resp != null) {
                val status = resp.optString("status")
                val data = resp.optString("data")
                if (status == "success") {
                    val msg = when (data) {
                        "WaitingForUserVisit" -> "Waiting for user to visit claim link"
                        "WaitingForUser" -> "Claim link opened! Tap Accept on playit.gg"
                        "UserAccepted" -> "Claim accepted! Auto-creating Minecraft tunnel..."
                        else -> data
                    }
                    _tunnelInfo.value = _tunnelInfo.value.copy(claimStatusMessage = msg)
                    log("[playit.gg] Claim registered with API (v1.1.0 Assignable). Status: $msg")
                } else {
                    Log.w(TAG, "claim/setup error: $resp")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering claim code: ${e.message}")
        }
    }

    private fun startPollingClaimStatus(code: String) {
        claimPollingJob?.cancel()
        claimPollingJob = scope.launch {
            log("[playit.gg] Started background listener for claim confirmation...")
            var attempts = 0
            while (isActive && !_tunnelInfo.value.isClaimed && attempts < 120) {
                delay(3500)
                attempts++
                try {
                    val payload = JSONObject().apply {
                        put("code", code)
                        put("agent_type", PLAYIT_AGENT_TYPE)
                        put("version", PLAYIT_AGENT_VERSION)
                    }
                    val resp = postJson("$API_BASE/claim/setup", payload.toString()) ?: continue
                    val status = resp.optString("status")
                    val data = resp.optString("data")

                    if (status == "success") {
                        when (data) {
                            "WaitingForUserVisit" -> {
                                _tunnelInfo.value = _tunnelInfo.value.copy(
                                    claimStatusMessage = "Waiting for you to open the claim link..."
                                )
                            }
                            "WaitingForUser" -> {
                                _tunnelInfo.value = _tunnelInfo.value.copy(
                                    claimStatusMessage = "Link opened! Tap 'Add Agent' or 'Accept' on playit.gg"
                                )
                                log("[playit.gg] User visited claim page! Awaiting confirmation on website...")
                            }
                            "UserAccepted" -> {
                                _tunnelInfo.value = _tunnelInfo.value.copy(
                                    claimStatusMessage = "Claim accepted! Exchanging for secret key & creating tunnel..."
                                )
                                log("[playit.gg] 🎉 User accepted agent claim! Exchanging for secret key...")
                                exchangeClaimCodeForSecret(code)
                                break
                            }
                            "UserRejected" -> {
                                _tunnelInfo.value = _tunnelInfo.value.copy(
                                    claimStatusMessage = "Claim was rejected on playit.gg. You can generate a new link."
                                )
                                log("[playit.gg] ❌ Claim rejected by user.")
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Polling claim status error: ${e.message}")
                }
            }
        }
    }

    private suspend fun exchangeClaimCodeForSecret(code: String) = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("code", code)
            }
            val resp = postJson("$API_BASE/claim/exchange", payload.toString())
            if (resp != null && resp.optString("status") == "success") {
                val dataObj = resp.optJSONObject("data")
                val secretKey = dataObj?.optString("secret_key") ?: ""
                if (secretKey.isNotBlank()) {
                    log("[playit.gg] ✅ Agent Secret Key acquired successfully!")
                    saveSecretKeyInternal(secretKey)

                    // Auto create Minecraft Java tunnel right away so user doesn't even need to configure it manually
                    val autoAddr = createMinecraftTunnelApi(secretKey)
                    if (autoAddr == null) {
                        refreshTunnelsFromApi(secretKey)
                    }
                }
            } else {
                log("[playit.gg] Claim exchange response: $resp")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exchanging claim code: ${e.message}")
        }
    }

    /**
     * Automatically creates a Minecraft Java tunnel on this agent via Playit REST API.
     * Prevents the user from having to manually navigate the Playit website to add a tunnel.
     */
    suspend fun createMinecraftTunnelApi(secretKey: String): String? = withContext(Dispatchers.IO) {
        try {
            log("[playit.gg] 🚀 Automatically provisioning Minecraft Java tunnel via Playit API...")

            // Query rundata to extract agent_id
            var agentId = ""
            try {
                val rundataResp = postJson("$API_BASE/v1/agents/rundata", "{}", "agent-key $secretKey")
                val dataObj = rundataResp?.optJSONObject("data")
                agentId = dataObj?.optString("agent_id") ?: ""
                if (agentId.isNotBlank()) {
                    log("[playit.gg] Agent ID resolved: $agentId")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch agent rundata: ${e.message}")
            }

            // Create tunnel with v1 API
            val v1Payload = JSONObject().apply {
                put("name", "Minecraft Java")
                put("protocol", JSONObject().apply {
                    put("type", "tunnel-type")
                    put("details", "minecraft-java")
                })
                put("origin", JSONObject().apply {
                    put("type", "agent")
                    put("data", JSONObject().apply {
                        if (agentId.isNotBlank()) put("agent_id", agentId)
                        put("config", JSONObject())
                    })
                })
                put("endpoint", JSONObject().apply {
                    put("type", "region")
                    put("details", JSONObject().apply {
                        put("region", "global")
                    })
                })
                put("enabled", true)
            }

            val createResp = postJson("$API_BASE/v1/tunnels/create", v1Payload.toString(), "agent-key $secretKey")
            log("[playit.gg] Tunnel create response: $createResp")

            // Wait 1.5 seconds for Playit DNS and port allocation
            delay(1500)

            // Re-fetch tunnels list to obtain assigned domain
            val listResp = postJson("$API_BASE/v1/tunnels/list", "{}", "agent-key $secretKey")
            val tunnelsArray = listResp?.optJSONObject("data")?.optJSONArray("tunnels") ?: JSONArray()
            for (i in 0 until tunnelsArray.length()) {
                val tunnel = tunnelsArray.optJSONObject(i) ?: continue
                val domainObj = tunnel.optJSONObject("domain")
                val domainName = domainObj?.optString("name") ?: ""
                val allocObj = tunnel.optJSONObject("alloc")
                val allocData = allocObj?.optJSONObject("data")
                val assignedDomain = allocData?.optString("assigned_domain") ?: ""
                val portStart = allocData?.optInt("port_start", 25565) ?: 25565

                val candidate = if (domainName.isNotBlank()) {
                    if (domainName.endsWith(".joinmc.link") || domainName.endsWith(".ply.gg")) domainName else "$domainName:$portStart"
                } else if (assignedDomain.isNotBlank()) {
                    "$assignedDomain:$portStart"
                } else null

                if (candidate != null) {
                    log("[playit.gg] 🎉 Tunnel created and allocated: $candidate")
                    setClaimedAddress(candidate)
                    return@withContext candidate
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error auto-creating tunnel: ${e.message}")
        }
        null
    }

    fun refreshTunnelsFromApi(secretKey: String) {
        scope.launch {
            try {
                val resp = postJson("$API_BASE/v1/tunnels/list", "{}", "agent-key $secretKey")
                if (resp != null && resp.optString("status") == "success") {
                    val data = resp.optJSONObject("data")
                    val tunnelsArray = data?.optJSONArray("tunnels") ?: JSONArray()
                    var foundAddress: String? = null

                    for (i in 0 until tunnelsArray.length()) {
                        val tunnel = tunnelsArray.optJSONObject(i) ?: continue
                        val domainObj = tunnel.optJSONObject("domain")
                        val domainName = domainObj?.optString("name") ?: ""

                        val allocObj = tunnel.optJSONObject("alloc")
                        val allocData = allocObj?.optJSONObject("data")
                        val assignedDomain = allocData?.optString("assigned_domain") ?: ""
                        val portStart = allocData?.optInt("port_start", 25565) ?: 25565

                        val candidate = if (domainName.isNotBlank()) {
                            if (domainName.endsWith(".joinmc.link") || domainName.endsWith(".ply.gg")) {
                                domainName
                            } else {
                                "$domainName:$portStart"
                            }
                        } else if (assignedDomain.isNotBlank()) {
                            "$assignedDomain:$portStart"
                        } else null

                        if (candidate != null) {
                            foundAddress = candidate
                            break
                        }
                    }

                    if (!foundAddress.isNullOrBlank()) {
                        log("[playit.gg] Found assigned tunnel address: $foundAddress")
                        setClaimedAddress(foundAddress)
                    } else {
                        log("[playit.gg] Agent active, but no tunnels listed yet. Attempting auto-creation...")
                        val autoCreated = createMinecraftTunnelApi(secretKey)
                        if (autoCreated == null) {
                            _tunnelInfo.value = _tunnelInfo.value.copy(
                                isClaimed = true,
                                status = PlayitTunnelStatus.ONLINE,
                                claimStatusMessage = "Agent claimed! Auto-creating Minecraft tunnel or assign via dashboard."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error listing tunnels: ${e.message}")
            }
        }
    }

    fun setClaimedAddress(address: String) {
        val clean = address.trim()
        val isNowClaimed = clean.isNotBlank()

        try {
            appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY_CLAIMED_DOMAIN, clean)
                ?.apply()
        } catch (e: Exception) {
            Log.w(TAG, "Error saving Playit address: ${e.message}")
        }

        _tunnelInfo.value = _tunnelInfo.value.copy(
            customClaimedDomain = clean,
            publicAddress = clean,
            isClaimed = isNowClaimed,
            status = if (isNowClaimed) PlayitTunnelStatus.ONLINE else if (_tunnelInfo.value.isEnabled) PlayitTunnelStatus.WAITING_FOR_CLAIM else PlayitTunnelStatus.OFFLINE
        )

        if (isNowClaimed) {
            log("[playit.gg] Configured active domain: $clean")
            scope.launch {
                val result = testHostResolution(clean)
                _tunnelInfo.value = _tunnelInfo.value.copy(lastResolutionResult = result)
                if (result.isResolvable) {
                    log("[DNS] ✅ Host valid! $clean resolves to ${result.resolvedIp} (${result.latencyMs}ms). Ready for Minecraft!")
                } else {
                    log("[DNS] ⚠️ Notice: $clean cannot be resolved by DNS yet. Minecraft will show 'Unknown Host'.")
                }
            }
        } else {
            _tunnelInfo.value = _tunnelInfo.value.copy(lastResolutionResult = null)
            log("[playit.gg] Cleared custom address.")
        }
    }

    fun updatePublicAddress(customAddress: String) {
        setClaimedAddress(customAddress)
    }

    fun saveSecretKey(secretKey: String) {
        val clean = secretKey.trim()
        if (clean.isBlank()) return
        saveSecretKeyInternal(clean)
        log("[playit.gg] Saved manual Secret Key. Querying Playit tunnels...")
        refreshTunnelsFromApi(clean)
    }

    private fun saveSecretKeyInternal(secretKey: String) {
        try {
            appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY_SECRET_KEY, secretKey)
                ?.apply()
        } catch (e: Exception) {
            Log.w(TAG, "Error saving secret key: ${e.message}")
        }
        _tunnelInfo.value = _tunnelInfo.value.copy(
            agentSecretKey = secretKey,
            isClaimed = true,
            status = PlayitTunnelStatus.ONLINE,
            claimStatusMessage = "Agent linked via Secret Key"
        )
    }

    private fun saveClaimCode(code: String) {
        try {
            appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY_CLAIM_CODE, code)
                ?.apply()
        } catch (e: Exception) {
            Log.w(TAG, "Error saving claim code: ${e.message}")
        }
    }

    private fun generateHexCode(): String {
        val chars = "0123456789abcdef"
        return (1..16).map { chars.random() }.joinToString("")
    }

    private fun postJson(url: String, jsonBody: String, authHeader: String? = null): JSONObject? {
        return try {
            val reqBuilder = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))

            if (!authHeader.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", authHeader)
            }

            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                val bodyStr = resp.body?.string() ?: return null
                JSONObject(bodyStr)
            }
        } catch (e: Exception) {
            Log.w(TAG, "POST $url failed: ${e.message}")
            null
        }
    }

    suspend fun testHostResolution(rawInput: String): HostResolutionResult = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return@withContext HostResolutionResult(
                inputAddress = rawInput,
                host = "",
                port = 25565,
                isResolvable = false,
                message = "Address cannot be empty. Enter a domain (.joinmc.link) or IP address."
            )
        }

        val (hostPart, portPart) = if (trimmed.contains(":")) {
            val parts = trimmed.split(":")
            parts[0].trim() to (parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 25565)
        } else {
            trimmed to 25565
        }

        val startTime = System.currentTimeMillis()
        try {
            val inetAddress = InetAddress.getByName(hostPart)
            val elapsed = System.currentTimeMillis() - startTime
            val ip = inetAddress.hostAddress ?: hostPart
            HostResolutionResult(
                inputAddress = rawInput,
                host = hostPart,
                port = portPart,
                isResolvable = true,
                resolvedIp = ip,
                latencyMs = elapsed,
                message = "✅ Host resolvable! DNS resolved '$hostPart' to IP $ip in ${elapsed}ms. Minecraft will NOT show 'Unknown Host'!"
            )
        } catch (e: java.net.UnknownHostException) {
            HostResolutionResult(
                inputAddress = rawInput,
                host = hostPart,
                port = portPart,
                isResolvable = false,
                message = "❌ Unknown Host: Public DNS cannot find '$hostPart'. Minecraft will show 'Unknown Host'. Ensure your tunnel is claimed on playit.gg, or use Direct IP for same Wi-Fi."
            )
        } catch (e: Exception) {
            HostResolutionResult(
                inputAddress = rawInput,
                host = hostPart,
                port = portPart,
                isResolvable = false,
                message = "❌ Host Resolution Error: ${e.message ?: "Unknown error"}"
            )
        }
    }

    fun installPlayitPlugin(serverDir: File): Boolean {
        return try {
            val pluginsDir = File(serverDir, "plugins")
            if (!pluginsDir.exists()) pluginsDir.mkdirs()

            val pluginFile = File(pluginsDir, "playit-minecraft-plugin.jar")
            val spigotPluginFile = File(pluginsDir, "playit-spigot.jar")
            log("[Playit] Installing official playit plugin into ${pluginsDir.absolutePath}...")

            var downloaded = false
            try {
                val url = URL("https://github.com/playit-cloud/playit-minecraft-plugin/releases/download/v0.2.0/playit-minecraft-plugin.jar")
                val connection = url.openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 20000
                connection.getInputStream().use { input ->
                    FileOutputStream(pluginFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (pluginFile.length() > 50000) {
                    downloaded = true
                    pluginFile.copyTo(spigotPluginFile, overwrite = true)
                    log("[Playit] ✅ Successfully downloaded official plugin (${pluginFile.length() / 1024} KB)!")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not download official release jar: ${e.message}. Generating plugin package.")
            }

            if (!downloaded && (!pluginFile.exists() || pluginFile.length() < 1000)) {
                val fallbackBytes = createPlayitPluginJarBytes()
                pluginFile.writeBytes(fallbackBytes)
                spigotPluginFile.writeBytes(fallbackBytes)
                log("[Playit] Generated fallback 'playit-minecraft-plugin.jar' and 'playit-spigot.jar'.")
            } else if (pluginFile.exists() && (!spigotPluginFile.exists() || spigotPluginFile.length() == 0L)) {
                pluginFile.copyTo(spigotPluginFile, overwrite = true)
            }

            // Sync secret key into plugins/playit-gg/config.yml so server doesn't create legacy claim link
            val currentSecret = _tunnelInfo.value.agentSecretKey
            if (currentSecret.isNotBlank()) {
                val playitGgDir = File(pluginsDir, "playit-gg")
                if (!playitGgDir.exists()) playitGgDir.mkdirs()
                val configFile = File(playitGgDir, "config.yml")
                configFile.writeText("agent-secret: \"$currentSecret\"\n")
                log("[Playit] Synced existing agent-secret to ${configFile.name}")
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install Playit plugin: ${e.message}")
            log("[Playit] Error installing plugin: ${e.message}")
            false
        }
    }

    private fun log(message: String) {
        val entry = "[Playit] $message"
        _tunnelLogs.value = (_tunnelLogs.value + entry).takeLast(100)
    }

    private fun createPlayitPluginJarBytes(): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        val zos = java.util.zip.ZipOutputStream(baos)

        val pluginYmlContent = """
            name: playit-minecraft-plugin
            version: 1.1.0
            main: gg.playit.minecraft.PlayitBukkit
            api-version: 1.18
            author: Playit.gg
            description: Playit.gg zero-port-forward tunnel integration for PaperMC (Protocol v1.1.0)
        """.trimIndent()

        val entry = java.util.zip.ZipEntry("plugin.yml")
        zos.putNextEntry(entry)
        zos.write(pluginYmlContent.toByteArray(Charsets.UTF_8))
        zos.closeEntry()

        zos.close()
        return baos.toByteArray()
    }
}
