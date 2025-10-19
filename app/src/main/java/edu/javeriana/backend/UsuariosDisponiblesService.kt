package edu.javeriana.backend

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.google.firebase.database.*
import edu.javeriana.backend.model.AppUser

class UsuariosDisponiblesService : Service() {

    private lateinit var db: DatabaseReference
    private lateinit var listener: ChildEventListener
    private val usuariosDisponibles = mutableMapOf<String, AppUser>()

    override fun onCreate() {
        super.onCreate()

        // 🔹 Conectarse a la base de datos
        db = FirebaseDatabase.getInstance("https://icm2025-87fbd-default-rtdb.firebaseio.com/")
            .getReference("users")

        // 🔹 Listener para escuchar cambios en los usuarios
        listener = db.addChildEventListener(object : ChildEventListener {

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val user = snapshot.getValue(AppUser::class.java)
                if (user != null && user.status == "disponible") {
                    usuariosDisponibles[user.uid] = user
                    Toast.makeText(
                        applicationContext,
                        "${user.nombre} se ha conectado",
                        Toast.LENGTH_SHORT
                    ).show()
                    sendBroadcast(Intent("USUARIOS_ACTUALIZADOS"))
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val user = snapshot.getValue(AppUser::class.java) ?: return
                val estabaDisponible = usuariosDisponibles.containsKey(user.uid)

                if (user.status == "disponible" && !estabaDisponible) {
                    usuariosDisponibles[user.uid] = user
                    Toast.makeText(
                        applicationContext,
                        "${user.nombre} se ha conectado",
                        Toast.LENGTH_SHORT
                    ).show()
                    sendBroadcast(Intent("USUARIOS_ACTUALIZADOS"))
                } else if (user.status != "disponible" && estabaDisponible) {
                    usuariosDisponibles.remove(user.uid)
                    Toast.makeText(
                        applicationContext,
                        "${user.nombre} se ha desconectado",
                        Toast.LENGTH_SHORT
                    ).show()
                    sendBroadcast(Intent("USUARIOS_ACTUALIZADOS"))
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        db.removeEventListener(listener)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

