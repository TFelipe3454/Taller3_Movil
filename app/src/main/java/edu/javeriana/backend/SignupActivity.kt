package edu.javeriana.backend

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import edu.javeriana.backend.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {

    private lateinit var b: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()

        b.btnCreate.setOnClickListener {
            val name = b.etName.text.toString().trim()
            val email = b.etEmail.text.toString().trim()
            val pass = b.etPassword.text.toString().trim()

            if (!validateForm(name, email, pass)) return@setOnClickListener

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // actualizar displayName (photoUri se puede agregar luego)
                        val user = auth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            // .setPhotoUri(Uri.parse("uri-de-prueba")) // opcional
                            .build()

                        user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                            // Lanzar bienvenida
                            val i = Intent(this, WelcomeActivity::class.java)
                            i.putExtra("user", user.email)
                            startActivity(i)
                            finish()
                        }
                    } else {
                        Toast.makeText(this, "Registro falló: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun validateForm(name: String, email: String, password: String): Boolean {
        var valid = true
        if (TextUtils.isEmpty(name)) {
            b.etName.error = "Requerido."
            valid = false
        }
        if (TextUtils.isEmpty(email)) {
            b.etEmail.error = "Requerido."
            valid = false
        }
        if (TextUtils.isEmpty(password) || password.length < 6) {
            b.etPassword.error = "Mínimo 6 caracteres"
            valid = false
        }
        return valid
    }
}