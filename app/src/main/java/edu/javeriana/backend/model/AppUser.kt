package edu.javeriana.backend.model

data class AppUser(
    var uid: String = "",
    var nombre: String = "",
    var apellido: String = "",
    var email: String = "",
    var idNumber: String = "",
    var imageUrl: String = "",
    var lat: Double? = null,
    var lon: Double? = null,
    var status: String = "desconectado"
)