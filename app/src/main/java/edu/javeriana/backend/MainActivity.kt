package edu.javeriana.backend

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import edu.javeriana.backend.databinding.ActivityMainBinding
import edu.javeriana.backend.model.AppUser
import kotlinx.coroutines.*
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private val scope = MainScope() // o lifecycleScope si lo prefieres

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference

        val uid = auth.currentUser?.uid
        if (uid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Cargar datos del usuario
        db.child("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(AppUser::class.java)
                    if (user != null) {
                        b.tvName.text = "${user.nombre} ${user.apellido}"
                        b.tvEmail.text = user.email
                        b.tvId.text = user.idNumber
                        b.tvCoord.text = "(${user.lat ?: "?"}, ${user.lon ?: "?"})"

                        // Cargar la imagen desde URL sin Glide
                        if (!user.imageUrl.isNullOrEmpty()) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    URL(user.imageUrl).openStream().use { stream ->
                                        val bmp = BitmapFactory.decodeStream(stream)
                                        withContext(Dispatchers.Main) {
                                            b.ivAvatar.setImageBitmap(bmp)
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        toast("No pude cargar la imagen: ${e.message}")
                                    }
                                }
                            }
                        }
                    } else {
                        toast("Usuario sin info en DB")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    toast("DB error: ${error.message}")
                }
            })

        // Si ya usarás el menú para cerrar sesión, puedes eliminar este botón:
        b.btnLogout.setOnClickListener {
            setDisconnectedThenLogout()
        }
    }

    override fun onStart() {
        super.onStart()
        // Si la app se cierra inesperadamente, marca desconectado
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("status").onDisconnect().setValue("desconectado")
        // Si quieres marcar disponible automáticamente al entrar, descomenta:
        // updateStatus("disponible")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // ---- MENÚ ----
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu) // res/menu/menu.xml
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_available -> {
                updateStatus("disponible")
                true
            }
            R.id.action_disconnected -> {
                updateStatus("desconectado")
                true
            }
            R.id.menuLogOut -> {
                setDisconnectedThenLogout()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // ---- HELPERS ----
    private fun updateStatus(status: String) {
        val uid = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "status" to status,
            "lastSeen" to System.currentTimeMillis() // opcional
        )
        db.child("users").child(uid).updateChildren(updates)
            .addOnSuccessListener { toast("Estado: $status") }
            .addOnFailureListener { e -> toast("No se pudo actualizar: ${e.message}") }
    }

    private fun setDisconnectedThenLogout() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.child("users").child(uid).child("status").setValue("desconectado")
        }
        auth.signOut()
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
