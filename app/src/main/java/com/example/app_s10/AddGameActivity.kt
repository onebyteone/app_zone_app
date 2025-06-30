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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AddGameActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

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
        setContentView(R.layout.activity_add_game)

        // Inicializar Firebase
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Inicializar views
        initViews()
        setupDropdowns()
        setupClickListeners()
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
    }

    private fun setupDropdowns() {
        // Configurar dropdown de géneros
        val genreAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genres)
        etGenre.setAdapter(genreAdapter)

        // Configurar dropdown de plataformas
        val platformAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, platforms)
        etPlatform.setAdapter(platformAdapter)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveGame()
        }
    }

    private fun saveGame() {
        // Validar campos requeridos
        val title = etTitle.text.toString().trim()
        val genre = etGenre.text.toString().trim()
        val platform = etPlatform.text.toString().trim()
        val releaseYearStr = etReleaseYear.text.toString().trim()
        val rating = rbRating.rating
        val description = etDescription.text.toString().trim()
        val completed = cbCompleted.isChecked

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

        // Obtener usuario actual
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear objeto Game
        val gameId = database.child("games").push().key ?: return
        val game = Game(
            id = gameId,
            title = title,
            genre = genre,
            platform = platform,
            rating = rating,
            description = description,
            releaseYear = releaseYear,
            completed = completed,
            userId = currentUser.uid
        )

        // Guardar en Firebase
        btnSave.isEnabled = false
        btnSave.text = "Guardando..."

        database.child("games").child(gameId).setValue(game)
            .addOnSuccessListener {
                Toast.makeText(this, "Juego guardado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al guardar: ${exception.message}", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
                btnSave.text = "Guardar"
            }
    }
}
