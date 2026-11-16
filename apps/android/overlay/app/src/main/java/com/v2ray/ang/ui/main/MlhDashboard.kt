package com.v2ray.ang.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.core.*
import java.util.Locale
import kotlinx.coroutines.launch

private val Ink = Color(0xFF041525)
private val Night = Color(0xFF06192C)
private val Panel = Color(0xFF0B2942)
private val PanelLight = Color(0xFF103852)
private val Cyan = Color(0xFF16D8DF)
private val Mint = Color(0xFF38F0B6)
private val Good = Color(0xFF58E56E)
private val Muted = Color(0xFF9EB6C8)
private val White = Color(0xFFF5FBFF)

private enum class DashboardTab { HOME, COUNTRIES, AI, PROTOCOLS, MORE }
private data class Snapshot(
    val connected: Boolean, val connecting: Boolean, val country: String, val code: String?,
    val ip: String, val ping: String, val quality: QualityAssessment?, val elapsed: String,
    val upload: String, val download: String, val sent: String, val received: String,
)

@Composable
fun MainScreen(mainViewModel: MainViewModel, onAction: (MainAction) -> Unit, onNavigate: (MainDestination) -> Unit) {
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()
    val now by mainViewModel.clock.collectAsStateWithLifecycle()
    val group by mainViewModel.serverGroupState(state.selectedGroupId).collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(DashboardTab.HOME) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val unavailable = stringResource(R.string.mlh_unavailable)
    val result = (state.status as? MainStatus.ConnectionTest)?.result?.takeIf { state.isRunning && it.delayMillis >= 0 }
    val quality = state.qualityAssessment ?: result?.let {
        SmartQualityEngine.assess(ProbeEvidence("active", 1, listOf(it.delayMillis), verifiedCountryCode = it.country))
    }
    val traffic = state.telemetry?.takeIf { state.isRunning }
    val fresh = traffic != null && now - traffic.sampledAt in 0..10_000
    val code = result?.country?.takeIf { Regex("[A-Za-z]{2}").matches(it) }?.uppercase(Locale.ROOT)
    val country = code?.let { Locale.Builder().setRegion(it).build().getDisplayCountry(Locale.getDefault()) }
        ?: result?.country?.takeIf { it.isNotBlank() } ?: stringResource(R.string.mlh_unknown)
    val snap = Snapshot(
        state.isRunning, !state.isRunning && now < state.connectingUntil, country, code,
        result?.ipAddress?.takeIf { it.isNotBlank() } ?: stringResource(R.string.mlh_unknown),
        result?.let { "${it.delayMillis} ms" } ?: unavailable, quality,
        traffic?.let { formatSessionTime((now - it.startedAt).coerceAtLeast(0)) } ?: "00:00:00",
        if (fresh) traffic?.sendRate?.let(::formatTraffic) ?: unavailable else unavailable,
        if (fresh) traffic?.receiveRate?.let(::formatTraffic) ?: unavailable else unavailable,
        traffic?.sent?.let { formatTraffic(it.toDouble()) } ?: unavailable,
        traffic?.received?.let { formatTraffic(it.toDouble()) } ?: unavailable,
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            scrimColor = Ink.copy(alpha = .72f),
            drawerContent = {
                NavigationDrawerSheet(drawerContainerColor = Night, drawerContentColor = White) {
                    Text("MolidoVPN AI", Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 24.dp), fontSize = 22.sp, fontWeight = FontWeight.Black)
                    listOf(
                        Triple(DashboardTab.HOME, "⌂", "داشبورد هوشمند"),
                        Triple(DashboardTab.COUNTRIES, "◎", "کشورها و سرورها"),
                        Triple(DashboardTab.AI, "✧", "مغز هوش مصنوعی"),
                        Triple(DashboardTab.PROTOCOLS, "◇", "پروتکل‌ها"),
                        Triple(DashboardTab.MORE, "☰", "تنظیمات و بیشتر"),
                    ).forEach { (destination, glyph, label) ->
                        NavigationDrawerItem(
                            label = { Text(label, fontWeight = if (tab == destination) FontWeight.Bold else FontWeight.Normal) },
                            selected = tab == destination,
                            onClick = { tab = destination; scope.launch { drawerState.close() } },
                            icon = { Text(glyph, color = if (tab == destination) Cyan else Muted, fontSize = 20.sp) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color(0xFF104B58),
                                selectedTextColor = Mint,
                                unselectedTextColor = Muted,
                            ),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("نسخه Premium • طراحی محلی و حریم خصوصی‌محور", Modifier.padding(horizontal = 22.dp), color = Muted, fontSize = 10.sp)
                }
            },
        ) {
        Scaffold(containerColor = Ink, contentColor = White) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                .background(Brush.verticalGradient(listOf(Ink, Night)))) {
                Header { scope.launch { drawerState.open() } }
                when (tab) {
                    DashboardTab.HOME -> PulseDashboard(snap, state, group.rows, onAction)
                    DashboardTab.COUNTRIES -> CountryDashboard(snap, state, group.rows, onAction)
                    DashboardTab.AI -> ControlDashboard(snap, state, onAction)
                    DashboardTab.PROTOCOLS -> ProtocolDashboard()
                    DashboardTab.MORE -> MlhSettingsDashboard(onAction, onNavigate)
                }
            }
        }
        }
    }
}

@Composable private fun MlhSettingsDashboard(action: (MainAction) -> Unit, navigate: (MainDestination) -> Unit) = ScrollPage {
    Text(stringResource(R.string.mlh_settings_title), Modifier.fillMaxWidth(), fontSize = 24.sp, fontWeight = FontWeight.Black)
    Text(stringResource(R.string.mlh_settings_summary), Modifier.fillMaxWidth(), color = Muted, fontSize = 12.sp)
    SettingsRow("↻", stringResource(R.string.mlh_app_update), stringResource(R.string.mlh_app_update_hint)) { navigate(MainDestination.CheckUpdate) }
    SettingsRow("⬡", stringResource(R.string.mlh_security_settings), stringResource(R.string.mlh_kill_switch_hint)) { action(MainAction.OpenVpnSecuritySettings) }
    SettingsRow("i", stringResource(R.string.mlh_about), stringResource(R.string.app_name)) { navigate(MainDestination.About) }
}

@Composable private fun SettingsRow(icon: String, title: String, detail: String, click: () -> Unit) = Surface(
    Modifier.fillMaxWidth().clickable(onClick = click), color = Panel, shape = RoundedCornerShape(20.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, White.copy(.07f)),
) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircleLabel(icon, Color(0xFF10495B), Cyan)
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(detail, color = Muted, fontSize = 10.sp, maxLines = 2) }
        Text("‹", color = Muted, fontSize = 24.sp)
    }
}

@Composable private fun Header(onMenu: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp)) {
        Box(Modifier.align(Alignment.CenterStart).size(42.dp).clip(CircleShape).background(Panel).border(1.dp, Cyan.copy(.55f), CircleShape).clickable(onClick = onMenu), contentAlignment = Alignment.Center) { Text("☰", color = Mint, fontSize = 20.sp) }
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Mint, Cyan))), contentAlignment = Alignment.Center) {
                Text("✓", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
            }
            Text(stringResource(R.string.app_name), fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
    }
}

@Composable private fun PulseDashboard(s: Snapshot, state: MainUiState, servers: List<ServerRowUiModel>, action: (MainAction) -> Unit) = ScrollPage {
    ConnectionPulse(s) { action(MainAction.ToggleService) }
    QualityPill(s.quality?.grade, ready = !s.connected && !s.connecting)
    ConnectionPreferences(s, state, servers, action)
    OutputIpCard(s, state, action)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Metric("↓", stringResource(R.string.mlh_receive), s.download, Modifier.weight(1f))
        Metric("↑", stringResource(R.string.mlh_send), s.upload, Modifier.weight(1f))
        Metric("⌁", stringResource(R.string.mlh_ping), s.ping, Modifier.weight(1f))
    }
}

@Composable private fun ConnectionPulse(s: Snapshot, click: () -> Unit) {
    val color = if (s.connected) Mint else if (s.connecting) Cyan else Color(0xFF2B6D86)
    Box(Modifier.fillMaxWidth().height(224.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(205.dp)) {
            drawCircle(color.copy(.08f), size.minDimension * .49f)
            drawCircle(color.copy(.18f), size.minDimension * .43f)
            drawArc(Brush.sweepGradient(listOf(color.copy(.1f), color, Cyan, color.copy(.1f))), -90f,
                if (s.connected) 330f else 250f, false, Offset(12.dp.toPx(), 12.dp.toPx()),
                Size(size.width - 24.dp.toPx(), size.height - 24.dp.toPx()), style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
            repeat(30) { i ->
                val a = Math.toRadians(i * 12.0 - 90); val r = size.minDimension * .49f
                drawCircle(color.copy(if (i % 3 == 0) .75f else .28f), 1.5.dp.toPx(),
                    Offset(center.x + kotlin.math.cos(a).toFloat() * r, center.y + kotlin.math.sin(a).toFloat() * r))
            }
        }
        Column(Modifier.size(164.dp).clip(CircleShape).clickable(onClick = click)
            .background(Brush.radialGradient(listOf(Color(0xFF0B5260), Color(0xFF063145), Night)))
            .border(1.dp, color.copy(.6f), CircleShape), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(if (s.connected) "✓" else "⏻", color = color, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(if (s.connecting) R.string.mlh_connecting else if (s.connected) R.string.mlh_connected else R.string.mlh_connect), fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(s.elapsed, color = Muted, fontSize = 17.sp)
        }
    }
}

@Composable private fun QualityPill(grade: QualityGrade?, ready: Boolean = false) {
    val c = if (ready) Mint else qualityColor(grade)
    Surface(color = c.copy(.14f), shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, c.copy(.45f))) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (ready) "ϟ" else "▮▮▮", color = c, fontSize = 13.sp)
            Text(if (ready) stringResource(R.string.mlh_ready) else stringResource(qualityResource(grade)), color = c, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun ExitCard(s: Snapshot) = GlassCard {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CountryBadge(s.code)
            Column { Text(stringResource(R.string.mlh_exit), color = Muted, fontSize = 12.sp); Text(s.country, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(s.ip, color = Muted, fontSize = 11.sp) }
        }
        Column(horizontalAlignment = Alignment.End) { Text(if (s.connected) "اتصال فعال" else "بررسی نشده", color = if (s.connected) Mint else Muted, fontSize = 11.sp); Text("‹", fontSize = 28.sp) }
    }
}

@Composable private fun OutputIpCard(s: Snapshot, state: MainUiState, action: (MainAction) -> Unit) = Surface(
    Modifier.fillMaxWidth().clickable(enabled = state.isRunning) { action(MainAction.TestCurrentServer) },
    color = Panel, shape = RoundedCornerShape(20.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, Cyan.copy(.22f)),
) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircleLabel("IP", Color(0xFF10495B), Mint)
        Column(Modifier.weight(1f)) { Text(stringResource(R.string.mlh_exit_ip), color = Muted, fontSize = 11.sp); Text(s.ip, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1) }
        Box(Modifier.size(48.dp).clip(CircleShape).background(Cyan.copy(.12f)).border(1.dp, Cyan.copy(.65f), CircleShape), contentAlignment = Alignment.Center) { Text("↻", color = Cyan, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun ConnectionPreferences(s: Snapshot, state: MainUiState, servers: List<ServerRowUiModel>, action: (MainAction) -> Unit) {
    var countryMenu by rememberSaveable { mutableStateOf(false) }
    var modeMenu by rememberSaveable { mutableStateOf(false) }
    val countries = remember(servers) { servers.mapNotNull { row -> SmartSelectionStore.verifiedCountry(row.guid) }.distinct().sorted() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth()) {
            PreferenceButton("✦", stringResource(R.string.mlh_exit), state.preferredCountry?.let(::countryName) ?: stringResource(R.string.mlh_country_auto), detail = "${s.country}  •  ${s.ip}") { countryMenu = true }
            DropdownMenu(countryMenu, { countryMenu = false }) {
                DropdownMenuItem({ Text(stringResource(R.string.mlh_country_auto)) }, onClick = { countryMenu = false; action(MainAction.SelectCountry(null)) })
                countries.forEach { code -> DropdownMenuItem({ Text(countryName(code)) }, onClick = { countryMenu = false; action(MainAction.SelectCountry(code)) }) }
            }
        }
        Box(Modifier.fillMaxWidth()) {
            val modeLabel = when (state.connectionMode) { ConnectionMode.AUTO -> stringResource(R.string.mlh_engine_auto); ConnectionMode.FAST -> stringResource(R.string.mlh_engine_fast); ConnectionMode.STABLE -> stringResource(R.string.mlh_engine_stable); ConnectionMode.COMPATIBLE -> stringResource(R.string.mlh_engine_compatible) }
            PreferenceButton("⌁", stringResource(R.string.mlh_connection_mode), modeLabel) { modeMenu = true }
            DropdownMenu(modeMenu, { modeMenu = false }) {
                DropdownMenuItem({ Text(stringResource(R.string.mlh_engine_auto)) }, onClick = { modeMenu = false; action(MainAction.SelectConnectionMode(ConnectionMode.AUTO)) })
                DropdownMenuItem({ Text(stringResource(R.string.mlh_engine_fast)) }, onClick = { modeMenu = false; action(MainAction.SelectConnectionMode(ConnectionMode.FAST)) })
                DropdownMenuItem({ Text(stringResource(R.string.mlh_engine_stable)) }, onClick = { modeMenu = false; action(MainAction.SelectConnectionMode(ConnectionMode.STABLE)) })
                DropdownMenuItem({ Text(stringResource(R.string.mlh_engine_compatible)) }, onClick = { modeMenu = false; action(MainAction.SelectConnectionMode(ConnectionMode.COMPATIBLE)) })
            }
        }
    }
}

@Composable private fun PreferenceButton(icon: String, title: String, value: String, detail: String? = null, click: () -> Unit) = Surface(Modifier.fillMaxWidth().clickable(onClick = click), color = Panel, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Cyan.copy(.22f))) {
    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Mint.copy(.34f), Cyan.copy(.10f)))).border(1.dp, Mint.copy(.7f), CircleShape), contentAlignment = Alignment.Center) { Text(icon, color = Mint, fontSize = 21.sp, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Muted, fontSize = 10.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            if (detail != null) Text(detail, color = Muted, fontSize = 10.sp, maxLines = 1)
        }
        Text("⌄", color = Muted)
    }
}

private fun countryName(code: String): String = Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.getDefault()).ifBlank { code }

@Composable private fun CountryDashboard(s: Snapshot, state: MainUiState, servers: List<ServerRowUiModel>, action: (MainAction) -> Unit) {
    val ranked = remember(servers) { servers.filter { it.testDelayMillis > 0 }.sortedBy { it.testDelayMillis }.take(3) }
    ScrollPage {
        WorldMap(s.code)
        ExitCard(s)
        GlassCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("اتصال هوشمند", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("انتخاب خودکار بهترین سرویس اندازه‌گیری‌شده", color = Muted, fontSize = 12.sp) }
                Text(s.quality?.score?.toString() ?: "—", color = Good, fontSize = 29.sp, fontWeight = FontWeight.Black)
            }
        }
        Text("بهترین سرویس‌های پیشنهادی", Modifier.fillMaxWidth(), fontWeight = FontWeight.Black, fontSize = 19.sp)
        if (ranked.isEmpty()) GlassCard {
            Text("هنوز نتیجه‌ای اندازه‌گیری نشده است", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Text("آزمایش هوشمند را اجرا کنید تا بهترین سرویس‌ها رتبه‌بندی شوند.", Modifier.fillMaxWidth(), color = Muted, textAlign = TextAlign.Center, fontSize = 12.sp)
        } else ranked.forEachIndexed { i, server -> ServerRow(i + 1, server, server.guid == state.selectedGuid) { action(MainAction.SelectServer(server.guid)) } }
        SmartButton(state, action)
    }
}

@Composable private fun WorldMap(code: String?) = GlassCard(PaddingValues(0.dp)) {
    Box(Modifier.fillMaxWidth().height(185.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val lands = listOf(
                listOf(.08f to .36f, .18f to .24f, .31f to .30f, .27f to .49f, .17f to .54f),
                listOf(.28f to .58f, .37f to .53f, .40f to .78f, .34f to .90f),
                listOf(.43f to .28f, .55f to .22f, .70f to .31f, .75f to .48f, .58f to .52f, .48f to .42f),
                listOf(.49f to .51f, .58f to .55f, .55f to .82f, .47f to .74f),
                listOf(.76f to .68f, .88f to .69f, .92f to .82f, .80f to .86f))
            lands.forEach { points -> val p = Path(); points.forEachIndexed { i, v -> if (i == 0) p.moveTo(size.width*v.first,size.height*v.second) else p.lineTo(size.width*v.first,size.height*v.second) }; p.close(); drawPath(p, Color(0xFF12607A)) }
            val t = when (code) { "DE","NL","FR","GB" -> Offset(size.width*.53f,size.height*.35f); "US","CA" -> Offset(size.width*.21f,size.height*.38f); "JP","SG" -> Offset(size.width*.82f,size.height*.50f); else -> Offset(size.width*.61f,size.height*.46f) }
            drawCircle(Mint.copy(.18f), 20.dp.toPx(), t); drawCircle(Mint, 7.dp.toPx(), t); drawCircle(White, 2.5.dp.toPx(), t)
        }
        Text("نمایش تقریبی منطقه خروجی", Modifier.align(Alignment.BottomCenter).padding(12.dp), color = Muted, fontSize = 11.sp)
    }
}

@Composable private fun ServerRow(rank: Int, server: ServerRowUiModel, selected: Boolean, click: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = click), color = if (selected) Color(0xFF104B58) else Panel, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Cyan else White.copy(.07f))) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircleLabel(rank.toString(), if (rank == 1) Mint else PanelLight, if (rank == 1) Ink else White)
            Column(Modifier.weight(1f)) { Text("سرویس هوشمند $rank", fontWeight = FontWeight.Bold); Text(if (selected) "انتخاب فعلی" else "آماده اتصال", color = if (selected) Mint else Muted, fontSize = 11.sp) }
            Column(horizontalAlignment = Alignment.End) { Text("${server.testDelayMillis} ms", fontWeight = FontWeight.Bold); Text("امتیاز ${latencyScore(server.testDelayMillis)}", color = Good, fontSize = 11.sp) }
        }
    }
}

@Composable private fun ProtocolDashboard() = ScrollPage {
    Text("پروتکل‌های اتصال", Modifier.fillMaxWidth(), fontSize = 23.sp, fontWeight = FontWeight.Black)
    Text("فهرست قابلیت‌های مستندشده در نسخه فعلی Android؛ نمایش یک پروتکل به معنی تأیید اتصال واقعی آن نیست.", Modifier.fillMaxWidth(), color = Muted, fontSize = 12.sp)
    val requested = listOf(
        MolidoProtocol.VLESS, MolidoProtocol.VMESS, MolidoProtocol.TROJAN,
        MolidoProtocol.SHADOWSOCKS, MolidoProtocol.HYSTERIA2, MolidoProtocol.WIREGUARD,
    )
    requested.forEach { protocol ->
        val capability = MolidoProtocolRegistry.find(protocol)
        GlassCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(protocol.label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (capability?.evidence == ProtocolEvidence.SOURCE_DOCUMENTED) "پشتیبانی در مستندات هسته فعلی" else "نیازمند بررسی",
                        color = Mint, fontSize = 11.sp
                    )
                }
                Surface(color = Color(0xFF553F16), shape = RoundedCornerShape(50)) {
                    Text("تست دستگاه لازم است", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color(0xFFFFD27A), fontSize = 10.sp)
                }
            }
        }
    }
    GlassCard {
        Text("انتخاب پروتکل از طریق پروفایل", fontWeight = FontWeight.Bold)
        Text("پروتکل هر اتصال از کانفیگ یا اشتراک واردشده تشخیص داده می‌شود. این صفحه سرور جدید نمی‌سازد و نوع پروفایل را به‌صورت دستی تغییر نمی‌دهد.", color = Muted, fontSize = 12.sp)
        Text("برای تأیید نهایی هر پروتکل باید با کانفیگ معتبر روی گوشی واقعی، ترافیک، DNS، IPv4/IPv6، اتصال مجدد و جابه‌جایی شبکه آزمایش شود.", color = Muted, fontSize = 12.sp)
    }
}

@Composable private fun ControlDashboard(s: Snapshot, state: MainUiState, action: (MainAction) -> Unit) = ScrollPage {
    Text("مغز هوش مصنوعی محلی", Modifier.fillMaxWidth(), fontSize = 22.sp, fontWeight = FontWeight.Black)
    Text("تحلیل بر پایه اندازه‌گیری‌های واقعی همین دستگاه؛ بدون نیاز به سرور هوش مصنوعی ابری.", Modifier.fillMaxWidth(), color = Muted, fontSize = 12.sp)

    GlassCard(brush = Brush.linearGradient(listOf(Color(0xFF0A6A65), Color(0xFF0A3E50)))) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(stringResource(if (s.connected) R.string.mlh_connected else R.string.mlh_disconnected), fontSize = 24.sp, fontWeight = FontWeight.Black); Text(s.elapsed, color = White.copy(.8f)) }
            QualityPill(s.quality?.grade)
            Box(Modifier.size(56.dp).clip(CircleShape).background(Mint.copy(.16f)).border(2.dp, Mint, CircleShape).clickable { action(MainAction.ToggleService) }, contentAlignment = Alignment.Center) { Text("⏻", color = Mint, fontSize = 29.sp) }
        }
    }
    QualityChart(s.quality)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ControlTile("⌁", "وضعیت شبکه", if (s.connected) "متصل" else "قطع", s.ping, Modifier.weight(1f))
        ControlTile("⬡", "مسیر DNS", "نیازمند آزمون", "آزمون نشت لازم است", Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ControlTile("★", "سرویس مورد علاقه", if (state.isFavorite) "ذخیره شده" else "انتخاب نشده", s.country, Modifier.weight(1f))
        ControlTile("◷", "خروجی فعلی", s.country, s.ip, Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Metric("↑", stringResource(R.string.mlh_sent_total), s.sent, Modifier.weight(1f)); Metric("↓", stringResource(R.string.mlh_received_total), s.received, Modifier.weight(1f))
    }
    SmartButton(state, action)
    OutlinedButton({ action(MainAction.OpenVpnSecuritySettings) }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Cyan.copy(.55f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan)) { Text(stringResource(R.string.mlh_security_settings)) }
}

@Composable private fun QualityChart(q: QualityAssessment?) = GlassCard {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column { Text("کیفیت اتصال", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(stringResource(qualityResource(q?.grade)), color = qualityColor(q?.grade)) }
        Text(q?.score?.toString() ?: "—", color = qualityColor(q?.grade), fontSize = 36.sp, fontWeight = FontWeight.Black)
    }
    Canvas(Modifier.fillMaxWidth().height(68.dp)) {
        val values = listOf(.7f,.55f,.66f,.38f,.48f,.24f,.42f,.30f,.36f,.20f); val p = Path()
        values.forEachIndexed { i,v -> val x=size.width*i/values.lastIndex; val y=size.height*v; if(i==0)p.moveTo(x,y) else p.lineTo(x,y) }
        drawPath(p, qualityColor(q?.grade), style=Stroke(3.dp.toPx(), cap=StrokeCap.Round))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        QualityDetail(stringResource(R.string.mlh_median_ping), q?.medianLatencyMillis?.let { "$it ms" } ?: "—")
        QualityDetail(stringResource(R.string.mlh_jitter), q?.jitterMillis?.let { "$it ms" } ?: "—")
        QualityDetail(stringResource(R.string.mlh_packet_loss), q?.let { "${it.failurePercent}%" } ?: "—")
        QualityDetail(stringResource(R.string.mlh_speed), q?.throughputBytesPerSecond?.let(::formatBitrate) ?: "—")
    }
}

@Composable private fun QualityDetail(label: String, value: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, color = Muted, fontSize = 9.sp, maxLines = 1)
    Text(value, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
}

@Composable private fun SmartButton(state: MainUiState, action: (MainAction) -> Unit) = Button(
    { action(if (state.isRunning) MainAction.TestCurrentServer else MainAction.TestRealAllServers) }, enabled = !state.isTesting,
    modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(19.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink, disabledContainerColor = Color(0xFF18535A))) {
    Text(
        if (state.isTesting) stringResource(R.string.mlh_smart_testing)
        else if (state.isRunning) "⚡  ${stringResource(R.string.mlh_quality_test)}"
        else "⚡  ${stringResource(R.string.mlh_smart_test)}",
        fontWeight = FontWeight.Black,
        fontSize = 17.sp,
    )
}

@Composable private fun Metric(icon: String, label: String, value: String, modifier: Modifier) = Surface(modifier, color = Panel, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, White.copy(.06f))) {
    Column(Modifier.padding(vertical = 12.dp, horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(icon, color = Cyan); Text(label, color = Muted, fontSize = 10.sp); Text(value, fontWeight = FontWeight.Bold, maxLines = 1) }
}

@Composable private fun ControlTile(icon:String,title:String,status:String,detail:String,modifier:Modifier)=Surface(modifier,color=Panel,shape=RoundedCornerShape(20.dp),border=androidx.compose.foundation.BorderStroke(1.dp,White.copy(.07f))){
    Column(Modifier.padding(14.dp), verticalArrangement=Arrangement.spacedBy(5.dp)){ Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(title,fontWeight=FontWeight.Bold,fontSize=12.sp);Text(icon,color=Mint,fontSize=19.sp)};Text(status,color=Good,fontWeight=FontWeight.Bold);Text(detail,color=Muted,fontSize=10.sp,maxLines=1)}
}

@Composable private fun CountryBadge(code:String?){ val flag=when(code){"DE"->"🇩🇪";"NL"->"🇳🇱";"FR"->"🇫🇷";"US"->"🇺🇸";"TR"->"🇹🇷";"GB"->"🇬🇧";"JP"->"🇯🇵";"SG"->"🇸🇬";else->"◎"};Box(Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF16425A)),contentAlignment=Alignment.Center){Text(flag,fontSize=if(code==null)22.sp else 27.sp,color=Cyan)} }
@Composable private fun CircleLabel(text:String,bg:Color,fg:Color=White)=Box(Modifier.size(30.dp).clip(CircleShape).background(bg),contentAlignment=Alignment.Center){Text(text,color=fg,fontWeight=FontWeight.Bold)}

@Composable private fun GlassCard(padding:PaddingValues=PaddingValues(16.dp),brush:Brush?=null,content:@Composable ColumnScope.()->Unit){
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).then(if(brush==null)Modifier.background(Panel) else Modifier.background(brush)).border(1.dp,White.copy(.08f),RoundedCornerShape(22.dp)).padding(padding),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
}
@Composable private fun ScrollPage(content:@Composable ColumnScope.()->Unit)=Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp,vertical=4.dp).padding(bottom=18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp),content=content)

@Composable private fun BottomNav(selected:DashboardTab,select:(DashboardTab)->Unit)=Surface(color=Color(0xFF061827),shadowElevation=16.dp){
    NavigationBar(containerColor=Color.Transparent,tonalElevation=0.dp,modifier=Modifier.height(76.dp)){
        NavItem(DashboardTab.HOME,"⌂","خانه",selected,select)
        NavItem(DashboardTab.COUNTRIES,"◎","سرورها",selected,select)
        NavItem(DashboardTab.AI,"✧","هوش AI",selected,select)
        NavItem(DashboardTab.PROTOCOLS,"◇","پروتکل",selected,select)
        NavItem(DashboardTab.MORE,"☰","بیشتر",selected,select)
    }
}
@Composable private fun RowScope.NavItem(tab:DashboardTab,icon:String,label:String,selected:DashboardTab,select:(DashboardTab)->Unit)=NavigationBarItem(selected==tab,{select(tab)},{Text(icon,fontSize=21.sp)},label={Text(label)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Cyan,selectedTextColor=Cyan,indicatorColor=Color(0xFF0C4A57),unselectedIconColor=Muted,unselectedTextColor=Muted))

private fun qualityResource(g:QualityGrade?)=when(g){QualityGrade.EXCELLENT->R.string.mlh_quality_excellent;QualityGrade.GOOD->R.string.mlh_quality_good;QualityGrade.FAIR->R.string.mlh_quality_fair;QualityGrade.POOR->R.string.mlh_quality_poor;else->R.string.mlh_unavailable}
private fun qualityColor(g:QualityGrade?)=when(g){QualityGrade.EXCELLENT->Good;QualityGrade.GOOD->Mint;QualityGrade.FAIR->Color(0xFFFFC857);QualityGrade.POOR->Color(0xFFFF6B6B);else->Muted}
private fun reasonResource(r:String?)=when(r){"no_current_server"->R.string.mlh_reason_first_choice;"current_unhealthy"->R.string.mlh_reason_unhealthy;"material_quality_gain"->R.string.mlh_reason_better;"keep_stable_connection"->R.string.mlh_reason_stable;else->R.string.mlh_reason_unavailable}
internal fun latencyScore(ms:Long)=when{ms<=0->0;ms<=80->96;ms<=130->88;ms<=200->76;ms<=350->58;else->35}
internal fun formatSessionTime(milliseconds:Long):String{val s=milliseconds.coerceAtLeast(0)/1000;return String.format(Locale.ROOT,"%02d:%02d:%02d",s/3600,s/60%60,s%60)}
internal fun formatTraffic(bytes:Double):String=when{bytes>=1_073_741_824->String.format(Locale.ROOT,"%.1f GiB",bytes/1_073_741_824);bytes>=1_048_576->String.format(Locale.ROOT,"%.1f MiB",bytes/1_048_576);bytes>=1024->String.format(Locale.ROOT,"%.1f KiB",bytes/1024);else->String.format(Locale.ROOT,"%.0f B",bytes.coerceAtLeast(0.0))}
internal fun formatBitrate(bytesPerSecond:Long):String=when{bytesPerSecond>=1_000_000->String.format(Locale.ROOT,"%.1f Mbps",bytesPerSecond*8/1_000_000.0);bytesPerSecond>=1_000->String.format(Locale.ROOT,"%.0f Kbps",bytesPerSecond*8/1_000.0);else->"${(bytesPerSecond*8).coerceAtLeast(0)} bps"}
