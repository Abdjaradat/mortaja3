package com.raed.app.data.mock

data class CarListing(
    val id: String,
    val make: String,
    val model: String,
    val year: Int,
    val color: String,
    val mileageKm: Int,
    val fuelType: FuelType,
    val transmission: Transmission,
    val priceJod: Int,
    val governorate: String,
    val description: String = "",
    val phoneNumber: String = "",
    val timeAgo: String,
)

data class ExemptionListing(
    val id: String,
    val posterType: PosterType,
    val isVerified: Boolean,
    val vehicleType: String,
    val priceJod: Int,
    val governorate: String,
    val notes: String = "",
    val phoneNumber: String = "",
    val timeAgo: String,
)

sealed class FeedItem {
    data class CarItem(val listing: CarListing) : FeedItem()
    data class ExemptionItem(val listing: ExemptionListing) : FeedItem()
}

enum class FuelType(val label: String) {
    GASOLINE("بنزين"),
    HYBRID("هايبرد"),
    ELECTRIC("كهربائي"),
    DIESEL("ديزل"),
}

enum class Transmission(val label: String) {
    AUTOMATIC("أوتوماتيك"),
    MANUAL("يدوي"),
}

enum class PosterType(val label: String) {
    OFFICER("ضابط"),
    BROKER("وسيط"),
}

enum class FilterType(val label: String, val emoji: String) {
    ALL("الكل", "✓"),
    CARS("سيارات", "🚗"),
    EXEMPTIONS("إعفاءات", "🎖"),
}

val GOVERNORATES = listOf(
    "عمّان", "إربد", "الزرقاء", "العقبة", "المفرق",
    "جرش", "عجلون", "الكرك", "الطفيلة", "معان", "السلط", "مادبا",
)

fun Int.toJod(): String = "%,d د.أ".format(this)

val CarListing.fullTitle get() = "$make $model $year"
