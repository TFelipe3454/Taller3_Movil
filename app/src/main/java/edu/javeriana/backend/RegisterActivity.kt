package edu.javeriana.backend

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import edu.javeriana.backend.databinding.ActivityRegisterBinding
import edu.javeriana.backend.model.AppUser

class RegisterActivity : AppCompatActivity() {

    private lateinit var b: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private lateinit var fusedLocation: FusedLocationProviderClient
    private var lastLat: Double? = null
    private var lastLon: Double? = null

    private var pickedImageUri: Uri? = null

    // Abrir galería (puedes integrar la cámara de tu Taller #2 si quieres)
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pickedImageUri = uri      // HD: usaremos el archivo original
            b.ivPreview.setImageURI(uri)
        }
    }

    // Permisos de ubicación + media (Android 13+)
    private val reqPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> fetchLastLocation() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()
        fusedLocation = LocationServices.getFusedLocationProviderClient(this)

        requestRuntimePermissions()

        b.btnPickImage.setOnClickListener { pickImage.launch("image/*") }

        b.btnRegister.setOnClickListener {
            val nombre = b.etNombre.text.toString().trim()
            val apellido = b.etApellido.text.toString().trim()
            val email = b.etEmail.text.toString().trim()
            val pass = b.etPassword.text.toString().trim()
            val idNumber = b.etIdNumber.text.toString().trim()

            if (nombre.isEmpty() || apellido.isEmpty() || email.isEmpty() || pass.isEmpty() || idNumber.isEmpty()) {
                toast("Completa todos los campos")
                return@setOnClickListener
            }
            if (pickedImageUri == null) {
                toast("Selecciona una imagen de perfil")
                return@setOnClickListener
            }

            // 1) Crear usuario en Auth
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { cred ->
                    val uid = cred.user!!.uid
                    // 2) Subir imagen HD (sin comprimir) a Storage
                    uploadProfileImage(uid, pickedImageUri!!) { imageUrl ->
                        // 3) Guardar datos secundarios en RTDB
                        val user = AppUser(
                            uid = uid,
                            nombre = nombre,
                            apellido = apellido,
                            email = email,
                            idNumber = idNumber,
                            imageUrl = imageUrl,
                            lat = lastLat,
                            lon = lastLon
                        )
                        db.child("users").child(uid).setValue(user)
                            .addOnSuccessListener {
                                toast("¡Registro completado!")
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e -> toast("DB error: ${e.message}") }
                    }
                }
                .addOnFailureListener { e -> toast("Auth error: ${e.message}") }
        }
    }

    private fun uploadProfileImage(uid: String, uri: Uri, onDone: (String) -> Unit) {
        val imageRef = storage.reference.child("images/profile/$uid.jpg")
        imageRef.putFile(uri) // sube el archivo original → alta resolución
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { onDone(it.toString()) }
                    .addOnFailureListener { e -> toast("URL error: ${e.message}") }
            }
            .addOnFailureListener { e -> toast("Upload error: ${e.message}") }
    }

    private fun requestRuntimePermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 33) {
            perms += Manifest.permission.READ_MEDIA_IMAGES
        }
        reqPerms.launch(perms.toTypedArray())
    }

    @SuppressLint("MissingPermission")
    private fun fetchLastLocation() {
        try {
            fusedLocation.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        lastLat = loc.latitude
                        lastLon = loc.longitude
                    }
                }
        } catch (_: SecurityException) {}
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}