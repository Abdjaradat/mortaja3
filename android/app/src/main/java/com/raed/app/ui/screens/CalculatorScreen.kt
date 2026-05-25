package com.raed.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.raed.app.ui.components.UnityBannerCard
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

// ── Tax rates per Decision No. 4306, June 28 2025 ──────────────────────────

private enum class VehicleType(val label: String, val emoji: String, val taxRate: Double) {
    GASOLINE("بنزين",    "⛽", 0.51),
    HYBRID  ("هايبرد",   "🔵", 0.39),
    ELECTRIC("كهربائي",  "⚡", 0.27),
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

private fun Double.jod(): String = "%,.0f د.أ".format(this)

// ── Colors ──────────────────────────────────────────────────────────────────

private val GreenText    = Color(0xFF2E7D32)
private val AmberText    = Color(0xFFF57F17)
private val GreenBg      = Color(0xFFE8F5E9)
private val AmberBg      = Color(0xFFFFF8E1)
private val RedBg        = Color(0xFFFFEBEE)

// ── Screen ──────────────────────────────────────────────────────────────────

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
                title = { Text("حاسبة الإعفاء") },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(
                            imageVector    = Icons.Outlined.HelpOutline,
                            contentDescription = "مساعدة",
                        )
                    }
                },
            )
        },
        bottomBar = { UnityBannerCard() },
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

            // ── Collapsible info card ────────────────────────────────────
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
                            "كيف تعمل الحاسبة؟ 💡",
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
                            text  = "أدخل سعر السيارة في السوق واختر نوع الوقود، وستحسب الحاسبة تلقائياً:\n" +
                                    "- قيمة الضريبة التي وفّرها الإعفاء\n" +
                                    "- ربحك الصافي بعد خصم ما دفعته للضابط\n" +
                                    "- الحد الأدنى لسعر السيارة الذي يجعل الصفقة مربحة\n\n" +
                                    "💡 نصيحة: ابحث عن سيارة يكون سعرها في السوق أعلى من 'أدنى سعر للربح'",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        )
                    }
                }
            }

            // ── Input: market price ──────────────────────────────────────
            OutlinedTextField(
                value         = marketPriceText,
                onValueChange = { marketPriceText = it.filter { c -> c.isDigit() || c == '.' } },
                label         = { Text("سعر السيارة في السوق") },
                placeholder   = { Text("السعر الذي يدفعه المواطن العادي") },
                suffix        = { Text("د.أ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // ── Input: vehicle type ──────────────────────────────────────
            Text(
                text  = "نوع المحرك",
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
                text  = "معدل الضريبة: ${(vehicleType.taxRate * 100).toInt()}%  —  قرار رقم 4306 بتاريخ 28 يونيو 2025",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )

            // ── Input: exemption cost ────────────────────────────────────
            OutlinedTextField(
                value         = exemptionCostText,
                onValueChange = { exemptionCostText = it.filter { c -> c.isDigit() || c == '.' } },
                label         = { Text("سعر شراء الإعفاء من الضابط") },
                placeholder   = { Text("يتغير حسب السوق — كان 16,000 في 2024") },
                suffix        = { Text("د.أ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )

            // ── Results card ─────────────────────────────────────────────
            if (result != null) {
                ResultsCard(result, exemptionCost)
                VerdictCard(result)
                ExampleCard(marketPrice, exemptionCost, vehicleType, result)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Help bottom sheet ────────────────────────────────────────────────
    if (showHelp) {
        HelpBottomSheet(onDismiss = { showHelp = false })
    }
}

// ── Results card ─────────────────────────────────────────────────────────────

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
                text  = "نتائج الحساب",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider()

            ResultRow(
                label = "قيمة الضريبة الموفّرة",
                value = result.taxSaved.jod(),
            )
            ResultRow(
                label = "دفعت مقابل الإعفاء",
                value = exemptionCost.jod(),
            )

            HorizontalDivider()

            ResultRow(
                label      = "ربحك الصافي",
                value      = result.netProfit.jod(),
                valueColor = profitColor,
                bold       = true,
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ResultRow(
                    label = "أدنى سعر سوق للربح",
                    value = result.breakEvenMarketPrice.jod(),
                )
                Text(
                    text  = "أي سيارة أغلى من هذا السعر = صفقة مربحة",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

// ── Verdict card ──────────────────────────────────────────────────────────────

@Composable
private fun VerdictCard(result: CalcResult) {
    val (bg, icon, title, subtitle) = when {
        result.netProfit > 1000.0 -> Quad(
            bg       = GreenBg,
            icon     = "✅",
            title    = "الصفقة مربحة",
            subtitle = "تربح ${result.netProfit.jod()} من هذه الصفقة",
        )
        result.netProfit >= 0.0   -> Quad(
            bg       = AmberBg,
            icon     = "⚠️",
            title    = "الصفقة على الحافة",
            subtitle = "الربح ضعيف — احسب تكاليف إضافية (ترخيص، نقل...)",
        )
        else                      -> Quad(
            bg       = RedBg,
            icon     = "❌",
            title    = "الصفقة خاسرة",
            subtitle = "تحتاج سيارة بسعر سوق لا يقل عن ${result.breakEvenMarketPrice.jod()}",
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

// ── Example card ──────────────────────────────────────────────────────────────

@Composable
private fun ExampleCard(
    marketPrice:   Double,
    exemptionCost: Double,
    vehicleType:   VehicleType,
    result:        CalcResult,
) {
    val verdict = when {
        result.netProfit > 1000.0 -> "صفقة مربحة ✅"
        result.netProfit >= 0.0   -> "على الحافة ⚠️"
        else                      -> "صفقة خاسرة ❌"
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
                text  = "مثال محسوب",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(2.dp))
            ExampleLine("${vehicleType.emoji} ${vehicleType.label} — سعر السوق", marketPrice.jod())
            ExampleLine("سعر الوكيل الأساسي (قبل الضريبة)", result.basePrice.jod())
            ExampleLine("الضريبة الموفّرة", result.taxSaved.jod())
            ExampleLine("دفعت للضابط", exemptionCost.jod())
            Spacer(Modifier.height(4.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text  = "ربحك: ${result.netProfit.jod()}",
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

// ── Help bottom sheet ─────────────────────────────────────────────────────────

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
                text      = "كيف تعمل حاسبة الإعفاء؟",
                style     = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            HelpSection(
                title = "ما هو الإعفاء؟",
                body  = "إعفاء الضابط هو حق قانوني يتيح شراء سيارة بدون ضريبة المبيعات " +
                        "(51% للبنزين، 39% للهايبرد، 27% للكهربائي).\n\n" +
                        "الوسيط يشتري هذا الحق من الضابط بسعر السوق، ويستخدمه على " +
                        "سيارة يختارها. ربحه هو الفرق بين الضريبة الموفّرة وما دفعه.",
            )

            HelpSection(title = "مثال توضيحي") {
                HelpExampleBlock(
                    lines = listOf(
                        "سيارة بنزين — سعر السوق: 35,000 د.أ",
                        "الضريبة الموفّرة (51%): 11,722 د.أ",
                        "دفعت للضابط: 12,000 د.أ",
                        "الربح: 278- ❌ خسارة بسيطة",
                        "← جرّب سيارة أغلى!",
                    ),
                )
                Spacer(Modifier.height(8.dp))
                HelpExampleBlock(
                    lines = listOf(
                        "هايبرد — سعر السوق 40,000: الضريبة 11,151 → ربح 849- ❌",
                        "هايبرد — سعر السوق 45,000: الضريبة 12,544 → ربح 544 ⚠️ على الحافة",
                        "هايبرد — سعر السوق 55,000: الضريبة 15,331 → ربح 3,331 ✅✅ مربحة",
                    ),
                )
            }

            HelpSection(
                title = "لماذا يتغير سعر الإعفاء؟",
                body  = "سعر الإعفاء يحدده السوق مثل أي سلعة:\n" +
                        "• عندما يرتفع الطلب على السيارات ← يرتفع سعر الإعفاء\n" +
                        "• عندما يتغير القانون ← يتغير سعر الإعفاء\n\n" +
                        "في 2024 كان السعر ~16,000 د.أ\n" +
                        "في 2025 انخفض لـ ~12,000 د.أ بسبب قرار تخفيض الضرائب\n\n" +
                        "تحقق دائماً من السعر الحالي قبل أي صفقة.",
            )

            HelpSection(title = "تنبيهات مهمة") {
                val warnings = listOf(
                    "نسب الضريبة حسب قرار 28 يونيو 2025 — قد تتغير بأي وقت",
                    "الحاسبة لا تشمل: رسوم الترخيص، نقل الملكية، عمولة الوسيط",
                    "سعر الإعفاء المعروض افتراضي — تحقق من السعر الفعلي بالسوق",
                    "هذا التطبيق منصة معلومات فقط وليس مسؤولاً عن أي صفقة",
                )
                warnings.forEach { warning ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("⚠️")
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
                Text("فهمت، لنبدأ الحساب")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Small shared composables ──────────────────────────────────────────────────

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

// ── Utility ───────────────────────────────────────────────────────────────────

private data class Quad<A, B, C, D>(val bg: A, val icon: B, val title: C, val subtitle: D)
