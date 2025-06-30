package com.example.app_s10

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class GamesAdapter(
    private val onEditClick: (Game) -> Unit,
    private val onDeleteClick: (Game) -> Unit
) : RecyclerView.Adapter<GamesAdapter.GameViewHolder>() {

    private var games: MutableList<Game> = mutableListOf()

    class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvGenre: TextView = view.findViewById(R.id.tvGenre)
        val tvPlatform: TextView = view.findViewById(R.id.tvPlatform)
        val rbRating: RatingBar = view.findViewById(R.id.rbRating)
        val tvRating: TextView = view.findViewById(R.id.tvRating)
        val tvYear: TextView = view.findViewById(R.id.tvYear)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val ivCompleted: ImageView = view.findViewById(R.id.ivCompleted)
        val btnEdit: MaterialButton = view.findViewById(R.id.btnEdit)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_game, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = games[position]

        holder.tvTitle.text = game.title
        holder.tvGenre.text = game.genre
        holder.tvPlatform.text = game.platform
        holder.rbRating.rating = game.rating
        holder.tvRating.text = String.format("%.1f", game.rating)
        holder.tvYear.text = if (game.releaseYear > 0) game.releaseYear.toString() else "N/A"
        holder.tvDescription.text = game.description.ifEmpty { "Sin descripción" }

        // Mostrar indicador de completado
        holder.ivCompleted.visibility = if (game.completed) View.VISIBLE else View.GONE

        // Click listeners
        holder.btnEdit.setOnClickListener { onEditClick(game) }
        holder.btnDelete.setOnClickListener { onDeleteClick(game) }
    }

    override fun getItemCount(): Int = games.size

    fun updateGames(newGames: List<Game>) {
        Log.d("GamesAdapter", "updateGames llamado con ${newGames.size} juegos")

        // Crear una nueva lista completamente
        games = newGames.toMutableList()

        Log.d("GamesAdapter", "Después de actualizar: games.size = ${games.size}")

        // Debug: Verificar que los juegos se copiaron correctamente
        games.forEach { game ->
            Log.d("GamesAdapter", "Juego en adapter: ${game.title}")
        }

        notifyDataSetChanged()
        Log.d("GamesAdapter", "notifyDataSetChanged() llamado")
    }

    fun filterGames(
        searchQuery: String = "",
        genreFilter: String = "",
        platformFilter: String = "",
        completedFilter: Boolean? = null
    ): List<Game> {
        return games.filter { game ->
            val matchesSearch = searchQuery.isEmpty() ||
                game.title.contains(searchQuery, ignoreCase = true) ||
                game.description.contains(searchQuery, ignoreCase = true)

            val matchesGenre = genreFilter.isEmpty() || game.genre == genreFilter
            val matchesPlatform = platformFilter.isEmpty() || game.platform == platformFilter
            val matchesCompleted = completedFilter == null || game.completed == completedFilter

            matchesSearch && matchesGenre && matchesPlatform && matchesCompleted
        }
    }
}
