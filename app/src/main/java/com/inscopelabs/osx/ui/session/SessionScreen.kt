package com.inscopelabs.osx.ui.session

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.inscopelabs.osx.domain.key.ConfirmationPolicy
import com.inscopelabs.osx.domain.session.model.ConsoleSession
import com.inscopelabs.osx.domain.session.model.SessionState
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
import com.inscopelabs.osx.util.formatEpoch
import com.inscopelabs.osx.util.formatEpochShort

@Composable
fun SessionScreen(appModule: AppModule) {
    val currentProfile by appModule.profileModule.profileSelector.currentProfile.collectAsState()
    val activeSession by appModule.sessionManager.activeSession.collectAsState()
    val sessionHistory by appModule.sessionManager.sessionHistory.collectAsState()

    var targetControllerId by remember { mutableStateOf("CTRL-CORE-01") }
    var handoffTargetConsole by remember { mutableStateOf("OSX-CONSOLE-RESERVE-02") }
    var handoffToken by remember { mutableStateOf("CONFIRM-HW-STEPUP-TOKEN-901") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ConsoleSectionHeader(
                title = "Session Lifecycle & Handoff",
                cnsSpec = "CNS-4"
            )

            if (activeSession != null) {
                TerminalCard(borderColor = TerminalGreen.copy(alpha = 0.6f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
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
                            HexValueText(value = activeSession!!.sessionId)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("OPERATOR", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(activeSession!!.operatorId, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            }
                            Column {
                                Text("PROFILE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(activeSession!!.profile.name, style = MaterialTheme.typography.bodyMedium, color = CyberCyan)
                            }
                            Column {
                                Text("LAST PING", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(activeSession!!.lastHeartbeatAt.formatEpochShort(), style = MaterialTheme.typography.bodyMedium, color = ElectricBlue)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    appModule.sessionManager.recordHeartbeat()
                                    statusMessage = "Recorded heartbeat telemetry keepalive"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = VoidDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Keepalive Ping", color = VoidDark, style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    val term = appModule.sessionManager.terminateSession()
                                    statusMessage = "Terminated session ${term.getOrNull()?.sessionId}"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TerminalCrimson.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Terminate Session", color = TerminalCrimson, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Handoff section
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceCardAlt)
                                .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "TRIGGER CNS-4 HANDOFF",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedTextField(
                                    value = handoffTargetConsole,
                                    onValueChange = { handoffTargetConsole = it },
                                    label = { Text("Target Console Node ID", color = TextMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = BorderDark,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = handoffToken,
                                    onValueChange = { handoffToken = it },
                                    label = { Text("Confirmation Auth Token", color = TextMuted) },
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
                                        val handoffResult = appModule.sessionManager.requestHandoff(
                                            targetConsoleId = handoffTargetConsole,
                                            confirmationToken = handoffToken
                                        )
                                        if (handoffResult.isSuccess) {
                                            statusMessage = "Handoff executed: ${handoffResult.getOrThrow().message}"
                                        } else {
                                            statusMessage = "Handoff failed: ${handoffResult.getOrNull() ?: "Check confirmation token"}"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("execute_handoff_btn")
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = VoidDark)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Transfer Session to Target", color = VoidDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                TerminalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "START NEW OPERATOR SESSION",
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = targetControllerId,
                            onValueChange = { targetControllerId = it },
                            label = { Text("Target Controller ID", color = TextMuted) },
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
                                val res = appModule.sessionManager.startSession(
                                    operatorId = appModule.secureStorage.operatorId,
                                    targetControllerId = targetControllerId,
                                    profile = currentProfile,
                                    policy = ConfirmationPolicy.USER_CONFIRMATION
                                )
                                statusMessage = if (res.isSuccess) {
                                    "Started session ${res.getOrThrow().sessionId}"
                                } else {
                                    "Failed to start session: ${res.getOrNull()}"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_session_btn")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = VoidDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Authorize & Start Session", color = VoidDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
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

        item {
            ConsoleSectionHeader(
                title = "Session Ledger & History",
                cnsSpec = "CNS-4 / ROOM"
            )
        }

        items(sessionHistory) { session ->
            SessionHistoryCard(session)
        }
    }
}

@Composable
private fun SessionHistoryCard(session: ConsoleSession) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
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
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (session.state) {
                                    SessionState.ACTIVE -> TerminalGreen
                                    SessionState.HANDED_OFF -> ElectricBlue
                                    else -> TextMuted
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = session.state.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (session.state) {
                            SessionState.ACTIVE -> TerminalGreen
                            SessionState.HANDED_OFF -> ElectricBlue
                            else -> TextMuted
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(session.startedAt.formatEpochShort(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text("SESSION: ${session.sessionId}", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
            Text("TARGET: ${session.targetControllerId}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            if (session.handoffTargetConsoleId != null) {
                Text("HANDED OFF TO: ${session.handoffTargetConsoleId}", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
            }
        }
    }
}
