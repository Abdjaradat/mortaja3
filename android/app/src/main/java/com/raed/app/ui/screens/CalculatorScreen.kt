package com.raed.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.raed.app.ui.components.UnityBannerAd
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// â”€â”€ Tax rates per Decision No. 4306, June 28 2025 â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private enum class VehicleType(val label: String, val emoji: String, val taxRate: Double) {
    GASOLINE("Ø¨Ù†Ø²ÙŠÙ†",    "â›½", 0.51),
    HYBRID  ("Ù‡Ø§ÙŠØ¨Ø±Ø¯",   "ðŸ”µ", 0.39),
    ELECTRIC("ÙƒÙ‡Ø±Ø¨Ø§Ø¦ÙŠ",  "âš¡", 0.27),
}

private data class CalcResult(
    val basePrice:             Double,
    val taxSaved:              Double,
    val netProfit:             Double,
    val breakEvenMarketPrice:  Double,
)

private fun calculate(
    marketPrice:   Double,
    exemptionCost: Double,
    type:          VehicleType,
): CalcResult {
    val base           = marketPrice / (1.0 + type.taxRate)
    val taxSaved       = marketPrice - base
    val netProfit      = taxSaved - exemptionCost
    val breakEven      = exemptionCost * (1.0 + type.taxRate)
    return CalcResult(base, taxSaved, netProfit, breakEven)
}

private fun Double.jod(): String = "%,.0f Ø¯.Ø£".format(this)

// â”€â”€ Colors â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private val GreenText    = Color(0xFF2E7D32)
private val AmberText    = Color(0xFFF57F17)
private val GreenBg      = Color(0xFFE8F5E9)
private val AmberBg      = Color(0xFFFFF8E1)
private val RedBg        = Color(0xFFFFEBEE)

// â”€â”€ Screen â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(prefilledExemptionCost: String = "") {
    var marketPriceText   by remember { mutableStateOf("") }
    var exemptionCostText by remember { mutableStateOf(prefilledExemptionCost.ifBlank { "12000" }) }
    var vehicleType       by remember { mutableStateOf(VehicleType.GASOLINE) }
    var showHelp          by remember { mutableStateOf(false) }

    val marketPrice   = marketPriceText.toDoubleOrNull() ?: 0.0
    val exemptionCost = exemptionCostText.toDoubleOrNull() ?: 0.0
    val result        = if (marketPrice > 0.0 && exemptionCost > 0.0)
                            calculate(marketPrice, exemptionCost, vehicleType)
                        else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ø­Ø§Ø³Ø¨Ø© Ø§Ù„Ø¥Ø¹ÙØ§Ø¡") },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(
                            imageVector    = Icons.Outlined.HelpOutline,
                            contentDescription = "Ù…Ø³Ø§Ø¹Ø¯Ø©",
                        )
                    }
                },
            )
        },
        bottomBar = { UnityBannerAd() },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // â”€â”€ Collapsible info card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            var infoExpanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { infoExpanded = !infoExpanded }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            "ÙƒÙŠÙ ØªØ¹Ù…Ù„ Ø§Ù„Ø­Ø§Ø³Ø¨Ø©ØŸ ðŸ’¡",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Icon(
                            imageVector        = if (infoExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    AnimatedVisibility(visible = infoExpanded) {
                        Text(
                            text  = "Ø£Ø¯Ø®Ù„ Ø³Ø¹Ø± Ø§Ù„Ø³ÙŠØ§Ø±Ø© ÙÙŠ Ø§Ù„Ø³ÙˆÙ‚ ÙˆØ§Ø®ØªØ± Ù†ÙˆØ¹ Ø§Ù„ÙˆÙ‚ÙˆØ¯ØŒ ÙˆØ³ØªØ­Ø³Ø¨ Ø§Ù„Ø­Ø§Ø³Ø¨Ø© ØªÙ„Ù‚Ø§Ø¦ÙŠØ§Ù‹:\n" +
                                    "- Ù‚ÙŠÙ…Ø© Ø§Ù„Ø¶Ø±ÙŠØ¨Ø© Ø§Ù„ØªÙŠ ÙˆÙÙ‘Ø±Ù‡Ø§ Ø§Ù„Ø¥Ø¹ÙØ§Ø¡\n" +
                                    "- Ø±Ø¨Ø­Ùƒ Ø§Ù„ØµØ§ÙÙŠ Ø¨Ø¹Ø¯ Ø®ØµÙ… Ù…Ø§ Ø¯ÙØ¹ØªÙ‡ Ù„Ù„Ø¶Ø§Ø¨Ø·\n" +
                                    "- Ø§Ù„Ø­Ø¯ Ø§Ù„Ø£Ø¯Ù†Ù‰ Ù„Ø³Ø¹Ø± Ø§Ù„Ø³ÙŠØ§Ø±Ø© Ø§Ù„Ø°ÙŠ ÙŠØ¬Ø¹Ù„ Ø§Ù„ØµÙÙ‚Ø© Ù…Ø±Ø¨Ø­Ø©\n\n" +
                                    "ðŸ’¡ Ù†ØµÙŠØ­Ø©: Ø§Ø¨Ø­Ø« Ø¹Ù† Ø³ÙŠØ§Ø±Ø© ÙŠÙƒÙˆÙ† Ø³Ø¹Ø±Ù‡Ø§ ÙÙŠ Ø§Ù„Ø³ÙˆÙ‚ Ø£Ø¹Ù„Ù‰ Ù…Ù† 'Ø£Ø¯Ù†Ù‰ Ø³Ø¹Ø± Ù„Ù„Ø±Ø¨Ø­'",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        )
                    }
                }
            }

            // â”€â”€ Input: market price â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            OutlinedTextField(
                value         = marketPriceText,
                onValueChange = { marketPriceText = it.filter { c -> c.isDigit() || c == '.' } },
                label         = { Text("Ø³Ø¹Ø± Ø§Ù„Ø³ÙŠØ§Ø±Ø© ÙÙŠ Ø§Ù„Ø³ÙˆÙ‚") },
                placeholder   = { Text("Ø§Ù„Ø³Ø¹Ø± Ø§Ù„Ø°ÙŠ ÙŠØ¯ÙØ¹Ù‡ Ø§Ù„Ù…ÙˆØ§Ø·Ù† Ø§Ù„Ø¹Ø§Ø¯ÙŠ") },
                suffix        = { Text("Ø¯.Ø£") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // â”€â”€ Input: vehicle type â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text(
                text  = "Ù†ÙˆØ¹ Ø§Ù„Ù…Ø­Ø±Ùƒ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                VehicleType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = vehicleType == type,
                        onClick  = { vehicleType = type },
                        shape    = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = VehicleType.entries.size,
                        ),
                        label = {
                            Text(
                                text  = "${type.emoji} ${type.label}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                    )
                }
            }
            Text(
                text  = "Ù…Ø¹Ø¯Ù„ Ø§Ù„Ø¶Ø±ÙŠØ¨Ø©: ${(vehicleType.taxRate * 100).toInt()}%  â€”  Ù‚Ø±Ø§Ø± Ø±Ù‚Ù… 4306 Ø¨ØªØ§Ø±ÙŠØ® 28 ÙŠÙˆÙ†ÙŠÙˆ 2025",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )

            // â”€â”€ Input: exemption cost â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            OutlinedTextField(
                value         = exemptionCostText,
                onValueChange = { exemptionCostText = it.filter { c -> c.isDigit() || c == '.' } },
                label         = { Text("Ø³Ø¹Ø± Ø´Ø±Ø§Ø¡ Ø§Ù„Ø¥Ø¹ÙØ§Ø¡ Ù…Ù† Ø§Ù„Ø¶Ø§Ø¨Ø·") },
                placeholder   = { Text("ÙŠØªØºÙŠØ± Ø­Ø³Ø¨ Ø§Ù„Ø³ÙˆÙ‚ â€” ÙƒØ§Ù† 16,000 ÙÙŠ 2024") },
                suffix        = { Text("Ø¯.Ø£") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // â”€â”€ Results card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (result != null) {
                ResultsCard(result, exemptionCost)
                VerdictCard(result)
                ExampleCard(marketPrice, exemptionCost, vehicleType, result)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // â”€â”€ Help bottom sheet â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    if (showHelp) {
        HelpBottomSheet(onDismiss = { showHelp = false })
    }
}

// â”€â”€ Results card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ResultsCard(result: CalcResult, exemptionCost: Double) {
    val profitColor = when {
        result.netProfit > 1000.0 -> GreenText
        result.netProfit >= 0.0   -> AmberText
        else                      -> MaterialTheme.colorScheme.error
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text  = "Ù†ØªØ§Ø¦Ø¬ Ø§Ù„Ø­Ø³Ø§Ø¨",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider()

            ResultRow(
                label = "Ù‚ÙŠÙ…Ø© Ø§Ù„Ø¶Ø±ÙŠØ¨Ø© Ø§Ù„Ù…ÙˆÙÙ‘Ø±Ø©",
                value = result.taxSaved.jod(),
            )
            ResultRow(
                label = "Ø¯ÙØ¹Øª Ù…Ù‚Ø§Ø¨Ù„ Ø§Ù„Ø¥Ø¹ÙØ§Ø¡",
                value = exemptionCost.jod(),
            )

            HorizontalDivider()

            ResultRow(
                label      = "Ø±Ø¨Ø­Ùƒ Ø§Ù„ØµØ§ÙÙŠ",
                value      = result.netProfit.jod(),
                valueColor = profitColor,
                bold       = true,
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ResultRow(
                    label = "Ø£Ø¯Ù†Ù‰ Ø³Ø¹Ø± Ø³ÙˆÙ‚ Ù„Ù„Ø±Ø¨Ø­",
                    value = result.breakEvenMarketPrice.jod(),
                )
                Text(
                    text  = "Ø£ÙŠ Ø³ÙŠØ§Ø±Ø© Ø£ØºÙ„Ù‰ Ù…Ù† Ù‡Ø°Ø§ Ø§Ù„Ø³Ø¹Ø± = ØµÙÙ‚Ø© Ù…Ø±Ø¨Ø­Ø©",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

// â”€â”€ Verdict card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun VerdictCard(result: CalcResult) {
    val (bg, icon, title, subtitle) = when {
        result.netProfit > 1000.0 -> Quad(
            bg       = GreenBg,
            icon     = "âœ…",
            title    = "Ø§Ù„ØµÙÙ‚Ø© Ù…Ø±Ø¨Ø­Ø©",
            subtitle = "ØªØ±Ø¨Ø­ ${result.netProfit.jod()} Ù…Ù† Ù‡Ø°Ù‡ Ø§Ù„ØµÙÙ‚Ø©",
        )
        result.netProfit >= 0.0   -> Quad(
            bg       = AmberBg,
            icon     = "âš ï¸",
            title    = "Ø§Ù„ØµÙÙ‚Ø© Ø¹Ù„Ù‰ Ø§Ù„Ø­Ø§ÙØ©",
            subtitle = "Ø§Ù„Ø±Ø¨Ø­ Ø¶Ø¹ÙŠÙ â€” Ø§Ø­Ø³Ø¨ ØªÙƒØ§Ù„ÙŠÙ Ø¥Ø¶Ø§ÙÙŠØ© (ØªØ±Ø®ÙŠØµØŒ Ù†Ù‚Ù„...)",
        )
        else                      -> Quad(
            bg       = RedBg,
            icon     = "âŒ",
            title    = "Ø§Ù„ØµÙÙ‚Ø© Ø®Ø§Ø³Ø±Ø©",
            subtitle = "ØªØ­ØªØ§Ø¬ Ø³ÙŠØ§Ø±Ø© Ø¨Ø³Ø¹Ø± Ø³ÙˆÙ‚ Ù„Ø§ ÙŠÙ‚Ù„ Ø¹Ù† ${result.breakEvenMarketPrice.jod()}",
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = bg),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = icon, style = MaterialTheme.typography.displaySmall)
            Text(
                text      = title,
                style     = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text      = subtitle,
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// â”€â”€ Example card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ExampleCard(
    marketPrice:   Double,
    exemptionCost: Double,
    vehicleType:   VehicleType,
    result:        CalcResult,
) {
    val verdict = when {
        result.netProfit > 1000.0 -> "ØµÙÙ‚Ø© Ù…Ø±Ø¨Ø­Ø© âœ…"
        result.netProfit >= 0.0   -> "Ø¹Ù„Ù‰ Ø§Ù„Ø­Ø§ÙØ© âš ï¸"
        else                      -> "ØµÙÙ‚Ø© Ø®Ø§Ø³Ø±Ø© âŒ"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text  = "Ù…Ø«Ø§Ù„ Ù…Ø­Ø³ÙˆØ¨",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(2.dp))
            ExampleLine("${vehicleType.emoji} ${vehicleType.label} â€” Ø³Ø¹Ø± Ø§Ù„Ø³ÙˆÙ‚", marketPrice.jod())
            ExampleLine("Ø³Ø¹Ø± Ø§Ù„ÙˆÙƒÙŠÙ„ Ø§Ù„Ø£Ø³Ø§Ø³ÙŠ (Ù‚Ø¨Ù„ Ø§Ù„Ø¶Ø±ÙŠØ¨Ø©)", result.basePrice.jod())
            ExampleLine("Ø§Ù„Ø¶Ø±ÙŠØ¨Ø© Ø§Ù„Ù…ÙˆÙÙ‘Ø±Ø©", result.taxSaved.jod())
            ExampleLine("Ø¯ÙØ¹Øª Ù„Ù„Ø¶Ø§Ø¨Ø·", exemptionCost.jod())
            Spacer(Modifier.height(4.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text  = "Ø±Ø¨Ø­Ùƒ: ${result.netProfit.jod()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text  = verdict,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// â”€â”€ Help bottom sheet â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text      = "ÙƒÙŠÙ ØªØ¹Ù…Ù„ Ø­Ø§Ø³Ø¨Ø© Ø§Ù„Ø¥Ø¹ÙØ§Ø¡ØŸ",
                style     = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            HelpSection(
                title = "Ù…Ø§ Ù‡Ùˆ Ø§Ù„Ø¥Ø¹ÙØ§Ø¡ØŸ",
                body  = "Ø¥Ø¹ÙØ§Ø¡ Ø§Ù„Ø¶Ø§Ø¨Ø· Ù‡Ùˆ Ø­Ù‚ Ù‚Ø§Ù†ÙˆÙ†ÙŠ ÙŠØªÙŠØ­ Ø´Ø±Ø§Ø¡ Ø³ÙŠØ§Ø±Ø© Ø¨Ø¯ÙˆÙ† Ø¶Ø±ÙŠØ¨Ø© Ø§Ù„Ù…Ø¨ÙŠØ¹Ø§Øª " +
                        "(51% Ù„Ù„Ø¨Ù†Ø²ÙŠÙ†ØŒ 39% Ù„Ù„Ù‡Ø§ÙŠØ¨Ø±Ø¯ØŒ 27% Ù„Ù„ÙƒÙ‡Ø±Ø¨Ø§Ø¦ÙŠ).\n\n" +
                        "Ø§Ù„ÙˆØ³ÙŠØ· ÙŠØ´ØªØ±ÙŠ Ù‡Ø°Ø§ Ø§Ù„Ø­Ù‚ Ù…Ù† Ø§Ù„Ø¶Ø§Ø¨Ø· Ø¨Ø³Ø¹Ø± Ø§Ù„Ø³ÙˆÙ‚ØŒ ÙˆÙŠØ³ØªØ®Ø¯Ù…Ù‡ Ø¹Ù„Ù‰ " +
                        "Ø³ÙŠØ§Ø±Ø© ÙŠØ®ØªØ§Ø±Ù‡Ø§. Ø±Ø¨Ø­Ù‡ Ù‡Ùˆ Ø§Ù„ÙØ±Ù‚ Ø¨ÙŠÙ† Ø§Ù„Ø¶Ø±ÙŠØ¨Ø© Ø§Ù„Ù…ÙˆÙÙ‘Ø±Ø© ÙˆÙ…Ø§ Ø¯ÙØ¹Ù‡.",
            )

            HelpSection(title = "Ù…Ø«Ø§Ù„ ØªÙˆØ¶ÙŠØ­ÙŠ") {
                HelpExampleBlock(
                    lines = listOf(
                        "Ø³ÙŠØ§Ø±Ø© Ø¨Ù†Ø²ÙŠÙ† â€” Ø³Ø¹Ø± Ø§Ù„Ø³ÙˆÙ‚: 35,000 Ø¯.Ø£",
                        "Ø§Ù„Ø¶Ø±ÙŠØ¨Ø© Ø§Ù„Ù…ÙˆÙÙ‘Ø±Ø© (51%): 11,722 Ø¯.Ø£",
                        "Ø¯ÙØ¹Øª Ù„Ù„Ø¶Ø§Ø¨Ø·: 12,000 Ø¯.Ø£",
                        "Ø§Ù„Ø±Ø¨Ø­: 278- âŒ Ø®Ø³Ø§Ø±Ø© Ø¨Ø³ÙŠØ·Ø©",
                        "â† Ø¬Ø±Ù‘Ø¨ Ø³ÙŠØ§Ø±Ø© Ø£ØºÙ„Ù‰!",
                    ),
                )
                Spacer(Modifier.height(8.dp))
                HelpExampleBlock(
                    lines = listOf(
                        "Ù‡Ø§ÙŠØ¨Ø±Ø¯ â€” Ø³Ø¹Ø± Ø§Ù„Ø³ÙˆÙ‚ 40,000: Ø§Ù„Ø¶Ø±ÙŠØ¨Ø© 11,151 â†’ Ø±Ø¨Ø­ 849- âŒ",
                        "Ù‡Ø§ÙŠØ¨Ø±Ø¯ â€” Ø³Ø¹Ø± Ø§Ù„Ø³ÙˆÙ‚ 45,000: Ø§Ù„Ø¶Ø±ÙŠØ¨Ø© 12,544 â†’ Ø±Ø¨Ø­ 544 âš ï¸ Ø¹Ù„Ù‰ Ø§Ù„Ø­Ø§ÙØ©",
                        "Ù‡Ø§ÙŠØ¨Ø±Ø¯ â€” Ø³Ø¹Ø± Ø§Ù„Ø³ÙˆÙ‚ 55,000: Ø§Ù„Ø¶Ø±ÙŠØ¨Ø© 15,331 â†’ Ø±Ø¨Ø­ 3,331 âœ…âœ… Ù…Ø±Ø¨Ø­Ø©",
                    ),
                )
            }

            HelpSection(
                title = "Ù„Ù…Ø§Ø°Ø§ ÙŠØªØºÙŠØ± Ø³Ø¹Ø± Ø§Ù„Ø¥Ø¹ÙØ§Ø¡ØŸ",
                body  = "Ø³Ø¹Ø± Ø§Ù„Ø¥Ø¹ÙØ§Ø¡ ÙŠØ­Ø¯Ø¯Ù‡ Ø§Ù„Ø³ÙˆÙ‚ Ù…Ø«Ù„ Ø£ÙŠ Ø³Ù„Ø¹Ø©:\n" +
                        "â€¢ Ø¹Ù†Ø¯Ù…Ø§ ÙŠØ±ØªÙØ¹ Ø§Ù„Ø·Ù„Ø¨ Ø¹Ù„Ù‰ Ø§Ù„Ø³ÙŠØ§Ø±Ø§Øª â† ÙŠØ±ØªÙØ¹ Ø³Ø¹Ø± Ø§Ù„Ø¥Ø¹ÙØ§Ø¡\n" +
                        "â€¢ Ø¹Ù†Ø¯Ù…Ø§ ÙŠØªØºÙŠØ± Ø§Ù„Ù‚Ø§Ù†ÙˆÙ† â† ÙŠØªØºÙŠØ± Ø³Ø¹Ø± Ø§Ù„Ø¥Ø¹ÙØ§Ø¡\n\n" +
                        "ÙÙŠ 2024 ÙƒØ§Ù† Ø§Ù„Ø³Ø¹Ø± ~16,000 Ø¯.Ø£\n" +
                        "ÙÙŠ 2025 Ø§Ù†Ø®ÙØ¶ Ù„Ù€ ~12,000 Ø¯.Ø£ Ø¨Ø³Ø¨Ø¨ Ù‚Ø±Ø§Ø± ØªØ®ÙÙŠØ¶ Ø§Ù„Ø¶Ø±Ø§Ø¦Ø¨\n\n" +
                        "ØªØ­Ù‚Ù‚ Ø¯Ø§Ø¦Ù…Ø§Ù‹ Ù…Ù† Ø§Ù„Ø³Ø¹Ø± Ø§Ù„Ø­Ø§Ù„ÙŠ Ù‚Ø¨Ù„ Ø£ÙŠ ØµÙÙ‚Ø©.",
            )

            HelpSection(title = "ØªÙ†Ø¨ÙŠÙ‡Ø§Øª Ù…Ù‡Ù…Ø©") {
                val warnings = listOf(
                    "Ù†Ø³Ø¨ Ø§Ù„Ø¶Ø±ÙŠØ¨Ø© Ø­Ø³Ø¨ Ù‚Ø±Ø§Ø± 28 ÙŠÙˆÙ†ÙŠÙˆ 2025 â€” Ù‚Ø¯ ØªØªØºÙŠØ± Ø¨Ø£ÙŠ ÙˆÙ‚Øª",
                    "Ø§Ù„Ø­Ø§Ø³Ø¨Ø© Ù„Ø§ ØªØ´Ù…Ù„: Ø±Ø³ÙˆÙ… Ø§Ù„ØªØ±Ø®ÙŠØµØŒ Ù†Ù‚Ù„ Ø§Ù„Ù…Ù„ÙƒÙŠØ©ØŒ Ø¹Ù…ÙˆÙ„Ø© Ø§Ù„ÙˆØ³ÙŠØ·",
                    "Ø³Ø¹Ø± Ø§Ù„Ø¥Ø¹ÙØ§Ø¡ Ø§Ù„Ù…Ø¹Ø±ÙˆØ¶ Ø§ÙØªØ±Ø§Ø¶ÙŠ â€” ØªØ­Ù‚Ù‚ Ù…Ù† Ø§Ù„Ø³Ø¹Ø± Ø§Ù„ÙØ¹Ù„ÙŠ Ø¨Ø§Ù„Ø³ÙˆÙ‚",
                    "Ù‡Ø°Ø§ Ø§Ù„ØªØ·Ø¨ÙŠÙ‚ Ù…Ù†ØµØ© Ù…Ø¹Ù„ÙˆÙ…Ø§Øª ÙÙ‚Ø· ÙˆÙ„ÙŠØ³ Ù…Ø³Ø¤ÙˆÙ„Ø§Ù‹ Ø¹Ù† Ø£ÙŠ ØµÙÙ‚Ø©",
                )
                warnings.forEach { warning ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("âš ï¸")
                        Text(
                            text  = warning,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Button(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("ÙÙ‡Ù…ØªØŒ Ù„Ù†Ø¨Ø¯Ø£ Ø§Ù„Ø­Ø³Ø§Ø¨")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// â”€â”€ Small shared composables â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ResultRow(
    label:      String,
    value:      String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    bold:       Boolean = false,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodyMedium,
            color      = valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ExampleLine(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HelpSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.primary,
        )
        Text(
            text  = body,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HelpSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.primary,
        )
        content()
    }
}

@Composable
private fun HelpExampleBlock(lines: List<String>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            lines.forEach { line ->
                Text(text = line, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// â”€â”€ Utility â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private data class Quad<A, B, C, D>(val bg: A, val icon: B, val title: C, val subtitle: D)

