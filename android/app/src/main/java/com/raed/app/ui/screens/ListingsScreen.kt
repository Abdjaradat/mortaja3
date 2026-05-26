package com.raed.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DriveEta
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Verified
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.ListingDto
import com.raed.app.data.mock.GOVERNORATES
import com.raed.app.data.mock.toJod
import com.raed.app.ui.components.AdEarnCard
import com.raed.app.ui.components.UnityBannerAd
import com.raed.app.utils.toTimeAgo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private val Gold = Color(0xFFC9A961)
private val ExemptionBg = Color(0xFFFFF8E7)

@HiltViewModel
class ListingsViewModel @Inject constructor(
    private val api: RaedApi,
) : ViewModel() {

    var categoryFilter by mutableStateOf<String?>(null)
    var governorateFilter by mutableStateOf<String?>(null)
    var searchQuery by mutableStateOf("")

    var listings by mutableStateOf<List<ListingDto>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun setCategory(cat: String?) {
        if (categoryFilter == cat && listings.isNotEmpty()) return
        categoryFilter = cat
        load()
    }

    fun watchAd() {
        viewModelScope.launch { runCatching { api.watchAd() } }
    }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            error = null
            runCatching {
                api.getListings(
                    governorate = governorateFilter,
                    category = categoryFilter,
                    limit = 50,
                )
            }
                .onSuccess { r ->
                    if (r.isSuccessful) listings = r.body()!!.listings
                    else error = "ÙØ´Ù„ ØªØ­Ù…ÙŠÙ„ Ø§Ù„Ø¥Ø¹Ù„Ø§Ù†Ø§Øª"
                }
                .onFailure { error = "ØªØ­Ù‚Ù‚ Ù…Ù† Ø§ØªØµØ§Ù„Ùƒ Ø¨Ø§Ù„Ø¥Ù†ØªØ±Ù†Øª" }
            isLoading = false
        }
    }
}

@Composable
fun ListingsContent(
    category: String,
    onListingClick: (id: String) -> Unit,
    onCalcClick: (price: Int) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    viewModel: ListingsViewModel = hiltViewModel(key = "listings_$category"),
) {
    LaunchedEffect(category) { viewModel.setCategory(category) }

    val feed = viewModel.listings
        .filter { viewModel.searchQuery.isBlank() || it.makeModel.contains(viewModel.searchQuery) || it.governorate.contains(viewModel.searchQuery) || (it.notes?.contains(viewModel.searchQuery) == true) }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            placeholder = { Text("Ø§Ø¨Ø­Ø«...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GOVERNORATES.take(5).forEach { gov ->
                FilterChip(
                    selected = viewModel.governorateFilter == gov,
                    onClick = {
                        viewModel.governorateFilter = if (viewModel.governorateFilter == gov) null else gov
                        viewModel.load()
                    },
                    label = { Text(gov, style = MaterialTheme.typography.bodySmall) },
                )
            }
        }

        when {
            viewModel.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            viewModel.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("âš ï¸", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(viewModel.error!!, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Outlined.Refresh, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Ø¥Ø¹Ø§Ø¯Ø© Ø§Ù„Ù…Ø­Ø§ÙˆÙ„Ø©")
                    }
                }
            }
            feed.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("ðŸ“­", fontSize = 48.sp)
                        Text(
                            "Ù„Ø§ ØªÙˆØ¬Ø¯ Ø¥Ø¹Ù„Ø§Ù†Ø§Øª Ø­Ø§Ù„ÙŠØ§Ù‹",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "ÙƒÙ† Ø£ÙˆÙ„ Ù…Ù† ÙŠÙ†Ø´Ø±!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    feed.forEachIndexed { index, listing ->
                        if (index > 0 && index % 5 == 0) {
                            item(key = "ad_$index") {
                                AdEarnCard(onAdWatched = { viewModel.watchAd() })
                            }
                        }
                        if (index > 0 && index % 6 == 0) {
                            item(key = "banner_$index") {
                                UnityBannerAd()
                            }
                        }
                        item(key = listing.id) {
                            if (listing.isExemptionRight) {
                                ExemptionListingCard(
                                    listing = listing,
                                    onClick = { onListingClick(listing.id) },
                                    onCalcClick = { onCalcClick(listing.expectedPrice ?: 0) },
                                )
                            } else {
                                CarListingCard(
                                    listing = listing,
                                    onClick = { onListingClick(listing.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CarListingCard(listing: ListingDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (listing.isBoosted) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp).background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.DriveEta,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = Color(0xFF90A4AE),
                )
                if (listing.isBoosted) {
                    TierBadge(tier = listing.tier, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
                }
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val title = buildString {
                    append(listing.makeModel)
                    listing.displayYear.takeIf { it.isNotEmpty() }?.let { append(" $it") }
                }
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                listing.expectedPrice?.let {
                    Text(
                        text = it.toJod(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                    )
                }
                val details = listOfNotNull(
                    listing.mileageKm?.let { "%,d ÙƒÙ…".format(it) },
                    listing.fuelTypeLabel.takeIf { it.isNotEmpty() },
                    listing.transmissionLabel.takeIf { it.isNotEmpty() },
                ).joinToString(" â€¢ ")
                if (details.isNotEmpty()) {
                    Text(text = details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("ðŸ“ ${listing.governorate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(listing.createdAt.toTimeAgo(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ExemptionListingCard(
    listing: ListingDto,
    onClick: () -> Unit,
    onCalcClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = ExemptionBg),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ðŸŽ–", fontSize = 18.sp)
                    Text("Ø¥Ø¹ÙØ§Ø¡ Ø¶Ø§Ø¨Ø·", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (listing.isBoosted) {
                        TierBadge(tier = listing.tier)
                    }
                    if (listing.isVerified) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Outlined.Verified, contentDescription = "Ù…ÙˆØ«Ù‘Ù‚", tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                            Text("Ù…ÙˆØ«Ù‘Ù‚", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }

            Text("ÙŠØ¨Ø­Ø« Ø¹Ù†: ${listing.makeModel}", style = MaterialTheme.typography.bodyMedium)
            listing.expectedPrice?.let {
                Text("Ø§Ù„Ø³Ø¹Ø±: ${it.toJod()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Gold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("ðŸ“ ${listing.governorate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(listing.createdAt.toTimeAgo(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(
                    onClick = onCalcClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text("Ø§Ø­Ø³Ø¨ Ø§Ù„Ø±Ø¨Ø­ ðŸ§®", style = MaterialTheme.typography.labelMedium, color = Gold)
                }
            }
        }
    }
}

@Composable
fun TierBadge(tier: String, modifier: Modifier = Modifier) {
    val (label, bg) = when (tier) {
        "GOLD" -> "Ø°Ù‡Ø¨ÙŠ" to Color(0xFFC9A961)
        "VIP" -> "VIP" to Color(0xFF7B1FA2)
        "FEATURED" -> "Ù…Ù…ÙŠØ²" to Color(0xFFE65100)
        else -> return
    }
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

