package edu.javeriana.backend

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import edu.javeriana.backend.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()


        b.btnLogin.setOnClickListener {
            val email = b.etEmail.text.toString().trim()
            val pass = b.etPassword.text.toString().trim()

            if (!validateForm(email, pass)) return@setOnClickListener

            // Iniciar sesión con Firebase
            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Login exitoso -> lanzar pantalla del mapa
                        val i = Intent(this, MapsActivity::class.java)
                        i.putExtra("user", auth.currentUser?.email)
                        startActivity(i)
                        finish()
                    } else {
                        // Mostrar toast con error
                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        b.btnRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        // Si ya está autenticado, ir directo a WelcomeActivity
        if (currentUser != null) {
            startActivity(Intent(this, MapsActivity::class.java))
            finish()
        }
    }

    private fun validateForm(email: String, password: String): Boolean {
        var valid = true
        if (TextUtils.isEmpty(email)) {
            b.etEmail.error = "Required."
            valid = false
        } else {
            b.etEmail.error = null
        }
        if (TextUtils.isEmpty(password)) {
            b.etPassword.error = "Required."
            valid = false
        } else {
            b.etPassword.error = null
        }
        // validación simple de email
        if (valid && (!email.contains("@") || !email.contains(".") || email.length < 5)) {
            b.etEmail.error = "Email inválido"
            valid = false
        }
        return valid
    }

}