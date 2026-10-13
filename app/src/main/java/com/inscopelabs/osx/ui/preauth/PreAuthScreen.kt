package com.inscopelabs.osx.ui.preauth

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.inscopelabs.osx.domain.runtime.model.PreAuthStatus
import com.inscopelabs.osx.domain.runtime.model.PreAuthorization
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
import com.inscopelabs.osx.util.formatEpochShort

@Composable
fun PreAuthScreen(appModule: AppModule) {
    val tokens by appModule.preAuthorizationManager.tokens.collectAsState()

    var scopeText by remember { mutableStateOf("compute:microvm:standard") }
    var budgetCapText by remember { mutableStateOf("500") }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ConsoleSectionHeader(
                title = "Issue Pre-Authorization Token",
                cnsSpec = "CNS-5 / CNS-1 INV-04"
            )

            TerminalCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "CRYPTOGRAPHIC BUDGET ALLOCATION",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = scopeText,
                        onValueChange = { scopeText = it },
                        label = { Text("Authorized Scope", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = budgetCapText,
                        onValueChange = { budgetCapText = it },
                        label = { Text("Budget Cap Units", color = TextMuted) },
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
                            val cap = budgetCapText.toLongOrNull() ?: 500L
                            val token = appModule.preAuthorizationManager.issuePreAuthorization(scopeText, cap)
                            actionMessage = "Issued Pre-Auth Token ${token.token} with $cap units"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("issue_preauth_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = VoidDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Grant Pre-Authorization", color = VoidDark, fontWeight = FontWeight.Bold)
                    }

                    if (actionMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceCardAlt)
                                .padding(10.dp)
                        ) {
                            Text(text = actionMessage!!, color = CyberCyan, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            ConsoleSectionHeader(
                title = "Active Pre-Authorization Tokens",
                cnsSpec = "CNS-5"
            )
        }

        items(tokens) { token ->
            PreAuthTokenCard(
                token = token,
                onDebitTest = {
                    val debitRes = appModule.preAuthorizationManager.debitBudget(token.id, 100L)
                    actionMessage = if (debitRes.isSuccess) {
                        "Debited 100 units from ${token.token}. Remaining: ${debitRes.getOrThrow().remainingUnits}"
                    } else {
                        "Debit failed: ${debitRes.getOrNull()}"
                    }
                },
                onRevoke = {
                    val rev = appModule.preAuthorizationManager.revokeToken(token.id)
                    actionMessage = "Revoked pre-auth token ${rev.getOrNull()?.token}"
                }
            )
        }
    }
}

@Composable
private fun PreAuthTokenCard(
    token: PreAuthorization,
    onDebitTest: () -> Unit,
    onRevoke: () -> Unit
) {
    val progress = if (token.budgetCapUnits > 0) token.remainingUnits.toFloat() / token.budgetCapUnits.toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp)),
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
                Text(
                    text = token.token,
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (token.status) {
                                PreAuthStatus.ACTIVE -> TerminalGreen.copy(alpha = 0.2f)
                                PreAuthStatus.EXHAUSTED -> TerminalAmber.copy(alpha = 0.2f)
                                else -> TerminalCrimson.copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = token.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (token.status) {
                            PreAuthStatus.ACTIVE -> TerminalGreen
                            PreAuthStatus.EXHAUSTED -> TerminalAmber
                            else -> TerminalCrimson
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("SCOPE: ${token.scope}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CyberCyan,
                trackColor = SurfaceCardAlt
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "REMAINING: ${token.remainingUnits} / ${token.budgetCapUnits} units",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary
                )
                Text(
                    text = "EXPIRES: ${token.expiresAt.formatEpochShort()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            if (token.status == PreAuthStatus.ACTIVE) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDebitTest,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Debit 100 units", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = onRevoke,
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalCrimson.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Revoke", color = TerminalCrimson, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
