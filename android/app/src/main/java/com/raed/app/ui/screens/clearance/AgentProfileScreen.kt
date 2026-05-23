package com.raed.app.ui.screens.clearance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.ClearanceAgentDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private val Gold = Color(0xFFC9A961)

@HiltViewModel
class AgentProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: RaedApi,
) : ViewModel() {
    val agentId: String = checkNotNull(savedStateHandle["agentId"])
    var agent by mutableStateOf<ClearanceAgentDto?>(null)
    var isLoading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            try {
                val resp = api.getClearanceAgent(agentId)
                if (resp.isSuccessful) agent = resp.body()
                else error = "المخلص غير موجود"
            } catch (e: Exception) {
                error = "تعذّر تحميل الملف"
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentProfileScreen(
    onBack: () -> Unit,
    viewModel: AgentProfileViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ملف المخلص") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        when {
            viewModel.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            viewModel.agent == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(viewModel.error ?: "المخلص غير موجود", textAlign = TextAlign.Center)
            }
            else -> AgentProfileContent(agent = viewModel.agent!!, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun AgentProfileContent(agent: ClearanceAgentDto, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // Hero card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Avatar
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(36.dp),
                    color = Gold.copy(alpha = 0.15f),
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("👤", fontSize = 36.sp) }
                }

                // Name + badge
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(agent.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                    if (agent.isVerifiedBadge) {
                        Surface(color = Color(0xFF1B5E20).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                            Text("مخلص موثق ✓", Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium, color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("📍 ${agent.locationLabel}", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Stats row
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    StatItem(label = "التقييم", value = "⭐ ${agent.ratingStars}")
                    VerticalDivider(modifier = Modifier.height(40.dp))
                    StatItem(label = "التقييمات", value = "${agent.ratingCount}")
                    VerticalDivider(modifier = Modifier.height(40.dp))
                    StatItem(label = "الصفقات", value = "✅ ${agent.totalDeals}")
                }
            }
        }

        // Specializations
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("التخصصات", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    agent.specializationLabels.forEach { spec ->
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                            Text(spec, Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }
                if (agent.yearsExperience > 0) {
                    Text("${agent.yearsExperience} سنوات خبرة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Bio
        val bio = agent.bio
        if (!bio.isNullOrBlank()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("نبذة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(bio, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
