package com.example.app_s10

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.util.Log

class GamesListActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var gamesAdapter: GamesAdapter
    private val gamesList = mutableListOf<Game>()
    private val filteredGamesList = mutableListOf<Game>()

    // Views
    private lateinit var toolbar: MaterialToolbar
    private lateinit var etSearch: TextInputEditText
    private lateinit var spinnerGenre: MaterialAutoCompleteTextView
    private lateinit var spinnerPlatform: MaterialAutoCompleteTextView
    private lateinit var btnFilterCompleted: MaterialButton
    private lateinit var btnFilterPending: MaterialButton
    private lateinit var btnClearFilters: MaterialButton
    private lateinit var rvGames: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var fabAddGame: FloatingActionButton

    // Filtros
    private var currentSearchQuery = ""
    private var currentGenreFilter = ""
    private var currentPlatformFilter = ""
    private var currentCompletedFilter: Boolean? = null

    private val genres = arrayOf(
        "Acción", "Aventura", "RPG", "Estrategia", "Simulación",
        "Deportes", "Carreras", "Puzzle", "Terror", "Plataformas",
        "Shooter", "MMORPG", "Indie", "Casual"
    )

    private val platforms = arrayOf(
        "PC", "PlayStation 5", "PlayStation 4", "Xbox Series X/S",
        "Xbox One", "Nintendo Switch", "Mobile", "Mac", "Linux"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games_list)

        // Inicializar Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Inicializar views
        initViews()
        setupRecyclerView()
        setupDropdowns()
        setupSearchAndFilters()
        setupClickListeners()

        // Cargar juegos
        loadGames()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        etSearch = findViewById(R.id.etSearch)
        spinnerGenre = findViewById(R.id.spinnerGenre)
        spinnerPlatform = findViewById(R.id.spinnerPlatform)
        btnFilterCompleted = findViewById(R.id.btnFilterCompleted)
        btnFilterPending = findViewById(R.id.btnFilterPending)
        btnClearFilters = findViewById(R.id.btnClearFilters)
        rvGames = findViewById(R.id.rvGames)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        fabAddGame = findViewById(R.id.fabAddGame)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        gamesAdapter = GamesAdapter(
            onEditClick = { game -> editGame(game) },
            onDeleteClick = { game -> showDeleteConfirmation(game) }
        )
        rvGames.adapter = gamesAdapter
        rvGames.layoutManager = LinearLayoutManager(this)
    }

    private fun setupDropdowns() {
        // Agregar opción "Todos" al inicio
        val genreOptions = arrayOf("Todos") + genres
        val platformOptions = arrayOf("Todos") + platforms

        val genreAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genreOptions)
        spinnerGenre.setAdapter(genreAdapter)

        val platformAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, platformOptions)
        spinnerPlatform.setAdapter(platformAdapter)
    }

    private fun setupSearchAndFilters() {
        // Búsqueda en tiempo real
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s.toString()
                applyFilters()
            }
        })

        // Filtros de dropdown
        spinnerGenre.setOnItemClickListener { _, _, _, _ ->
            currentGenreFilter = if (spinnerGenre.text.toString() == "Todos") "" else spinnerGenre.text.toString()
            applyFilters()
        }

        spinnerPlatform.setOnItemClickListener { _, _, _, _ ->
            currentPlatformFilter = if (spinnerPlatform.text.toString() == "Todos") "" else spinnerPlatform.text.toString()
            applyFilters()
        }
    }

    private fun setupClickListeners() {
        toolbar.setNavigationOnClickListener { finish() }

        fabAddGame.setOnClickListener {
            val intent = Intent(this, AddGameActivity::class.java)
            startActivity(intent)
        }

        btnFilterCompleted.setOnClickListener {
            currentCompletedFilter = true
            updateFilterButtons()
            applyFilters()
        }

        btnFilterPending.setOnClickListener {
            currentCompletedFilter = false
            updateFilterButtons()
            applyFilters()
        }

        btnClearFilters.setOnClickListener {
            clearAllFilters()
        }
    }

    private fun updateFilterButtons() {
        btnFilterCompleted.isSelected = currentCompletedFilter == true
        btnFilterPending.isSelected = currentCompletedFilter == false
    }

    private fun clearAllFilters() {
        currentSearchQuery = ""
        currentGenreFilter = ""
        currentPlatformFilter = ""
        currentCompletedFilter = null

        etSearch.setText("")
        spinnerGenre.setText("")
        spinnerPlatform.setText("")

        updateFilterButtons()
        applyFilters()
    }

    private fun applyFilters() {
        Log.d("GamesListActivity", "applyFilters llamado - gamesList.size: ${gamesList.size}")
        Log.d("GamesListActivity", "Filtros actuales - Search: '$currentSearchQuery', Genre: '$currentGenreFilter', Platform: '$currentPlatformFilter', Completed: $currentCompletedFilter")

        // Filtrar directamente desde gamesList (los datos originales de Firebase)
        val filtered = gamesList.filter { game ->
            val matchesSearch = currentSearchQuery.isEmpty() ||
                game.title.contains(currentSearchQuery, ignoreCase = true) ||
                game.description.contains(currentSearchQuery, ignoreCase = true)

            val matchesGenre = currentGenreFilter.isEmpty() || game.genre == currentGenreFilter
            val matchesPlatform = currentPlatformFilter.isEmpty() || game.platform == currentPlatformFilter
            val matchesCompleted = currentCompletedFilter == null || game.completed == currentCompletedFilter

            matchesSearch && matchesGenre && matchesPlatform && matchesCompleted
        }

        Log.d("GamesListActivity", "Juegos después del filtro: ${filtered.size}")

        // Debug: Imprimir títulos de juegos filtrados
        filtered.forEach { game ->
            Log.d("GamesListActivity", "Juego filtrado: ${game.title}")
        }

        filteredGamesList.clear()
        filteredGamesList.addAll(filtered)

        Log.d("GamesListActivity", "filteredGamesList después de clear y addAll: ${filteredGamesList.size}")

        // Debug: Verificar que los juegos estén en filteredGamesList
        filteredGamesList.forEach { game ->
            Log.d("GamesListActivity", "En filteredGamesList: ${game.title}")
        }

        gamesAdapter.updateGames(filteredGamesList)

        Log.d("GamesListActivity", "filteredGamesList.size final: ${filteredGamesList.size}")

        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (filteredGamesList.isEmpty()) {
            rvGames.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvGames.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun loadGames() {
        val currentUser = auth.currentUser ?: return

        Log.d("GamesListActivity", "Iniciando carga de juegos para usuario: ${currentUser.uid}")

        // Versión simplificada sin filtros complejos
        database.child("games")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("GamesListActivity", "onDataChange llamado - snapshot existe: ${snapshot.exists()}")
                    Log.d("GamesListActivity", "Número total de juegos en DB: ${snapshot.childrenCount}")

                    gamesList.clear()
                    for (gameSnapshot in snapshot.children) {
                        val game = gameSnapshot.getValue(Game::class.java)
                        if (game != null && game.userId == currentUser.uid) {
                            Log.d("GamesListActivity", "Juego del usuario encontrado: ${game.title}")
                            gamesList.add(game)
                        } else if (game != null) {
                            Log.d("GamesListActivity", "Juego de otro usuario: ${game.title} (${game.userId})")
                        }
                    }

                    Log.d("GamesListActivity", "Total juegos del usuario actual: ${gamesList.size}")

                    // Llamar a applyFilters() en lugar de actualizar directamente
                    // Esto asegura que se mantenga la lógica de filtrado
                    applyFilters()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("GamesListActivity", "Error al cargar juegos: ${error.message}")
                    Toast.makeText(this@GamesListActivity,
                        "Error al cargar juegos: ${error.message}",
                        Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun editGame(game: Game) {
        val intent = Intent(this, EditGameActivity::class.java)
        intent.putExtra("gameId", game.id)
        startActivity(intent)
    }

    private fun showDeleteConfirmation(game: Game) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar juego")
            .setMessage("¿Estás seguro de que quieres eliminar \"${game.title}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteGame(game)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteGame(game: Game) {
        database.child("games").child(game.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Juego eliminado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al eliminar: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        // Comentar temporalmente para evitar llamadas duplicadas
        // loadGames()
    }
}
