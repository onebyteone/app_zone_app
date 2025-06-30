package com.example.app_s10

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EditGameActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var gameId: String = ""
    private var currentGame: Game? = null

    // Views
    private lateinit var btnBack: ImageView
    private lateinit var etTitle: TextInputEditText
    private lateinit var etGenre: MaterialAutoCompleteTextView
    private lateinit var etPlatform: MaterialAutoCompleteTextView
    private lateinit var etReleaseYear: TextInputEditText
    private lateinit var rbRating: RatingBar
    private lateinit var etDescription: TextInputEditText
    private lateinit var cbCompleted: CheckBox
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSave: MaterialButton

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
        setContentView(R.layout.activity_add_game) // Reutilizamos el mismo layout

        // Obtener ID del juego
        gameId = intent.getStringExtra("gameId") ?: ""
        if (gameId.isEmpty()) {
            Toast.makeText(this, "Error: ID de juego no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicializar Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Inicializar views
        initViews()
        setupDropdowns()
        setupClickListeners()

        // Cargar datos del juego
        loadGameData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        etTitle = findViewById(R.id.etTitle)
        etGenre = findViewById(R.id.etGenre)
        etPlatform = findViewById(R.id.etPlatform)
        etReleaseYear = findViewById(R.id.etReleaseYear)
        rbRating = findViewById(R.id.rbRating)
        etDescription = findViewById(R.id.etDescription)
        cbCompleted = findViewById(R.id.cbCompleted)
        btnCancel = findViewById(R.id.btnCancel)
        btnSave = findViewById(R.id.btnSave)

        // Cambiar título de la actividad
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar?.title = "Editar Juego"
    }

    private fun setupDropdowns() {
        val genreAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genres)
        etGenre.setAdapter(genreAdapter)

        val platformAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, platforms)
        etPlatform.setAdapter(platformAdapter)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener { updateGame() }
    }

    private fun loadGameData() {
        database.child("games").child(gameId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val game = snapshot.getValue(Game::class.java)
                    if (game != null) {
                        currentGame = game
                        populateFields(game)
                    } else {
                        Toast.makeText(this@EditGameActivity, "Juego no encontrado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditGameActivity,
                        "Error al cargar juego: ${error.message}",
                        Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun populateFields(game: Game) {
        etTitle.setText(game.title)
        etGenre.setText(game.genre, false)
        etPlatform.setText(game.platform, false)
        if (game.releaseYear > 0) {
            etReleaseYear.setText(game.releaseYear.toString())
        }
        rbRating.rating = game.rating
        etDescription.setText(game.description)
        cbCompleted.isChecked = game.completed
    }

    private fun updateGame() {
        val title = etTitle.text.toString().trim()
        val genre = etGenre.text.toString().trim()
        val platform = etPlatform.text.toString().trim()
        val releaseYearStr = etReleaseYear.text.toString().trim()
        val rating = rbRating.rating
        val description = etDescription.text.toString().trim()
        val completed = cbCompleted.isChecked

        // Validaciones
        if (title.isEmpty()) {
            etTitle.error = "El título es requerido"
            etTitle.requestFocus()
            return
        }

        if (genre.isEmpty()) {
            etGenre.error = "El género es requerido"
            etGenre.requestFocus()
            return
        }

        if (platform.isEmpty()) {
            etPlatform.error = "La plataforma es requerida"
            etPlatform.requestFocus()
            return
        }

        val releaseYear = if (releaseYearStr.isNotEmpty()) {
            try {
                releaseYearStr.toInt()
            } catch (e: NumberFormatException) {
                etReleaseYear.error = "Año inválido"
                etReleaseYear.requestFocus()
                return
            }
        } else 0

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear objeto actualizado
        val updatedGame = currentGame?.copy(
            title = title,
            genre = genre,
            platform = platform,
            rating = rating,
            description = description,
            releaseYear = releaseYear,
            completed = completed
        ) ?: return

        // Actualizar en Firebase
        btnSave.isEnabled = false
        btnSave.text = "Actualizando..."

        database.child("games").child(gameId).setValue(updatedGame)
            .addOnSuccessListener {
                Toast.makeText(this, "Juego actualizado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al actualizar: ${exception.message}", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
                btnSave.text = "Guardar"
            }
    }
}
