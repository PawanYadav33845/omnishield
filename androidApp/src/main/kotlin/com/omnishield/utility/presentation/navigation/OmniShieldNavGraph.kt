package com.omnishield.utility.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omnishield.utility.data.repository.CryptoRepository
import com.omnishield.utility.data.repository.FileConverterRepository
import com.omnishield.utility.data.repository.MetadataRepository
import com.omnishield.utility.presentation.components.OmniShieldBottomBar
import com.omnishield.utility.presentation.converter.FileConverterScreen
import com.omnishield.utility.presentation.converter.FileConverterViewModel
import com.omnishield.utility.presentation.crypto.CryptoViewModel
import com.omnishield.utility.presentation.crypto.EncryptionScreen
import com.omnishield.utility.presentation.dashboard.DashboardViewModel
import com.omnishield.utility.presentation.dashboard.MainDashboardScreen
import com.omnishield.utility.presentation.hardware.HardwareStudioScreen
import com.omnishield.utility.presentation.hardware.HardwareStudioViewModel
import com.omnishield.utility.presentation.hex.HexViewerScreen
import com.omnishield.utility.presentation.hex.HexViewerViewModel
import com.omnishield.utility.presentation.ir.IrBlasterScreen
import com.omnishield.utility.presentation.ir.IrViewModel
import com.omnishield.utility.presentation.metadata.MetadataStripperScreen
import com.omnishield.utility.presentation.metadata.MetadataViewModel
import com.omnishield.utility.presentation.network.NetworkStudioScreen
import com.omnishield.utility.presentation.network.NetworkStudioViewModel
import com.omnishield.utility.presentation.nfc.NfcScreen
import com.omnishield.utility.presentation.nfc.NfcViewModel
import com.omnishield.utility.presentation.pdf.PdfStudioScreen
import com.omnishield.utility.presentation.pdf.PdfStudioViewModel
import com.omnishield.utility.presentation.security.SecurityStudioScreen
import com.omnishield.utility.presentation.security.SecurityStudioViewModel
import com.omnishield.utility.presentation.tokens.TokenInspectorScreen
import com.omnishield.utility.presentation.tokens.TokenInspectorViewModel
import com.omnishield.utility.presentation.vault.NotesVaultScreen
import com.omnishield.utility.presentation.vault.NotesVaultViewModel

@Composable
fun OmniShieldNavGraph(
    navController: NavHostController = rememberNavController(),
    nfcViewModel: NfcViewModel = viewModel()
) {
    val context = LocalContext.current.applicationContext

    val converterRepo = remember { FileConverterRepository(context) }
    val cryptoRepo = remember { CryptoRepository(context) }
    val metadataRepo = remember { MetadataRepository(context) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            OmniShieldBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route
            ) {
                composable(Screen.Dashboard.route) {
                    val dashboardViewModel: DashboardViewModel = viewModel()
                    MainDashboardScreen(
                        viewModel = dashboardViewModel,
                        onNavigateTo = { route -> navController.navigate(route) }
                    )
                }

                composable(Screen.SecurityStudio.route) {
                    val securityViewModel: SecurityStudioViewModel = viewModel {
                        SecurityStudioViewModel(context)
                    }
                    SecurityStudioScreen(
                        viewModel = securityViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.PdfStudio.route) {
                    val pdfViewModel: PdfStudioViewModel = viewModel {
                        PdfStudioViewModel(context)
                    }
                    PdfStudioScreen(
                        viewModel = pdfViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.HardwareStudio.route) {
                    val hardwareViewModel: HardwareStudioViewModel = viewModel {
                        HardwareStudioViewModel(context)
                    }
                    HardwareStudioScreen(
                        viewModel = hardwareViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Crypto.route) {
                    val cryptoViewModel: CryptoViewModel = viewModel {
                        CryptoViewModel(cryptoRepo)
                    }
                    EncryptionScreen(
                        viewModel = cryptoViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Metadata.route) {
                    val metadataViewModel: MetadataViewModel = viewModel {
                        MetadataViewModel(metadataRepo)
                    }
                    MetadataStripperScreen(
                        viewModel = metadataViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Converter.route) {
                    val converterViewModel: FileConverterViewModel = viewModel {
                        FileConverterViewModel(converterRepo)
                    }
                    FileConverterScreen(
                        viewModel = converterViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.HexViewer.route) {
                    val hexViewModel: HexViewerViewModel = viewModel {
                        HexViewerViewModel(context)
                    }
                    HexViewerScreen(
                        viewModel = hexViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Nfc.route) {
                    NfcScreen(
                        viewModel = nfcViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.IrBlaster.route) {
                    val irViewModel: IrViewModel = viewModel()
                    IrBlasterScreen(
                        viewModel = irViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "network_studio?tab={tab}",
                    arguments = listOf(
                        navArgument("tab") {
                            type = NavType.IntType
                            defaultValue = 0
                        }
                    )
                ) { backStackEntry ->
                    val initialTab = backStackEntry.arguments?.getInt("tab") ?: 0
                    val networkViewModel: NetworkStudioViewModel = viewModel {
                        NetworkStudioViewModel(context)
                    }
                    LaunchedEffect(initialTab) {
                        networkViewModel.selectTab(initialTab)
                    }
                    NetworkStudioScreen(
                        viewModel = networkViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.NetworkStudio.route) {
                    val networkViewModel: NetworkStudioViewModel = viewModel {
                        NetworkStudioViewModel(context)
                    }
                    NetworkStudioScreen(
                        viewModel = networkViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.NotesVault.route) {
                    val notesViewModel: NotesVaultViewModel = viewModel {
                        NotesVaultViewModel(context)
                    }
                    NotesVaultScreen(
                        viewModel = notesViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.TokenInspector.route) {
                    val tokenViewModel: TokenInspectorViewModel = viewModel()
                    TokenInspectorScreen(
                        viewModel = tokenViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
