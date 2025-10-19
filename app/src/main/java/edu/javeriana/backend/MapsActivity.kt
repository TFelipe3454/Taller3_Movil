package edu.javeriana.backend

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import edu.javeriana.backend.databinding.ActivityMapsBinding
import org.json.JSONObject
import java.io.InputStream

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var b: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: DatabaseReference
    private val dbUrl = "https://icm2025-87fbd-default-rtdb.firebaseio.com/"

    private var markerSeguido: Marker? = null
    private lateinit var uidSeguir: String
    private var myLatLng: LatLng? = null

    private var myMarker: Marker? = null

    private lateinit var tvDistancia: TextView
    private var currentStatus: String = "desconectado"

    private var cameraMovedOnce = false
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance(dbUrl).reference
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        uidSeguir = intent.getStringExtra("uid_seguir") ?: ""
        tvDistancia = b.tvDistancia // asegúrate que existe en el XML

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupLocationUpdates()
    }

    private fun setupLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateDistanceMeters(1f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val uid = auth.currentUser?.uid ?: return
                myLatLng = LatLng(loc.latitude, loc.longitude)

                // ✅ Actualiza marcador sin borrar el mapa
                if (myMarker == null) {
                    myMarker = mMap.addMarker(
                        MarkerOptions()
                            .position(myLatLng!!)
                            .title("Tu ubicación")
                    )
                } else {
                    myMarker!!.position = myLatLng!!
                }

                // ✅ Actualiza texto con coordenadas
                b.tvDistancia.text = "Tu posición: ${loc.latitude.format(5)}, ${loc.longitude.format(5)}"

                // 🔵 Solo centra cámara la primera vez
                if (!cameraMovedOnce) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng!!, 13f))
                    cameraMovedOnce = true
                }

                // 🔴 Si sigues a alguien, actualiza distancia
                if (markerSeguido != null) {
                    updateDistance()
                }

                // 📡 Sube la ubicación a Firebase
                db.child("users").child(uid).updateChildren(
                    mapOf(
                        "lat" to loc.latitude,
                        "lon" to loc.longitude
                    )
                )
            }

        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    override fun onStart() {
        super.onStart()
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).child("status").onDisconnect().setValue("desconectado")
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocationOnMapIfGranted()

        // Cargar POIs locales
        val jsonString = loadJSONFromAssets("locations.json")
        if (jsonString.isNotBlank()) {
            val jsonObject = JSONObject(jsonString)
            val locationsArray = jsonObject.getJSONArray("locationsArray")
            for (i in 0 until locationsArray.length()) {
                val obj = locationsArray.getJSONObject(i)
                val lat = obj.getDouble("latitude")
                val lon = obj.getDouble("longitude")
                val name = obj.getString("name")
                mMap.addMarker(MarkerOptions().position(LatLng(lat, lon)).title(name))
            }
        }

        // Si se está siguiendo a alguien
        if (uidSeguir.isNotEmpty()) {
            db.child("users").child(uidSeguir)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val lat = snapshot.child("lat").getValue(Double::class.java) ?: return
                        val lon = snapshot.child("lon").getValue(Double::class.java) ?: return
                        val userLatLng = LatLng(lat, lon)

                        if (markerSeguido == null) {
                            markerSeguido = mMap.addMarker(
                                MarkerOptions().position(userLatLng).title("Usuario seguido")
                            )
                        } else {
                            markerSeguido!!.position = userLatLng
                        }

                        updateDistance()
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        val bogota = LatLng(4.65, -74.08)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bogota, 12f))
    }

    private fun loadJSONFromAssets(filename: String): String {
        return try {
            val inputStream: InputStream = assets.open(filename)
            inputStream.bufferedReader().use { it.readText() }
        } catch (ex: Exception) {
            toast("Error al leer JSON: ${ex.message}")
            ""
        }
    }

    private fun enableMyLocationOnMapIfGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        }
    }

    // ===== MENÚ =====
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_available -> { updateStatus("disponible"); true }
            R.id.action_disconnected -> { updateStatus("desconectado"); true }
            R.id.action_users -> {
                startActivity(Intent(this, UsuariosDisponiblesActivity::class.java))
                true
            }
            R.id.menuLogOut -> { logoutKeepingCurrentStatus(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateStatus(status: String) {
        val user = auth.currentUser ?: return
        currentStatus = status
        db.child("users").child(user.uid)
            .updateChildren(mapOf(
                "status" to status,
                "lastSeen" to System.currentTimeMillis()
            ))
            .addOnSuccessListener { toast("Estado: $status") }
            .addOnFailureListener { e -> toast("Error: ${e.message}") }
    }

    private fun logoutKeepingCurrentStatus() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        db.child("users").child(uid).child("lastSeen").setValue(System.currentTimeMillis())
            .addOnCompleteListener {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
    }
    // ===== FIN MENÚ =====

    private fun updateDistance() {
        myLatLng?.let { myLoc ->
            markerSeguido?.let { marker ->
                val distance = FloatArray(1)
                Location.distanceBetween(
                    myLoc.latitude, myLoc.longitude,
                    marker.position.latitude, marker.position.longitude,
                    distance
                )
                tvDistancia.text = "Distancia: ${distance[0].toInt()} m"
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            setupLocationUpdates()
            enableMyLocationOnMapIfGranted()
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
