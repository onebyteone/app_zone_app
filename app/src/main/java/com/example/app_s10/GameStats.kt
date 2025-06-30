package com.example.app_s10

data class GenreStats(
    val genre: String,
    val count: Int,
    val percentage: Float
)

data class PlatformStats(
    val platform: String,
    val count: Int,
    val percentage: Float
)

data class GameStats(
    val totalGames: Int,
    val completedGames: Int,
    val pendingGames: Int,
    val completionPercentage: Float,
    val averageRating: Float,
    val genreStats: List<GenreStats>,
    val platformStats: List<PlatformStats>,
    val topRatedGames: List<Game>
)
