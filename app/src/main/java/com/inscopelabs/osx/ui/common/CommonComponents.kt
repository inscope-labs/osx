package com.inscopelabs.osx.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.osx.domain.profile.OperationalProfile
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
fun ConsoleTopBar(
    currentProfile: OperationalProfile,
    onProfileClick: () -> Unit,
    operatorId: String
) {
    Surface(
        color = VoidDark,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(TerminalGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OS/10 CONSOLE",
                        style = MaterialTheme.typography.titleLarge,
                        color = CyberCyan
                    )
                }
                Text(
                    text = "OP: $operatorId // IN-SCOPE LABS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            ProfileBadge(
                profile = currentProfile,
                onClick = onProfileClick,
                modifier = Modifier.testTag("profile_badge_btn")
            )
        }
    }
}

@Composable
fun ProfileBadge(
    profile: OperationalProfile,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (profile) {
        OperationalProfile.BASIC -> Triple(
            Color(0xFF1E293B),
            TextSecondary,
            "BASIC"
        )
        OperationalProfile.STANDARD -> Triple(
            Color(0xFF00363D),
            CyberCyan,
            "STANDARD"
        )
        OperationalProfile.HIGH -> Triple(
            Color(0xFF38104D),
            Color(0xFFE0AAFF),
            "HIGH (TEE)"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "PROFILE: $label",
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ConsoleSectionHeader(
    title: String,
    cnsSpec: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceCardAlt)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = cnsSpec,
                style = MaterialTheme.typography.labelSmall,
                color = ElectricBlue
            )
        }
    }
}

@Composable
fun TerminalCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BorderDark,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    accentColor: Color = CyberCyan,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCardAlt)
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HexValueText(
    value: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = value,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = CyberCyan,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(VoidDark)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}
