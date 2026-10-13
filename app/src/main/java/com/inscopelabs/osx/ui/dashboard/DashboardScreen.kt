package com.inscopelabs.osx.ui.dashboard

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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inscopelabs.osx.di.AppModule
import com.inscopelabs.osx.domain.envelope.EnvelopeType
import com.inscopelabs.osx.domain.key.ConfirmationPolicy
import com.inscopelabs.osx.domain.profile.OperationalProfile
import com.inscopelabs.osx.domain.session.model.ConsoleSession
import com.inscopelabs.osx.domain.validation.GateAuditEntry
import com.inscopelabs.osx.ui.common.ConsoleSectionHeader
import com.inscopelabs.osx.ui.common.HexValueText
import com.inscopelabs.osx.ui.common.MetricTile
import com.inscopelabs.osx.ui.common.TerminalCard
import com.inscopelabs.osx.ui.navigation.Screen
import com.inscopelabs.osx.ui.theme.BorderDark
import com.inscopelabs.osx.ui.theme.CyberCyan
import com.inscopelabs.osx.ui.theme.ElectricBlue
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
fun DashboardScreen(
    appModule: AppModule,
    onNavigate: (Screen) -> Unit
) {
    val currentProfile by appModule.profileModule.profileSelector.currentProfile.collectAsState()
    val activeSession by appModule.sessionManager.activeSession.collectAsState()
    val auditLogs by appModule.validationGateEngine.auditLogs.collectAsState()
    val runtimes by appModule.runtimeRegistry.runtimes.collectAsState()
    val preAuthTokens by appModule.preAuthorizationManager.tokens.collectAsState()
    val transportStats by appModule.networkModule.controllerTransport.stats.collectAsState()

    val onlineRuntimesCount = runtimes.count { it.status.name == "ONLINE" }
    val totalRemainingBudget = preAuthTokens.filter { it.isValid }.sumOf { it.remainingUnits }
    val passedAuditsCount = auditLogs.count { it.passed }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ConsoleSectionHeader(
                title = "Security Posture & Telemetry",
                cnsSpec = "CNS-1 / CNS-7"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile(
                    label = "Active Gate Audits",
                    value = "${auditLogs.size}",
                    accentColor = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Online Runtimes",
                    value = "$onlineRuntimesCount/${runtimes.size}",
                    accentColor = TerminalGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Pre-Auth Units",
                    value = "$totalRemainingBudget",
                    accentColor = ElectricBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            ConsoleSectionHeader(
                title = "Active Operator Session",
                cnsSpec = "CNS-4"
            )

            if (activeSession != null) {
                ActiveSessionCard(
                    session = activeSession!!,
                    onHeartbeat = { appModule.sessionManager.recordHeartbeat() },
                    onTerminate = { appModule.sessionManager.terminateSession() },
                    onHandoff = { onNavigate(Screen.SESSIONS) }
                )
            } else {
                NoSessionCard(
                    onStartSession = {
                        appModule.sessionManager.startSession(
                            operatorId = appModule.secureStorage.operatorId,
                            targetControllerId = "CTRL-CORE-01",
                            profile = currentProfile,
                            policy = if (currentProfile == OperationalProfile.HIGH) ConfirmationPolicy.STEP_UP_AUTHENTICATED else ConfirmationPolicy.USER_CONFIRMATION
                        )
                    }
                )
            }
        }

        item {
            ConsoleSectionHeader(
                title = "Quick Command Actions",
                cnsSpec = "CNS-3 / CNS-5"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onNavigate(Screen.ENVELOPES) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_dispatch_envelope")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Dispatch",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Button(
                    onClick = { onNavigate(Screen.RUNTIMES) },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardAlt),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_work_orders")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        modifier = Modifier.size(16.dp),
                        tint = CyberCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Work Order",
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Button(
                    onClick = { onNavigate(Screen.SETTINGS) },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardAlt),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_security_profile")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Profile",
                        modifier = Modifier.size(16.dp),
                        tint = TerminalAmber
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Security",
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        item {
            ConsoleSectionHeader(
                title = "Live Validation Gate Stream",
                cnsSpec = "CNS-6"
            )
        }

        if (auditLogs.isEmpty()) {
            item {
                TerminalCard {
                    Text(
                        text = "Validation gate idle. Transmit envelopes to generate live cryptographic gate records.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            items(auditLogs.take(5)) { audit ->
                AuditItemRow(audit)
            }
        }
    }
}

@Composable
private fun ActiveSessionCard(
    session: ConsoleSession,
    onHeartbeat: () -> Unit,
    onTerminate: () -> Unit,
    onHandoff: () -> Unit
) {
    TerminalCard(borderColor = TerminalGreen.copy(alpha = 0.5f)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(TerminalGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SESSION ACTIVE",
                        style = MaterialTheme.typography.titleMedium,
                        color = TerminalGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                HexValueText(value = session.sessionId)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("TARGET CONTROLLER", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(session.targetControllerId, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Column {
                    Text("LAST HEARTBEAT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(session.lastHeartbeatAt.formatEpochShort(), style = MaterialTheme.typography.bodyMedium, color = CyberCyan)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onHeartbeat,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Heartbeat",
                        tint = TerminalCrimson,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ping", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onHandoff,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Handoff",
                        tint = CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Handoff", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onTerminate,
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalCrimson.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Logout", color = TerminalCrimson, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun NoSessionCard(onStartSession: () -> Unit) {
    TerminalCard(borderColor = TerminalAmber.copy(alpha = 0.5f)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NO OPERATOR SESSION ATTACHED",
                style = MaterialTheme.typography.titleMedium,
                color = TerminalAmber,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Initiate an authenticated operator session to anchor cryptographic envelopes.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onStartSession,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("initiate_session_btn")
            ) {
                Text("Initiate Session (CNS-4)", color = VoidDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AuditItemRow(audit: GateAuditEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (audit.passed) BorderDark else TerminalCrimson.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardAlt),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (audit.passed) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (audit.passed) TerminalGreen else TerminalCrimson,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = audit.envelopeType,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = audit.timestamp.formatEpochShort(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Text(
                        text = audit.details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
