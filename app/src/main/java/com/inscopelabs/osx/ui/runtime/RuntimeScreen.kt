package com.inscopelabs.osx.ui.runtime

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
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.osx.di.AppModule
import com.inscopelabs.osx.domain.runtime.model.ConsoleRuntime
import com.inscopelabs.osx.domain.runtime.model.RuntimeStatus
import com.inscopelabs.osx.domain.runtime.model.WorkOrder
import com.inscopelabs.osx.domain.runtime.model.WorkOrderStatus
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
fun RuntimeScreen(appModule: AppModule) {
    val runtimes by appModule.runtimeRegistry.runtimes.collectAsState()
    val workOrders by appModule.workOrderManager.workOrders.collectAsState()
    val preAuthTokens by appModule.preAuthorizationManager.tokens.collectAsState()

    var selectedOperation by remember { mutableStateOf("SECURITY_AUDIT_PASS") }
    var parametersJson by remember { mutableStateOf("{\"depth\": \"fast\", \"verifySignatures\": true}") }
    var budgetUnits by remember { mutableStateOf("50") }
    var dispatchStatus by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ConsoleSectionHeader(
                title = "Registered Execution Runtimes",
                cnsSpec = "CNS-5"
            )
        }

        items(runtimes) { runtime ->
            RuntimeNodeCard(runtime)
        }

        item {
            ConsoleSectionHeader(
                title = "Dispatch Work Order",
                cnsSpec = "CNS-5"
            )

            TerminalCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "NEW WORK ORDER SUBMISSION",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = selectedOperation,
                        onValueChange = { selectedOperation = it },
                        label = { Text("Operation Code", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = parametersJson,
                        onValueChange = { parametersJson = it },
                        label = { Text("Parameters JSON", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = budgetUnits,
                        onValueChange = { budgetUnits = it },
                        label = { Text("Computational Budget Units (CNS-1 Invariant 04)", color = TextMuted) },
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
                            val targetRuntime = runtimes.firstOrNull { it.status == RuntimeStatus.ONLINE } ?: runtimes.first()
                            val validPreAuth = preAuthTokens.firstOrNull { it.isValid }
                            val units = budgetUnits.toLongOrNull() ?: 50L

                            val result = appModule.workOrderManager.submitWorkOrder(
                                targetRuntime = targetRuntime,
                                operation = selectedOperation,
                                parameters = parametersJson,
                                budgetUnits = units,
                                preAuthTokenId = validPreAuth?.id
                            )

                            dispatchStatus = if (result.isSuccess) {
                                "Dispatched to ${targetRuntime.name}: ${result.getOrThrow().resultSummary}"
                            } else {
                                "Dispatch rejected: ${result.getOrNull()}"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_work_order_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = VoidDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit Work Order", color = VoidDark, fontWeight = FontWeight.Bold)
                    }

                    if (dispatchStatus != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceCardAlt)
                                .padding(10.dp)
                        ) {
                            Text(text = dispatchStatus!!, color = CyberCyan, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            ConsoleSectionHeader(
                title = "Work Orders Log",
                cnsSpec = "CNS-5"
            )
        }

        items(workOrders) { order ->
            WorkOrderCard(order)
        }
    }
}

@Composable
private fun RuntimeNodeCard(runtime: ConsoleRuntime) {
    TerminalCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = runtime.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (runtime.status == RuntimeStatus.ONLINE) TerminalGreen.copy(alpha = 0.2f) else TerminalAmber.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = runtime.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (runtime.status == RuntimeStatus.ONLINE) TerminalGreen else TerminalAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("ENGINE: ${runtime.executionEngine}", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
            Text("ENDPOINT: ${runtime.endpointUrl}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "PROFILES: ${runtime.supportedProfiles.joinToString(", ") { it.name }}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun WorkOrderCard(order: WorkOrder) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (order.status == WorkOrderStatus.COMPLETED) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (order.status == WorkOrderStatus.COMPLETED) TerminalGreen else TerminalCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.operation,
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(order.submittedAt.formatEpochShort(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text("ID: ${order.id} // BUDGET: ${order.budgetUnits} units", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            if (order.resultSummary != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = order.resultSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}
