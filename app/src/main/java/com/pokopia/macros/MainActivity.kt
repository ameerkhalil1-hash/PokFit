package com.pokopia.macros

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.pokopia.macros.ui.PokopiaViewModel
import com.pokopia.macros.ui.screens.DittosCafeScreen
import com.pokopia.macros.ui.screens.IslandSanctuaryScreen
import com.pokopia.macros.ui.screens.PokopiaPantryScreen
import com.pokopia.macros.ui.screens.PorygonBarcodeScannerScreen
import com.pokopia.macros.ui.screens.PorygonResearchLabScreen
import com.pokopia.macros.ui.theme.PokopiaColors
import com.pokopia.macros.ui.theme.PokopiaMacrosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_OPEN_SCANNER = "com.pokopia.macros.ACTION_OPEN_SCANNER"
        const val ACTION_OPEN_CAMERA = "com.pokopia.macros.ACTION_OPEN_CAMERA"
    }

    private val viewModel: PokopiaViewModel by viewModels()
    private var selectedTabIndex by mutableIntStateOf(0)
    private var openAiCameraModalOnIsland by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        setContent {
            PokopiaMacrosTheme {
                PokopiaMainScaffold(
                    viewModel = viewModel,
                    selectedTab = selectedTabIndex,
                    openAiCameraModalInitially = openAiCameraModalOnIsland,
                    onSelectTab = { idx ->
                        openAiCameraModalOnIsland = false
                        selectedTabIndex = idx
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_OPEN_SCANNER -> {
                selectedTabIndex = 3 // Tab 4: Porygon Cyber-Scanner
            }
            ACTION_OPEN_CAMERA -> {
                selectedTabIndex = 0 // Tab 1: Island Sanctuary with Porygon Lens open
                openAiCameraModalOnIsland = true
            }
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null && uri.scheme == "pokopia" && uri.host == "oauth2callback") {
                    selectedTabIndex = 4 // Tab 5: Porygon Research Lab
                    viewModel.syncWearablesAndStrava(this, null)
                }
            }
        }
    }
}

private enum class PokopiaNavTab(
    val title: String,
    val icon: ImageVector
) {
    ISLAND("Island", Icons.Default.Spa),
    PANTRY("Pantry", Icons.Default.Kitchen),
    CAFE("Ditto Cafe", Icons.Default.RestaurantMenu),
    SCANNER("Scanner", Icons.Default.QrCodeScanner),
    LAB("Cyber Lab", Icons.Default.Analytics)
}

@Composable
private fun PokopiaMainScaffold(
    viewModel: PokopiaViewModel,
    selectedTab: Int,
    openAiCameraModalInitially: Boolean,
    onSelectTab: (Int) -> Unit
) {
    val tabs = PokopiaNavTab.entries

    Scaffold(
        containerColor = PokopiaColors.DeepOceanBg,
        bottomBar = {
            NavigationBar(
                containerColor = PokopiaColors.LagoonSurface
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { onSelectTab(index) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PokopiaColors.PorygonCyberCyan,
                            selectedTextColor = PokopiaColors.PorygonCyberCyan,
                            unselectedIconColor = PokopiaColors.TextMutedSand,
                            unselectedTextColor = PokopiaColors.TextMutedSand,
                            indicatorColor = PokopiaColors.DriftwoodCard
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> IslandSanctuaryScreen(
                    viewModel = viewModel,
                    openAiCameraModalInitially = openAiCameraModalInitially
                )
                1 -> PokopiaPantryScreen(viewModel = viewModel)
                2 -> DittosCafeScreen(viewModel = viewModel)
                3 -> PorygonBarcodeScannerScreen(viewModel = viewModel)
                4 -> PorygonResearchLabScreen(viewModel = viewModel)
            }
        }
    }
}
