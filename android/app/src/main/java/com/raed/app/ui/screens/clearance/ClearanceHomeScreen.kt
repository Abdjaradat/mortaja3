package com.raed.app.ui.screens.clearance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.raed.app.ui.components.UnityBannerCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.ClearanceAgentDto
import com.raed.app.data.api.models.ClearanceRequestDto
import com.raed.app.data.local.SessionDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private val Gold = Color(0xFFC9A961)
private fun Int.toJod() = "%,d د.أ".format(this)

@HiltViewModel
class ClearanceHomeViewModel @Inject constructor(
    private val api: RaedApi,
    private val sessionDataStore: SessionDataStore,
) : ViewModel() {
    var currentUserId by mutableStateOf<String?>(null)
    var openRequests by mutableStateOf<List<ClearanceRequestDto>>(emptyList())
    var myRequests by mutableStateOf<List<ClearanceRequestDto>>(emptyList())
    var myAgentProfile by mutableStateOf<ClearanceAgentDto?>(null)
    var agentCheckDone by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)

    init { refresh() }

    fun refresh(isManual: Boolean = false) {
        viewModelScope.launch {
            if (isManual) isRefreshing = true
            else if (openRequests.isEmpty()) isLoading = true
            try {
                if (currentUserId == null) currentUserId = sessionDataStore.userId.firstOrNull()
                val agentDef = async { api.getMyClearanceAgent() }
                val openDef  = async { api.getClearanceRequests(mine = false) }
                val mineDef  = async { api.getClearanceRequests(mine = true) }
                val agentResp = agentDef.await()
                myAgentProfile = if (agentResp.isSuccessful) agentResp.body() else null
                agentCheckDone = true
                openDef.await().body()?.let { openRequests = it }
                mineDef.await().body()?.let { myRequests = it }
            } catch (_: Exception) {
                agentCheckDone = true
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearanceHomeContent(
    onPostRequest: () -> Unit,
    onRequestClick: (id: String) -> Unit,
    onRegisterAgent: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClearanceHomeViewModel = hiltViewModel(),
) {
    var subTab by remember { mutableIntStateOf(0) }
    var tick by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) { delay(1000L); tick = System.currentTimeMillis() }
    }

    PullToRefreshBox(
        isRefreshing = viewModel.isRefreshing,
        onRefresh = { viewModel.refresh(isManual = true) },
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = subTab) {
                Tab(selected = subTab == 0, onClick = { subTab = 0 },
                    text = { Text("أنا عميل", style = MaterialTheme.typography.bodySmall) })
                Tab(selected = subTab == 1, onClick = { subTab = 1 },
                    text = { Text("أنا مخلص", style = MaterialTheme.typography.bodySmall) })
            }
            when (subTab) {
                0 -> CustomerTab(
                    viewModel = viewModel,
                    tick = tick,
                    onPostRequest = onPostRequest,
                    onRequestClick = onRequestClick,
                    modifier = Modifier.weight(1f),
                )
                1 -> AgentTab(
                    viewModel = viewModel,
                    tick = tick,
                    onRegisterAgent = onRegisterAgent,
                    onRequestClick = onRequestClick,
                    modifier = Modifier.weight(1f),
                )
            }
            UnityBannerCard()
        }
    }
}

// ─── Customer tab ─────────────────────────────────────────────────────────────

@Composable
private fun CustomerTab(
    viewModel: ClearanceHomeViewModel,
    tick: Long,
    onPostRequest: () -> Unit,
    onRequestClick: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var infoExpanded by remember { mutableStateOf(false) }

    if (viewModel.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Info card
        item {
            CollapsibleInfoCard(
                title = "كيف تستخدم سوق التخليص؟ 💡",
                expanded = infoExpanded,
                onToggle = { infoExpanded = !infoExpanded },
                content = "١. انشر طلبك مجاناً — اذكر نوع البضاعة والموقع\n" +
                        "٢. انتظر 4 ساعات — المخلصون يقدمون عروضهم بأسعار تنافسية\n" +
                        "٣. اختر الأنسب — شوف الأسعار والتقييمات واختر\n" +
                        "٤. تواصل مباشرة — احصل على بيانات المخلص وأكمل معه\n\n" +
                        "✅ النشر مجاني تماماً\n" +
                        "✅ أسعار شفافة ومتنافسة\n" +
                        "✅ متاح في بحرة عمان والزرقاء فقط",
            )
        }

        // Post button
        item {
            Button(
                onClick = onPostRequest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
            ) {
                Text("انشر طلبك مجاناً 🎁", fontWeight = FontWeight.Bold)
            }
        }

        // My requests
        if (viewModel.myRequests.isNotEmpty()) {
            item {
                Text(
                    "طلباتي",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(viewModel.myRequests, key = { "mine-${it.id}" }) { request ->
                ClearanceRequestCard(
                    request = request,
                    tick = tick,
                    showBadge = true,
                    onClick = { onRequestClick(request.id) },
                )
            }
        }

        // All open requests
        item {
            Text(
                "كل الطلبات المفتوحة",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (viewModel.openRequests.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("🏛️", fontSize = 40.sp)
                        Text(
                            "لا توجد طلبات مفتوحة حالياً",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(viewModel.openRequests, key = { "open-${it.id}" }) { request ->
                ClearanceRequestCard(
                    request = request,
                    tick = tick,
                    showBadge = false,
                    onClick = { onRequestClick(request.id) },
                )
            }
        }
    }
}

// ─── Agent tab ────────────────────────────────────────────────────────────────

@Composable
private fun AgentTab(
    viewModel: ClearanceHomeViewModel,
    tick: Long,
    onRegisterAgent: () -> Unit,
    onRequestClick: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var infoExpanded by remember { mutableStateOf(false) }

    if (viewModel.isLoading || !viewModel.agentCheckDone) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val agent = viewModel.myAgentProfile

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CollapsibleInfoCard(
                title = "كيف تكسب كمخلص؟ 💡",
                expanded = infoExpanded,
                onToggle = { infoExpanded = !infoExpanded },
                content = "١. سجّل ملفك المهني — أضف خبرتك وتخصصك (50 🪙)\n" +
                        "٢. شوف الطلبات الجديدة — اطلع على طلبات بمنطقتك\n" +
                        "٣. قدّم عرضك — حدد سعرك وأضف ملاحظات (10 🪙 لكل عرض)\n" +
                        "٤. اكسب العميل — لو اختارك العميل تُخصم 30 🪙 وتحصل على بيانات التواصل\n" +
                        "٥. ابنِ سمعتك — التقييمات تزيد فرصك بالصفقات القادمة\n\n" +
                        "💡 العروض مخفية عن المخلصين الآخرين — كل عرض سري",
            )
        }

        if (agent == null) {
            // Not registered yet
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("🏛️", fontSize = 40.sp)
                        Text(
                            "سجّل كمخلص جمركي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "أضف ملفك المهني وابدأ بتلقي الطلبات",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Button(
                            onClick = onRegisterAgent,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        ) {
                            Text("سجّل الآن — 50 🪙", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Registered agent profile card
            item { AgentProfileCard(agent = agent) }

            // Open requests feed
            item {
                Text(
                    "الطلبات المتاحة",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (viewModel.openRequests.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("📭", fontSize = 40.sp)
                            Text(
                                "لا توجد طلبات مفتوحة حالياً",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(viewModel.openRequests, key = { it.id }) { request ->
                    ClearanceRequestCard(
                        request = request,
                        tick = tick,
                        showBadge = false,
                        actionLabel = "قدّم عرضك",
                        onClick = { onRequestClick(request.id) },
                    )
                }
            }
        }
    }
}

// ─── Shared composables ───────────────────────────────────────────────────────

@Composable
internal fun CollapsibleInfoCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
internal fun ClearanceRequestCard(
    request: ClearanceRequestDto,
    tick: Long,
    showBadge: Boolean,
    actionLabel: String = "عرض التفاصيل",
    onClick: () -> Unit,
) {
    val remaining = (request.expiresAtMillis - tick).coerceAtLeast(0L)
    val isExpired = remaining == 0L
    val hours = remaining / 3_600_000L
    val minutes = (remaining % 3_600_000L) / 60_000L

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                        Text(
                            request.serviceTypeLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text("📍 ${request.locationLabel}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (showBadge && request.status == "CLOSED") {
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.small) {
                        Text("مغلق", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                } else if (!isExpired) {
                    Text(
                        "⏱ %02dس %02dد".format(hours, minutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hours < 3) Color(0xFFBF360C) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                request.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )

            request.budgetMax?.let { budget ->
                Text("الميزانية: حتى ${budget.toJod()}",
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${request.offerCount} عرض مقدّم",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!isExpired && request.status == "OPEN") {
                    TextButton(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(actionLabel, color = Gold, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentProfileCard(agent: ClearanceAgentDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Gold.copy(alpha = 0.15f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("👤", fontSize = 24.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(agent.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (agent.isVerifiedBadge) {
                            Surface(color = Color(0xFF1B5E20).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                Text("موثق ✓", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall, color = Color(0xFF1B5E20),
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("📍 ${agent.locationLabel}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("⭐ ${agent.ratingStars} (${agent.ratingCount})", style = MaterialTheme.typography.bodySmall)
                Text("✅ ${agent.totalDeals} صفقة", style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                agent.specializationLabels.forEach { spec ->
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                        Text(spec, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
    }
}
