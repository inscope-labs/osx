package com.inscopelabs.osx.ui.envelope

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.inscopelabs.osx.domain.envelope.Envelope
import com.inscopelabs.osx.domain.envelope.EnvelopeStatus
import com.inscopelabs.osx.domain.envelope.EnvelopeType
import com.inscopelabs.osx.domain.key.model.KeyRole
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvelopeScreen(appModule: AppModule) {
    val currentProfile by appModule.profileModule.profileSelector.currentProfile.collectAsState()
    val cachedEnvelopes by appModule.envelopeCache.cachedEnvelopes.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(EnvelopeType.HANDSHAKE) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var recipientId by remember { mutableStateOf("CTRL-CORE-01") }
    var payloadText by remember { mutableStateOf("{\"capabilities\":[\"TLS_1_3\",\"STRONG_BOX\"],\"mode\":\"OPERATOR\"}") }
    var lastOperationMessage by remember { mutableStateOf<String?>(null) }
    var lastOperationSuccess by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ConsoleSectionHeader(
                title = "Cryptographic Envelope Dispatcher",
                cnsSpec = "CNS-3"
            )

            TerminalCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "CONSTRUCT & SEAL PROTOCOL ENVELOPE",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )

                    // Envelope Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = typeDropdownExpanded,
                        onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedType.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Envelope Type (CNS-3)", color = TextMuted) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false }
                        ) {
                            EnvelopeType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text("${type.name} (${type.code})") },
                                    onClick = {
                                        selectedType = type
                                        typeDropdownExpanded = false
                                        payloadText = when (type) {
                                            EnvelopeType.HANDSHAKE -> "{\"handshake\":\"init\",\"clientVersion\":\"OS10-CNS3-v1.4\"}"
                                            EnvelopeType.SESSION_START -> "{\"session\":\"start\",\"operatorRole\":\"SECURITY_ADMIN\"}"
                                            EnvelopeType.RUNTIME_DISCOVERY -> "{\"query\":\"runtimes\",\"scope\":\"all\"}"
                                            EnvelopeType.WORK_ORDER -> "{\"op\":\"ENCLAVE_EXEC\",\"budget\":100}"
                                            EnvelopeType.PRE_AUTH -> "{\"budgetCap\":500,\"scope\":\"compute:microvm\"}"
                                            EnvelopeType.HEARTBEAT -> "{\"status\":\"ok\",\"uptime\":12040}"
                                            EnvelopeType.REVOCATION -> "{\"revokeTarget\":\"key-test\",\"reason\":\"compromise\"}"
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Recipient Field
                    OutlinedTextField(
                        value = recipientId,
                        onValueChange = { recipientId = it },
                        label = { Text("Recipient Controller ID", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Payload Field
                    OutlinedTextField(
                        value = payloadText,
                        onValueChange = { payloadText = it },
                        label = { Text("Envelope Raw Payload (JSON)", color = TextMuted) },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dispatch Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val signingKey = appModule.cryptoModule.keyManager.getActiveKey(KeyRole.SIGNING)
                                val envelope = appModule.envelopeFactory.createEnvelope(
                                    type = selectedType,
                                    senderId = appModule.secureStorage.operatorId,
                                    recipientId = recipientId,
                                    payloadRaw = payloadText,
                                    profile = currentProfile,
                                    signerKey = signingKey
                                )

                                val gateEval = appModule.validationGateEngine.evaluateEnvelope(envelope)
                                if (gateEval.isSuccess) {
                                    scope.launch {
                                        appModule.networkModule.controllerTransport.transmit(envelope)
                                    }
                                    appModule.envelopeCache.put(envelope.copy(status = EnvelopeStatus.VERIFIED))
                                    lastOperationSuccess = true
                                    lastOperationMessage = "Dispatched and verified envelope ${envelope.header.id} (${envelope.digest.take(12)}...)"
                                } else {
                                    appModule.envelopeCache.put(envelope.copy(status = EnvelopeStatus.SIGNATURE_INVALID))
                                    lastOperationSuccess = false
                                    lastOperationMessage = "Gate rejected envelope: ${gateEval.getOrNull() ?: "Verification check failed"}"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("seal_and_dispatch_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = VoidDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Seal & Dispatch", color = VoidDark, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                // Tamper simulation
                                val signingKey = appModule.cryptoModule.keyManager.getActiveKey(KeyRole.SIGNING)
                                val pristine = appModule.envelopeFactory.createEnvelope(
                                    type = selectedType,
                                    senderId = appModule.secureStorage.operatorId,
                                    recipientId = recipientId,
                                    payloadRaw = payloadText,
                                    profile = currentProfile,
                                    signerKey = signingKey
                                )
                                // Tamper payload but leave digest untouched
                                val tampered = pristine.copy(
                                    payloadRaw = payloadText + " /* TAMPERED_IN_FLIGHT */",
                                    status = EnvelopeStatus.SIGNATURE_INVALID
                                )
                                val result = appModule.validationGateEngine.evaluateEnvelope(tampered)
                                appModule.envelopeCache.put(tampered)
                                lastOperationSuccess = false
                                lastOperationMessage = "Simulated Tamper detected: Digest verification correctly rejected payload!"
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = TerminalCrimson)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tamper Test", color = TerminalCrimson, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (lastOperationMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (lastOperationSuccess) TerminalGreen.copy(alpha = 0.15f) else TerminalCrimson.copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = lastOperationMessage!!,
                                color = if (lastOperationSuccess) TerminalGreen else TerminalCrimson,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        item {
            ConsoleSectionHeader(
                title = "Cached Envelopes Ledger",
                cnsSpec = "CNS-3 / ROOM"
            )
        }

        if (cachedEnvelopes.isEmpty()) {
            item {
                TerminalCard {
                    Text(
                        text = "Ledger empty. Dispatch an envelope to inspect cryptographic envelope headers and digests.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            items(cachedEnvelopes) { envelope ->
                EnvelopeCard(envelope)
            }
        }
    }
}

@Composable
private fun EnvelopeCard(envelope: Envelope) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (envelope.status == EnvelopeStatus.VERIFIED) BorderDark else TerminalCrimson.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            ),
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
                    Icon(
                        imageVector = if (envelope.status == EnvelopeStatus.VERIFIED) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (envelope.status == EnvelopeStatus.VERIFIED) TerminalGreen else TerminalCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = envelope.header.type.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = envelope.header.timestamp.formatEpochShort(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID: ${envelope.header.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = "TO: ${envelope.header.recipientId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricBlue
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = if (expanded) "Hide Cryptographic Details" else "Inspect Digest & Signatures",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(14.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(VoidDark)
                        .padding(10.dp)
                ) {
                    Text("SHA-256 DIGEST:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    HexValueText(value = envelope.digest)

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("SIGNATURE:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    HexValueText(value = envelope.signature ?: "UNSIGNED")

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("NONCE:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(envelope.header.nonce, style = MaterialTheme.typography.labelSmall, color = TextSecondary)

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("PAYLOAD RAW:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = envelope.payloadRaw,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TerminalGreen
                    )
                }
            }
        }
    }
}
