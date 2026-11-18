package com.example.control_academico

/** Sesión en memoria del usuario autenticado. El DatabaseHelper la consulta para validar permisos. */
object Sesion {
    const val ROL_DOCENTE = "DOCENTE"
    const val ROL_ESTUDIANTE = "ESTUDIANTE"

    var usuarioId: Int = -1
        private set
    var nombre: String = ""
        private set
    var correo: String = ""
        private set
    var rol: String = ""
        private set

    val activa: Boolean get() = usuarioId != -1
    val esDocente: Boolean get() = activa && rol == ROL_DOCENTE
    val esEstudiante: Boolean get() = activa && rol == ROL_ESTUDIANTE

    fun iniciar(id: Int, nombre: String, correo: String, rol: String) {
        this.usuarioId = id
        this.nombre = nombre
        this.correo = correo
        this.rol = rol
    }

    fun cerrar() {
        usuarioId = -1
        nombre = ""
        correo = ""
        rol = ""
    }
}
