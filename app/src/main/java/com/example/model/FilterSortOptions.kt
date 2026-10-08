package com.example.model

enum class SortOption {
    FASTEST_PING,
    HIGHEST_SPEED,
    BEST_SCORE
}

data class FilterSortOptions(
    val selectedCountry: String = "ALL",
    val sortOption: SortOption = SortOption.FASTEST_PING,
    val minSpeedMbps: Double = 0.0,
    val showOnlyFavorites: Boolean = false,
    val searchQuery: String = ""
)
