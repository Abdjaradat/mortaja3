package com.raed.app.data.mock

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

enum class VehicleRequestType(val label: String) {
    SEDAN("سيدان"),
    SUV("SUV"),
    PICKUP("بيكأب"),
    HYBRID("هايبرد"),
    ELECTRIC("كهربائي"),
}

data class TokenBid(
    val brokerId: String,
    val brokerName: String,
    val tokens: Int,
    val placedAt: Long = System.currentTimeMillis(),
)

data class BuyerRequest(
    val id: String,
    val vehicleType: VehicleRequestType,
    val budgetMin: Int,
    val budgetMax: Int,
    val governorate: String,
    val notes: String = "",
    val postedAt: Long,
    val durationMs: Long = 24L * 60 * 60 * 1000,
    val bids: SnapshotStateList<TokenBid> = mutableStateListOf(),
) {
    val endsAt: Long get() = postedAt + durationMs
    val highestBid: Int get() = bids.maxOfOrNull { it.tokens } ?: 0
    val bidCount: Int get() = bids.size
    val myBid: TokenBid? get() = bids.find { it.brokerId == "me" }
    val amIWinning: Boolean get() = myBid != null && myBid!!.tokens == highestBid
}

object MockRequestsSource {
    private val _requests = mutableStateListOf<BuyerRequest>()

    init {
        val now = System.currentTimeMillis()
        val hour = 60L * 60 * 1000
        // SUV هايبرد — 18h remaining (posted 6h ago)
        _requests.add(
            BuyerRequest(
                id = "req-1",
                vehicleType = VehicleRequestType.SUV,
                budgetMin = 30_000,
                budgetMax = 40_000,
                governorate = "عمّان",
                notes = "يفضل هايبرد، موديل 2021 أو أحدث",
                postedAt = now - 6 * hour,
                bids = mutableStateListOf(
                    TokenBid("broker-a", "أحمد الوسيط", 150),
                    TokenBid("broker-b", "محمد الوسيط", 120),
                    TokenBid("broker-c", "عمر الوسيط", 100),
                ),
            ),
        )
        // سيدان بنزين — 6h remaining (posted 18h ago)
        _requests.add(
            BuyerRequest(
                id = "req-2",
                vehicleType = VehicleRequestType.SEDAN,
                budgetMin = 15_000,
                budgetMax = 20_000,
                governorate = "إربد",
                postedAt = now - 18 * hour,
                bids = mutableStateListOf(
                    TokenBid("broker-a", "أحمد الوسيط", 80),
                ),
            ),
        )
    }

    fun getAll(): List<BuyerRequest> = _requests

    fun getById(id: String): BuyerRequest? = _requests.find { it.id == id }

    fun addRequest(request: BuyerRequest) {
        _requests.add(0, request)
    }

    fun placeBid(requestId: String, tokens: Int) {
        val request = _requests.find { it.id == requestId } ?: return
        request.bids.removeAll { it.brokerId == "me" }
        request.bids.add(TokenBid("me", "أنت", tokens))
    }
}
