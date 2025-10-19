package edu.javeriana.backend

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import edu.javeriana.backend.model.AppUser

class UsuariosDisponiblesActivity : AppCompatActivity() {

    private lateinit var rvUsuarios: RecyclerView
    private lateinit var db: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // 🔹 Receptor para escuchar actualizaciones del servicio
    private val usuariosReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            cargarUsuariosDisponibles()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usuarios_disponibles)

        rvUsuarios = findViewById(R.id.rvUsuarios)
        rvUsuarios.layoutManager = LinearLayoutManager(this)

        // 🔹 Iniciar el servicio que escucha cambios en tiempo real
        val serviceIntent = Intent(this, UsuariosDisponiblesService::class.java)
        startService(serviceIntent)

        // 🔹 Registrar el receiver para recibir notificaciones del servicio
        val filter = IntentFilter("USUARIOS_ACTUALIZADOS")

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // 👇 Usa NOT_EXPORTED porque el broadcast solo viene de dentro de la app
            registerReceiver(usuariosReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usuariosReceiver, filter)
        }

        // 🔹 Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance("https://icm2025-87fbd-default-rtdb.firebaseio.com/")
            .getReference("users")

        // 🔹 Cargar inicialmente los usuarios disponibles
        cargarUsuariosDisponibles()
    }

    private fun cargarUsuariosDisponibles() {
        db.orderByChild("status").equalTo("disponible")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lista = mutableListOf<AppUser>()
                    for (userSnap in snapshot.children) {
                        val u = userSnap.getValue(AppUser::class.java)
                        if (u != null && u.uid != auth.currentUser?.uid) {
                            lista.add(u)
                        }
                    }

                    Log.d("UsuariosDisponibles", "Usuarios encontrados: ${lista.size}")
                    if (lista.isEmpty()) {
                        Toast.makeText(this@UsuariosDisponiblesActivity, "No hay usuarios disponibles", Toast.LENGTH_SHORT).show()
                    }

                    rvUsuarios.adapter = UsuariosAdapter(lista)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@UsuariosDisponiblesActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        // 🔹 Importante: desregistrar el receiver
        unregisterReceiver(usuariosReceiver)
    }
}
