package com.v2ray.ang.ui.main

import android.content.Intent
import android.content.BroadcastReceiver
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.enums.PermissionType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.handler.UpdateInstaller
import com.v2ray.ang.ui.AboutActivity
import com.v2ray.ang.ui.base.HelperBaseComponentActivity
import com.v2ray.ang.ui.checkupdate.CheckUpdateActivity
import com.v2ray.ang.util.Utils

class MainActivity : HelperBaseComponentActivity() {

    private val updateViewModel: com.v2ray.ang.ui.checkupdate.CheckUpdateViewModel by viewModels()
    private var updateReceiver: BroadcastReceiver? = null

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application, MainRepository(application as AngApplication))
    }

    private val requestVpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) startTunnel()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateReceiver = UpdateInstaller.register(this)
        mainViewModel.onAction(MainAction.Initialize)
        if (savedInstanceState == null) updateViewModel.checkAutomatically()

        checkAndRequestPermission(PermissionType.POST_NOTIFICATIONS) {}
    }

    override fun onResume() {
        super.onResume()
        UpdateInstaller.openPendingInstall(this)
    }

    override fun onDestroy() {
        updateReceiver?.let { runCatching { unregisterReceiver(it) } }
        updateReceiver = null
        super.onDestroy()
    }

    @Composable
    override fun ScreenContent() {
        com.v2ray.ang.ui.checkupdate.UpdateAvailableDialog(updateViewModel) { url ->
            runCatching { UpdateInstaller.startDownload(this, url) }
                .onFailure { toastError(R.string.toast_failure) }
        }
        BackHandler { moveTaskToBack(false) }
        MainScreen(
            mainViewModel = mainViewModel,
            onAction = { action ->
                when (action) {
                    MainAction.ToggleService -> handleFabAction()
                    MainAction.TestCurrentServer -> handleLayoutTestClick()
                    MainAction.OpenVpnSecuritySettings -> openVpnSecuritySettings()
                    MainAction.RestartService -> LauncherManager.restartServiceOrStart(this, ::requestServiceStart)
                    MainAction.LocateSelectedServer -> mainViewModel.triggerLocateSelectedServer()
                    is MainAction.SelectServer -> setSelectServer(action.guid)
                    is MainAction.SelectCountry, is MainAction.SelectConnectionMode -> {
                        val previous = mainViewModel.uiState.value.selectedGuid
                        val running = mainViewModel.uiState.value.isRunning
                        mainViewModel.onAction(action)
                        if (running && previous != mainViewModel.uiState.value.selectedGuid) {
                            LauncherManager.restartService(this)
                        }
                    }
                    else -> mainViewModel.onAction(action)
                }
            },
            onNavigate = { route -> navigateTo(route) },
        )
    }

    private fun navigateTo(destination: MainDestination) {
        val intent = when (destination) {
            MainDestination.About -> Intent(this, AboutActivity::class.java)
            MainDestination.CheckUpdate, MainDestination.Settings -> Intent(this, CheckUpdateActivity::class.java)
            MainDestination.Promotion -> {
                Utils.openUri(
                    this,
                    "${Utils.decode(AppConfig.APP_PROMOTION_URL)}?t=${System.currentTimeMillis()}"
                )
                return
            }
            else -> return
        }
        startActivity(intent)
    }

    private fun handleFabAction() {
        if (mainViewModel.uiState.value.isRunning) {
            LauncherManager.stopService(this)
        } else {
            requestServiceStart()
        }
    }

    private fun requestServiceStart() {
        if (!SettingsManager.isVpnMode()) {
            startTunnel()
            return
        }
        val intent = VpnService.prepare(this)
        if (intent == null) startTunnel() else requestVpnPermission.launch(intent)
    }

    private fun handleLayoutTestClick() {
        if (mainViewModel.uiState.value.isRunning) {
            mainViewModel.testCurrentServerRealPing()
        }
    }

    private fun openVpnSecuritySettings() {
        runCatching { startActivity(Intent(android.provider.Settings.ACTION_VPN_SETTINGS)) }
            .onFailure { toastError(R.string.toast_failure) }
    }

    private fun startTunnel() {
        if (mainViewModel.uiState.value.selectedGuid.isNullOrEmpty()) {
            toast(R.string.title_file_chooser)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN
        ) {
            checkAndRequestPermission(PermissionType.ACCESS_LOCAL_NETWORK) {}
        }
        mainViewModel.onAction(MainAction.ConnectionRequested)
        LauncherManager.startService(this)
    }

    private fun setSelectServer(guid: String) {
        val selected = mainViewModel.uiState.value.selectedGuid
        if (guid != selected) {
            mainViewModel.updateSelectedGuid(guid)
            LauncherManager.restartService(this)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
