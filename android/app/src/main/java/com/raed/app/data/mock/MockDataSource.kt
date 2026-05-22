package com.raed.app.data.mock

import androidx.compose.runtime.mutableStateListOf

object MockDataSource {

    private val _cars = mutableStateListOf(
        CarListing(
            id = "car-1", make = "تويوتا", model = "كامري", year = 2022,
            color = "أبيض", mileageKm = 45_000,
            fuelType = FuelType.GASOLINE, transmission = Transmission.AUTOMATIC,
            priceJod = 18_500, governorate = "عمّان",
            description = "سيارة بحالة ممتازة، صيانة دورية منتظمة، لم تُدخل ورشة.",
            phoneNumber = "0791234567",
            timeAgo = "منذ يومين",
        ),
        CarListing(
            id = "car-2", make = "هيونداي", model = "توسان", year = 2021,
            color = "رمادي", mileageKm = 62_000,
            fuelType = FuelType.GASOLINE, transmission = Transmission.AUTOMATIC,
            priceJod = 16_200, governorate = "إربد",
            description = "سيارة عائلية مريحة، لا تحتاج أي صيانة.",
            phoneNumber = "0782345678",
            timeAgo = "منذ 5 أيام",
        ),
        CarListing(
            id = "car-3", make = "كيا", model = "سبورتاج", year = 2023,
            color = "أسود", mileageKm = 28_000,
            fuelType = FuelType.HYBRID, transmission = Transmission.AUTOMATIC,
            priceJod = 24_800, governorate = "عمّان",
            description = "هايبرد اقتصادي، استهلاك وقود ممتاز، كفالة وكيل.",
            phoneNumber = "0773456789",
            timeAgo = "منذ ساعة",
        ),
    )

    private val _exemptions = mutableStateListOf(
        ExemptionListing(
            id = "ex-1", posterType = PosterType.OFFICER, isVerified = true,
            vehicleType = "SUV هايبرد", priceJod = 12_500, governorate = "عمّان",
            notes = "إعفاء متاح فوراً، التواصل جاد فقط.",
            phoneNumber = "0794567890",
            timeAgo = "منذ 3 ساعات",
        ),
        ExemptionListing(
            id = "ex-2", posterType = PosterType.BROKER, isVerified = true,
            vehicleType = "سيدان بنزين", priceJod = 12_000, governorate = "الزرقاء",
            notes = "ميزانية 12,000 د.أ، بحث عن سيدان موديل 2022+.",
            phoneNumber = "0785678901",
            timeAgo = "منذ يوم",
        ),
    )

    fun carById(id: String): CarListing? = _cars.find { it.id == id }
    fun exemptionById(id: String): ExemptionListing? = _exemptions.find { it.id == id }

    fun addCar(car: CarListing) { _cars.add(0, car) }
    fun addExemption(exemption: ExemptionListing) { _exemptions.add(0, exemption) }

    fun feed(
        typeFilter: FilterType = FilterType.ALL,
        governorateFilter: String? = null,
        query: String = "",
    ): List<FeedItem> {
        val cars = _cars
            .filter { governorateFilter == null || it.governorate == governorateFilter }
            .filter { query.isBlank() || "${it.make} ${it.model}".contains(query) || it.governorate.contains(query) }
            .map { FeedItem.CarItem(it) }

        val exemptions = _exemptions
            .filter { governorateFilter == null || it.governorate == governorateFilter }
            .filter { query.isBlank() || it.vehicleType.contains(query) || it.governorate.contains(query) }
            .map { FeedItem.ExemptionItem(it) }

        return when (typeFilter) {
            FilterType.ALL -> interleave(cars, exemptions)
            FilterType.CARS -> cars
            FilterType.EXEMPTIONS -> exemptions
        }
    }

    private fun interleave(cars: List<FeedItem>, exemptions: List<FeedItem>): List<FeedItem> {
        val result = mutableListOf<FeedItem>()
        val cIter = cars.iterator()
        val eIter = exemptions.iterator()
        while (cIter.hasNext() || eIter.hasNext()) {
            if (cIter.hasNext()) result.add(cIter.next())
            if (cIter.hasNext()) result.add(cIter.next())
            if (eIter.hasNext()) result.add(eIter.next())
        }
        return result
    }
}
