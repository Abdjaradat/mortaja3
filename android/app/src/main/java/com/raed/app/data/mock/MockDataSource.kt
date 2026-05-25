package com.raed.app.data.mock

import androidx.compose.runtime.mutableStateListOf

object MockDataSource {

    private val _cars = mutableStateListOf<CarListing>()

    private val _exemptions = mutableStateListOf<ExemptionListing>()

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
