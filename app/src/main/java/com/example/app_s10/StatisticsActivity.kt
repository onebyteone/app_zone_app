package com.example.app_s10

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class StatisticsActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val gamesList = mutableListOf<Game>()

    // Views
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvTotalGames: TextView
    private lateinit var tvCompletedGames: TextView
    private lateinit var tvPendingGames: TextView
    private lateinit var progressCompletion: ProgressBar
    private lateinit var tvProgressPercentage: TextView
    private lateinit var rvGenreStats: RecyclerView
    private lateinit var rvPlatformStats: RecyclerView
    private lateinit var rvTopRatedGames: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        // Inicializar Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        initViews()
        setupRecyclerViews()
        loadGamesAndCalculateStats()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvTotalGames = findViewById(R.id.tvTotalGames)
        tvCompletedGames = findViewById(R.id.tvCompletedGames)
        tvPendingGames = findViewById(R.id.tvPendingGames)
        progressCompletion = findViewById(R.id.progressCompletion)
        tvProgressPercentage = findViewById(R.id.tvProgressPercentage)
        rvGenreStats = findViewById(R.id.rvGenreStats)
        rvPlatformStats = findViewById(R.id.rvPlatformStats)
        rvTopRatedGames = findViewById(R.id.rvTopRatedGames)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        rvGenreStats.layoutManager = LinearLayoutManager(this)
        rvPlatformStats.layoutManager = LinearLayoutManager(this)
        rvTopRatedGames.layoutManager = LinearLayoutManager(this)
    }

    private fun loadGamesAndCalculateStats() {
        val currentUser = auth.currentUser ?: return

        database.child("games")
            .orderByChild("userId")
            .equalTo(currentUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    gamesList.clear()
                    for (gameSnapshot in snapshot.children) {
                        val game = gameSnapshot.getValue(Game::class.java)
                        game?.let { gamesList.add(it) }
                    }
                    calculateAndDisplayStats()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Manejar error
                }
            })
    }

    private fun calculateAndDisplayStats() {
        if (gamesList.isEmpty()) {
            displayEmptyStats()
            return
        }

        val stats = calculateGameStats()
        displayStats(stats)
    }

    private fun calculateGameStats(): GameStats {
        val totalGames = gamesList.size
        val completedGames = gamesList.count { it.completed }
        val pendingGames = totalGames - completedGames
        val completionPercentage = if (totalGames > 0) (completedGames * 100f) / totalGames else 0f
        val averageRating = if (gamesList.isNotEmpty()) gamesList.map { it.rating }.average().toFloat() else 0f

        // Calcular estadísticas por género
        val genreMap = gamesList.groupBy { it.genre }
        val genreStats = genreMap.map { (genre, games) ->
            GenreStats(
                genre = genre,
                count = games.size,
                percentage = (games.size * 100f) / totalGames
            )
        }.sortedByDescending { it.count }

        // Calcular estadísticas por plataforma
        val platformMap = gamesList.groupBy { it.platform }
        val platformStats = platformMap.map { (platform, games) ->
            PlatformStats(
                platform = platform,
                count = games.size,
                percentage = (games.size * 100f) / totalGames
            )
        }.sortedByDescending { it.count }

        // Top 5 juegos mejor calificados
        val topRatedGames = gamesList
            .filter { it.rating > 0 }
            .sortedByDescending { it.rating }
            .take(5)

        return GameStats(
            totalGames = totalGames,
            completedGames = completedGames,
            pendingGames = pendingGames,
            completionPercentage = completionPercentage,
            averageRating = averageRating,
            genreStats = genreStats,
            platformStats = platformStats,
            topRatedGames = topRatedGames
        )
    }

    private fun displayStats(stats: GameStats) {
        // Mostrar estadísticas generales
        tvTotalGames.text = stats.totalGames.toString()
        tvCompletedGames.text = stats.completedGames.toString()
        tvPendingGames.text = stats.pendingGames.toString()

        progressCompletion.progress = stats.completionPercentage.toInt()
        tvProgressPercentage.text = String.format("%.1f%%", stats.completionPercentage)

        // Mostrar estadísticas por género
        if (stats.genreStats.isNotEmpty()) {
            val genreAdapter = StatsAdapter(stats.genreStats)
            rvGenreStats.adapter = genreAdapter
        }

        // Mostrar estadísticas por plataforma
        if (stats.platformStats.isNotEmpty()) {
            val platformAdapter = StatsAdapter(stats.platformStats)
            rvPlatformStats.adapter = platformAdapter
        }

        // Mostrar top juegos calificados
        if (stats.topRatedGames.isNotEmpty()) {
            val topGamesAdapter = GamesAdapter(
                onEditClick = { /* No action needed in stats */ },
                onDeleteClick = { /* No action needed in stats */ }
            )
            topGamesAdapter.updateGames(stats.topRatedGames)
            rvTopRatedGames.adapter = topGamesAdapter
        }
    }

    private fun displayEmptyStats() {
        tvTotalGames.text = "0"
        tvCompletedGames.text = "0"
        tvPendingGames.text = "0"
        progressCompletion.progress = 0
        tvProgressPercentage.text = "0%"
    }
}
