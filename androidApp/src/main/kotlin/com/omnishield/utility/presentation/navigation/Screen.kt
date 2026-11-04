package com.omnishield.utility.presentation.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Converter : Screen("converter")
    data object Crypto : Screen("crypto")
    data object Metadata : Screen("metadata")
    data object Nfc : Screen("nfc")
    data object IrBlaster : Screen("ir_blaster")
    data object PdfStudio : Screen("pdf_studio")
    data object SecurityStudio : Screen("security_studio")
    data object HardwareStudio : Screen("hardware_studio")
    data object HexViewer : Screen("hex_viewer")
    data object NetworkStudio : Screen("network_studio") {
        fun createRoute(tab: Int = 0) = "network_studio?tab=$tab"
    }
    data object P2pTransfer : Screen("network_studio?tab=2")
    data object NotesVault : Screen("notes_vault")
    data object TokenInspector : Screen("token_inspector")
}
