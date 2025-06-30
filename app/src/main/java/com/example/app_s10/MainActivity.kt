package com.example.app_s10

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // Views
    private lateinit var tvWelcome: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnLogout: MaterialButton
    private lateinit var cardGames: CardView
    private lateinit var cardStats: CardView
    private lateinit var cardAchievements: CardView
    private lateinit var cardProfile: CardView
    private lateinit var fabAddGame: FloatingActionButton

    // Statistics
    private lateinit var tvTotalGamesCount: TextView
    private lateinit var tvCompletedGamesCount: TextView

    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Verificar autenticación
        val currentUser = auth.currentUser
        if (currentUser == null) {
            redirectToLogin()
            return
        }
        
        // Configurar UI
        setupUI()
        setupWindowInsets()
        
        // Cargar información del usuario
        loadUserInfo(currentUser)
        
        // Configurar listeners
        setupClickListeners()
        
        // Cargar estadísticas rápidas
        loadQuickStats()

        Log.d(TAG, "MainActivity iniciado para usuario: ${currentUser.email}")
    }
    
    private fun setupUI() {
        // Inicializar views
        tvWelcome = findViewById(R.id.tv_welcome)
        tvUserEmail = findViewById(R.id.tv_user_email)
        btnLogout = findViewById(R.id.btn_logout)
        cardGames = findViewById(R.id.card_games)
        cardStats = findViewById(R.id.card_stats)
        cardAchievements = findViewById(R.id.card_achievements)
        cardProfile = findViewById(R.id.card_profile)
        fabAddGame = findViewById(R.id.fab_add_game)
        tvTotalGamesCount = findViewById(R.id.tv_total_games_count)
        tvCompletedGamesCount = findViewById(R.id.tv_completed_games_count)
    }
    
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    
    private fun loadUserInfo(user: FirebaseUser) {
        // Personalizar saludo según el tipo de usuario
        val welcomeMessage = if (user.isAnonymous) {
            "¡Hola, Invitado!"
        } else {
            "¡Hola, ${user.displayName ?: "Gamer"}!"
        }
        
        tvWelcome.text = welcomeMessage
        
        // Mostrar email o indicar usuario anónimo
        tvUserEmail.text = if (user.isAnonymous) {
            "Usuario invitado"
        } else {
            user.email ?: "Sin email"
        }
        
        // Verificar estado de verificación de email
        if (!user.isAnonymous && user.email != null && !user.isEmailVerified) {
            showEmailVerificationReminder()
        }
    }
    
    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        cardGames.setOnClickListener {
            val intent = Intent(this, GamesListActivity::class.java)
            startActivity(intent)
        }
        
        cardStats.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }
        
        cardAchievements.setOnClickListener {
            Toast.makeText(this, "Logros - Próximamente disponible", Toast.LENGTH_SHORT).show()
        }
        
        cardProfile.setOnClickListener {
            Toast.makeText(this, "Perfil - Próximamente disponible", Toast.LENGTH_SHORT).show()
        }
        
        fabAddGame.setOnClickListener {
            val intent = Intent(this, AddGameActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadQuickStats() {
        val currentUser = auth.currentUser ?: return

        database.child("games")
            .orderByChild("userId")
            .equalTo(currentUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalGames = 0
                    var completedGames = 0

                    for (gameSnapshot in snapshot.children) {
                        val game = gameSnapshot.getValue(Game::class.java)
                        if (game != null) {
                            totalGames++
                            if (game.completed) {
                                completedGames++
                            }
                        }
                    }

                    tvTotalGamesCount.text = totalGames.toString()
                    tvCompletedGamesCount.text = completedGames.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading quick stats: ${error.message}")
                }
            })
    }

    private fun showEmailVerificationReminder() {
        AlertDialog.Builder(this)
            .setTitle("Verificar Email")
            .setMessage("Por favor verifica tu email para acceder a todas las funcionalidades.")
            .setPositiveButton("Enviar verificación") { _, _ ->
                sendEmailVerification()
            }
            .setNegativeButton("Después", null)
            .show()
    }
    
    private fun sendEmailVerification() {
        auth.currentUser?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Email de verificación enviado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al enviar verificación", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Cerrar Sesión") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun performLogout() {
        auth.signOut()
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        redirectToLogin()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Recargar estadísticas al volver a la actividad
        loadQuickStats()
    }
}