package com.inscopelabs.osx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.inscopelabs.osx.ui.audit.AuditScreen
import com.inscopelabs.osx.ui.common.ConsoleTopBar
import com.inscopelabs.osx.ui.dashboard.DashboardScreen
import com.inscopelabs.osx.ui.envelope.EnvelopeScreen
import com.inscopelabs.osx.ui.navigation.Screen
import com.inscopelabs.osx.ui.preauth.PreAuthScreen
import com.inscopelabs.osx.ui.runtime.RuntimeScreen
import com.inscopelabs.osx.ui.session.SessionScreen
import com.inscopelabs.osx.ui.settings.SettingsScreen
import com.inscopelabs.osx.ui.theme.BorderDark
import com.inscopelabs.osx.ui.theme.CyberCyan
import com.inscopelabs.osx.ui.theme.OsxConsoleTheme
import com.inscopelabs.osx.ui.theme.SurfaceCard
import com.inscopelabs.osx.ui.theme.TextMuted
import com.inscopelabs.osx.ui.theme.TextPrimary
import com.inscopelabs.osx.ui.theme.VoidDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appModule = (application as OsxApplication).appModule

        setContent {
            OsxConsoleTheme {
                MainContent(appModule)
            }
        }
    }
}

@Composable
fun MainContent(appModule: com.inscopelabs.osx.di.AppModule) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    val currentProfile by appModule.profileModule.profileSelector.currentProfile.collectAsState()

    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        currentScreen = Screen.DASHBOARD
    }

    Scaffold(
        topBar = {
            ConsoleTopBar(
                currentProfile = currentProfile,
                onProfileClick = { currentScreen = Screen.SETTINGS },
                operatorId = appModule.secureStorage.operatorId
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceCard,
                contentColor = TextPrimary
            ) {
                listOf(
                    Screen.DASHBOARD,
                    Screen.ENVELOPES,
                    Screen.SESSIONS,
                    Screen.RUNTIMES,
                    Screen.PREAUTH,
                    Screen.GATE_AUDIT,
                    Screen.SETTINGS
                ).forEach { screen ->
                    val selected = currentScreen == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = VoidDark
                        ),
                        modifier = Modifier.testTag("nav_tab_${screen.route}")
                    )
                }
            }
        },
        containerColor = VoidDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(VoidDark)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> DashboardScreen(
                    appModule = appModule,
                    onNavigate = { currentScreen = it }
                )
                Screen.ENVELOPES -> EnvelopeScreen(appModule = appModule)
                Screen.SESSIONS -> SessionScreen(appModule = appModule)
                Screen.RUNTIMES -> RuntimeScreen(appModule = appModule)
                Screen.PREAUTH -> PreAuthScreen(appModule = appModule)
                Screen.GATE_AUDIT -> AuditScreen(appModule = appModule)
                Screen.SETTINGS -> SettingsScreen(appModule = appModule)
            }
        }
    }
}
