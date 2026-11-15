package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.ConnectionTestResult
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.LocateTarget

/** Locale-neutral state formatted only when it reaches the main UI. */
sealed interface MainStatus {
    data object Disconnected : MainStatus
    data object Connected : MainStatus
    data object Testing : MainStatus
    data class TestProgress(val progress: String) : MainStatus
    data class ConnectionTest(val result: ConnectionTestResult) : MainStatus
}

/**
 * Main UI state
 */
data class MainUiState(
    val connectingUntil: Long = 0,
    val telemetry: com.v2ray.ang.dto.SessionTelemetry? = null,
    val groups: List<GroupMapItem> = emptyList(),
    val selectedGroupId: String = "",
    val selectedGuid: String? = null,
    val isRunning: Boolean = false,
    val isTesting: Boolean = false,
    val status: MainStatus = MainStatus.Disconnected,
    val locateTarget: LocateTarget? = null,
    val doubleColumnDisplay: Boolean = false,
    val isFavorite: Boolean = false,
    val selectionReason: String? = null,
    val preferredCountry: String? = null,
    val connectionMode: ConnectionMode = ConnectionMode.AUTO,
    val qualityAssessment: com.v2ray.ang.core.QualityAssessment? = null,
)

enum class ConnectionMode { AUTO, WIREGUARD, SECURE }

/**
 * All possible user interaction intents
 */
sealed interface MainAction {
    data object ConnectionRequested : MainAction
    data object Initialize : MainAction
    data object ToggleService : MainAction
    data object TestCurrentServer : MainAction
    data object ToggleFavorite : MainAction
    data object OpenVpnSecuritySettings : MainAction
    data object TestRealAllServers : MainAction
    data object CancelTesting : MainAction
    data object RestartService : MainAction
    data object LocateSelectedServer : MainAction

    data class SelectServer(val guid: String) : MainAction
    data class SelectCountry(val countryCode: String?) : MainAction
    data class SelectConnectionMode(val mode: ConnectionMode) : MainAction
    data object LocateHandled : MainAction
}
