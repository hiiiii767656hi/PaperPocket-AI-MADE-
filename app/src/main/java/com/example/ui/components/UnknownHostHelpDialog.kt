package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.server.HostResolutionResult
import com.example.server.PlayitTunnelManager
import com.example.ui.theme.PaperCyan
import com.example.ui.theme.PaperDarkBg
import com.example.ui.theme.PaperGold
import com.example.ui.theme.PaperGreen
import com.example.ui.theme.PaperPurple
import com.example.ui.theme.PaperRed
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.PaperSurfaceBorder
import kotlinx.coroutines.launch

@Composable
fun UnknownHostHelpDialog(
    deviceIp: String,
    serverPort: Int,
    currentPlayitAddress: String = "",
    claimUrl: String = "",
    claimCode: String = "",
    onSavePlayitAddress: (String) -> Unit = {},
    onRefreshClaimCode: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var testInput by remember(currentPlayitAddress) { mutableStateOf(currentPlayitAddress) }
    var testResult by remember { mutableStateOf<HostResolutionResult?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    val effectiveClaimUrl = if (claimUrl.isNotBlank()) claimUrl else if (claimCode.isNotBlank()) "https://playit.gg/claim/$claimCode" else "https://playit.gg/claim"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PaperRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = "Unknown Host",
                        tint = PaperGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "Fix 'Unknown Host' in Minecraft",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        "How to connect without DNS errors",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Why it happens
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperDarkBg),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PaperGold.copy(alpha = 0.4f))),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Why Minecraft Says 'Unknown Host':",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PaperGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Minecraft uses public DNS to find server domains. Placeholder domains (e.g. craft-xxxx) do not exist in DNS, so Minecraft triggers 'Unknown Host'. Use Option 1 for same Wi-Fi, or Option 2 to link your real Playit tunnel!",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Solution 1: Direct Local IP
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PaperCyan.copy(alpha = 0.5f))),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = PaperCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Option 1: Same Wi-Fi / Hotspot (Instant)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = PaperCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "For friends on the same Wi-Fi or phone hotspot, connect using your Direct IP. Raw IP addresses bypass DNS completely and NEVER trigger 'Unknown Host'!",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val localFullAddr = "$deviceIp:$serverPort"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(PaperDarkBg)
                                .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SelectionContainer {
                                Text(
                                    text = localFullAddr,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PaperGreen
                                )
                            }
                            IconButton(
                                onClick = {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("Direct IP", localFullAddr))
                                    Toast.makeText(context, "Copied $localFullAddr to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = PaperGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Solution 2: Real Playit.gg Agent Claim Link
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PaperGreen.copy(alpha = 0.5f))),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PaperGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Option 2: Internet Play via Playit.gg",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = PaperGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Each agent generates a unique Claim Link. Visiting it links this Android server directly to your Playit.gg account and creates your public .joinmc.link address!",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Claim URL Box
                        Text("AGENT CLAIM LINK:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PaperGreen)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(PaperDarkBg)
                                .border(1.dp, PaperSurfaceBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SelectionContainer(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = effectiveClaimUrl,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PaperGreen
                                )
                            }
                            IconButton(
                                onClick = {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("Playit Claim URL", effectiveClaimUrl))
                                    Toast.makeText(context, "Copied Claim Link to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Claim Link", tint = PaperGreen, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (claimCode.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Claim Code: ", fontSize = 11.sp, color = Color.Gray)
                                Text(claimCode, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PaperCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cb.setPrimaryClip(ClipData.newPlainText("Playit Claim Code", claimCode))
                                        Toast.makeText(context, "Copied claim code $claimCode!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = PaperCyan, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(effectiveClaimUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PaperGreen, contentColor = PaperDarkBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Claim Link", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onRefreshClaimCode,
                                shape = RoundedCornerShape(8.dp),
                                border = ButtonDefaults.outlinedButtonBorder,
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp), tint = Color.LightGray)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Fix Version Too Old box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(PaperPurple.copy(alpha = 0.15f))
                                .border(1.dp, PaperPurple.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    "Notice: 'Version Too Old (v0.0.0)' / 'Tunnel Not Supported'?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Tap 'Refresh' (or button below) to generate a modern Assignable Agent (v1.1.0). Once claimed, your Minecraft tunnel is created automatically via API!",
                                    fontSize = 10.sp,
                                    color = Color.LightGray,
                                    lineHeight = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = onRefreshClaimCode,
                                    colors = ButtonDefaults.buttonColors(containerColor = PaperPurple, contentColor = Color.White),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth().height(32.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Regenerate Modern v1.1.0 Agent", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Interactive Live DNS Tester
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperDarkBg),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PaperSurfaceBorder)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = PaperCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Live DNS & Host Diagnostic Tester",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Test any address before typing it in Minecraft to see if Minecraft can resolve it:",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = testInput,
                            onValueChange = { testInput = it },
                            placeholder = { Text("e.g. your-name.gl.joinmc.link", fontSize = 12.sp, color = Color.Gray) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PaperCyan,
                                unfocusedBorderColor = PaperSurfaceBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isTesting = true
                                        testResult = PlayitTunnelManager.testHostResolution(testInput)
                                        isTesting = false
                                    }
                                },
                                enabled = testInput.isNotBlank() && !isTesting,
                                colors = ButtonDefaults.buttonColors(containerColor = PaperCyan, contentColor = PaperDarkBg),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PaperDarkBg, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Testing...", fontSize = 12.sp)
                                } else {
                                    Text("Test Host Resolution", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (testResult?.isResolvable == true) {
                                Button(
                                    onClick = {
                                        onSavePlayitAddress(testInput.trim())
                                        Toast.makeText(context, "Saved as active Playit address!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PaperGreen, contentColor = PaperDarkBg),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("Use Address", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        // Test result feedback
                        testResult?.let { res ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (res.isResolvable) PaperGreen.copy(alpha = 0.15f) else PaperRed.copy(alpha = 0.15f))
                                    .border(1.dp, if (res.isResolvable) PaperGreen.copy(alpha = 0.5f) else PaperRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        if (res.isResolvable) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (res.isResolvable) PaperGreen else PaperRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = res.message,
                                        fontSize = 11.sp,
                                        color = if (res.isResolvable) PaperGreen else Color(0xFFFF8A80),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PaperCyan, contentColor = PaperDarkBg)
            ) {
                Text("Close Guide", fontWeight = FontWeight.Bold)
            }
        }
    )
}
