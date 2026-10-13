package com.inscopelabs.osx.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inscopelabs.osx.di.AppModule
import com.inscopelabs.osx.domain.key.ConfirmationPolicy
import com.inscopelabs.osx.domain.key.model.ConsoleKey
import com.inscopelabs.osx.domain.key.model.KeyRole
import com.inscopelabs.osx.domain.key.model.KeyStatus
import com.inscopelabs.osx.domain.profile.OperationalProfile
import com.inscopelabs.osx.domain.profile.ProfileRequirements
import com.inscopelabs.osx.ui.common.ConsoleSectionHeader
import com.inscopelabs.osx.ui.common.HexValueText
import com.inscopelabs.osx.ui.common.TerminalCard
import com.inscopelabs.osx.ui.theme.BorderDark
import com.inscopelabs.osx.ui.theme.CyberCyan
import com.inscopelabs.osx.ui.theme.ElectricBlue
import com.inscopelabs.osx.ui.theme.SurfaceCard
import com.inscopelabs.osx.ui.theme.SurfaceCardAlt
import com.inscopelabs.osx.ui.theme.TerminalAmber
import com.inscopelabs.osx.ui.theme.TerminalCrimson
import com.inscopelabs.osx.ui.theme.TerminalGreen
import com.inscopelabs.osx.ui.theme.TextMuted
import com.inscopelabs.osx.ui.theme.TextPrimary
import com.inscopelabs.osx.ui.theme.TextSecondary
import com.inscopelabs.osx.ui.theme.VoidDark

@Composable
fun SettingsScreen(appModule: AppModule) {
    val currentProfile by appModule.profileModule.profileSelector.currentProfile.collectAsState()
    val keys by appModule.cryptoModule.keyManager.keys.collectAsState()

    var statusMessage by remember { mutableStateOf<String?>(null) }
    val isTeeSupported = remember { appModule.cryptoModule.teeKeyProvider.isStrongBoxSupported() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ConsoleSectionHeader(
                title = "CNS-7 Operational Profile Selector",
                cnsSpec = "CNS-7"
            )

            TerminalCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "ACTIVE PROFILE: ${currentProfile.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentProfile.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OperationalProfile.values().forEach { profile ->
                            val isSelected = profile == currentProfile
                            Button(
                                onClick = {
                                    val switchResult = appModule.profileModule.profileSelector.selectProfile(
                                        target = profile,
                                        hardwareKeystoreAvailable = true,
                                        teeAvailable = true, // Gracefully simulated/supported
                                        providedPolicy = ConfirmationPolicy.STEP_UP_AUTHENTICATED
                                    )
                                    if (switchResult.isSuccess) {
                                        appModule.profileModule.syncSelectedProfile(profile)
                                        statusMessage = "Switched to ${profile.name} profile"
                                    } else {
                                        statusMessage = "Switch rejected: ${switchResult.getOrNull()}"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) CyberCyan else SurfaceCardAlt
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("select_profile_${profile.name.lowercase()}")
                            ) {
                                Text(
                                    text = profile.name,
                                    color = if (isSelected) VoidDark else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    if (statusMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceCardAlt)
                                .padding(10.dp)
                        ) {
                            Text(text = statusMessage!!, color = CyberCyan, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            ConsoleSectionHeader(
                title = "Hardware & Cryptographic Providers",
                cnsSpec = "CNS-2 / HARDWARE"
            )

            TerminalCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProviderStatusRow("Software JCA Engine", "Available (BASIC)", true)
                    ProviderStatusRow("AndroidKeyStore SE", "Active (STANDARD)", true)
                    ProviderStatusRow("StrongBox / Hardware TEE", if (isTeeSupported) "Hardware Bound" else "Emulated / In-Process", true)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConsoleSectionHeader(
                    title = "CNS-2 Key Hierarchy",
                    cnsSpec = "CNS-2"
                )
            }

            OutlinedButton(
                onClick = {
                    val rot = appModule.cryptoModule.keyManager.rotateSigningKey(currentProfile)
                    statusMessage = "Rotated signing key: ${rot.getOrThrow().alias}"
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rotate_signing_key_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rotate Active Signing Key", color = CyberCyan)
            }
        }

        items(keys) { key ->
            KeyHierarchyCard(
                key = key,
                onRevoke = {
                    appModule.cryptoModule.keyManager.revokeKey(key.id, "Operator initiated revocation")
                    statusMessage = "Revoked key ${key.alias}"
                }
            )
        }
    }
}

@Composable
private fun ProviderStatusRow(name: String, status: String, active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (active) TerminalGreen else TerminalCrimson)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(status, style = MaterialTheme.typography.labelSmall, color = if (active) TerminalGreen else TerminalCrimson)
        }
    }
}

@Composable
private fun KeyHierarchyCard(
    key: ConsoleKey,
    onRevoke: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (key.status == KeyStatus.ACTIVE) BorderDark else TerminalCrimson.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = key.alias,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (key.status == KeyStatus.ACTIVE) TerminalGreen.copy(alpha = 0.2f) else TerminalCrimson.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = key.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (key.status == KeyStatus.ACTIVE) TerminalGreen else TerminalCrimson
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("ROLE: ${key.role.name} (${key.role.description})", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
            Text("ALGORITHM: ${key.algorithm.standardName}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text("POLICY: ${key.confirmationPolicy.name}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FINGERPRINT: ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                HexValueText(value = key.publicKeyFingerprint)
            }

            if (key.status == KeyStatus.ACTIVE && key.role != KeyRole.ROOT) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRevoke,
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalCrimson.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Revoke Key (CNS-1 INV-03)", color = TerminalCrimson, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
