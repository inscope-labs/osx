package com.inscopelabs.osx.ui.audit

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.inscopelabs.osx.domain.validation.GateAuditEntry
import com.inscopelabs.osx.domain.validation.RevocationRecord
import com.inscopelabs.osx.ui.common.ConsoleSectionHeader
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
import com.inscopelabs.osx.util.formatEpochShort

@Composable
fun AuditScreen(appModule: AppModule) {
    val auditLogs by appModule.validationGateEngine.auditLogs.collectAsState()
    val revocations by appModule.revocationHandler.revokedEntries.collectAsState()

    var revokeId by remember { mutableStateOf("") }
    var revokeReason by remember { mutableStateOf("Compromise suspected") }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ConsoleSectionHeader(
                title = "Emergency Revocation Portal",
                cnsSpec = "CNS-6 / CNS-1 INV-03"
            )

            TerminalCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "INSTANT REVOCATION INJECTION",
                        style = MaterialTheme.typography.titleMedium,
                        color = TerminalCrimson,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = revokeId,
                        onValueChange = { revokeId = it },
                        label = { Text("Identifier to Blacklist (Key ID / Envelope ID)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = revokeReason,
                        onValueChange = { revokeReason = it },
                        label = { Text("Revocation Reason", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (revokeId.isNotBlank()) {
                                appModule.revocationHandler.revoke(revokeId, "MANUAL_OPERATOR", revokeReason)
                                actionMessage = "Revocation record propagated for $revokeId. Invariant 03 in effect."
                                revokeId = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalCrimson),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_revocation_btn")
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = VoidDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Inject Immediate Revocation", color = VoidDark, fontWeight = FontWeight.Bold)
                    }

                    if (actionMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceCardAlt)
                                .padding(10.dp)
                        ) {
                            Text(text = actionMessage!!, color = TerminalCrimson, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        if (revocations.isNotEmpty()) {
            item {
                ConsoleSectionHeader(
                    title = "Certificate Revocation List (CRL)",
                    cnsSpec = "CNS-6"
                )
            }

            items(revocations) { rec ->
                RevocationCard(rec)
            }
        }

        item {
            ConsoleSectionHeader(
                title = "Validation Gate Audit Stream",
                cnsSpec = "CNS-6 / INVARIANTS"
            )
        }

        if (auditLogs.isEmpty()) {
            item {
                TerminalCard {
                    Text(
                        text = "No validation audits yet. Transactions dispatched through the console gate will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            items(auditLogs) { entry ->
                GateAuditCard(entry)
            }
        }
    }
}

@Composable
private fun RevocationCard(rec: RevocationRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TerminalCrimson.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Block, contentDescription = null, tint = TerminalCrimson, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(rec.identifier, style = MaterialTheme.typography.titleMedium, color = TerminalCrimson, fontWeight = FontWeight.Bold)
                    Text("REASON: ${rec.reason}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }
            Text(rec.revokedAt.formatEpochShort(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun GateAuditCard(entry: GateAuditEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (entry.passed) BorderDark else TerminalCrimson.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
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
                    Icon(
                        imageVector = if (entry.passed) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (entry.passed) TerminalGreen else TerminalCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (entry.passed) "GATE PASS" else "GATE REJECT",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (entry.passed) TerminalGreen else TerminalCrimson,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(entry.envelopeType, style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                }
                Text(entry.timestamp.formatEpochShort(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("TARGET: ${entry.targetId}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.details,
                style = MaterialTheme.typography.bodyMedium,
                color = if (entry.passed) TextPrimary else TerminalCrimson
            )
        }
    }
}
