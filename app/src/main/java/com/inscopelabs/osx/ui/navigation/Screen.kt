package com.inscopelabs.osx.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val cnsSpec: String
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard, "CNS-1"),
    ENVELOPES("envelopes", "Envelopes", Icons.Default.Send, "CNS-3"),
    SESSIONS("sessions", "Sessions", Icons.Default.Lock, "CNS-4"),
    RUNTIMES("runtimes", "Runtimes", Icons.Default.Memory, "CNS-5"),
    PREAUTH("preauth", "Pre-Auth", Icons.Default.AccountTree, "CNS-5"),
    GATE_AUDIT("gate_audit", "Gate Audit", Icons.Default.Security, "CNS-6"),
    SETTINGS("settings", "Security & Profiles", Icons.Default.Key, "CNS-7")
}
