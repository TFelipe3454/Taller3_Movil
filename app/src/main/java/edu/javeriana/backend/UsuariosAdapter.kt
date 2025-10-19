package edu.javeriana.backend

import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import edu.javeriana.backend.databinding.ItemUsuarioBinding
import edu.javeriana.backend.model.AppUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class UsuariosAdapter(
    private val users: List<AppUser>
) : RecyclerView.Adapter<UsuariosAdapter.ViewHolder>() {

    inner class ViewHolder(val b: ItemUsuarioBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUsuarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.b.tvName.text = "${user.nombre} ${user.apellido}"

        // Cargar imagen desde URL sin Glide
        if (!user.imageUrl.isNullOrEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val stream = URL(user.imageUrl).openStream()
                    val bitmap = BitmapFactory.decodeStream(stream)
                    withContext(Dispatchers.Main) {
                        holder.b.ivAvatar.setImageBitmap(bitmap)
                    }
                } catch (_: Exception) { }
            }
        }

        // 🔹 Al presionar el botón, abrir MapsActivity siguiendo al usuario
        holder.b.btnFollow.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, MapsActivity::class.java)
            intent.putExtra("uid_seguir", user.uid)
            context.startActivity(intent)
        }
    }
}




