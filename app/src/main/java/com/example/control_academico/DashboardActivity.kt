package com.example.control_academico

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Panel del DOCENTE: acceso completo a estudiantes, asignaturas, notas e informes. */
class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Sesion.esDocente) {
            irALogin()
            return
        }
        setContentView(R.layout.activity_dashboard)

        findViewById<TextView>(R.id.tvSaludo).text =
            "Hola, ${Sesion.nombre.substringBefore(' ')}"

        val destinos = mapOf(
            R.id.btnEstudiantes to EstudiantesActivity::class.java,
            R.id.btnAsignaturas to AsignaturasActivity::class.java,
            R.id.btnCalificaciones to CalificacionesActivity::class.java,
            R.id.btnReportes to InformesActivity::class.java
        )
        destinos.forEach { (id, clase) ->
            findViewById<android.view.View>(id).setOnClickListener {
                startActivity(Intent(this, clase))
            }
        }

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            Sesion.cerrar()
            irALogin()
        }
    }

    private fun irALogin() {
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
        finish()
    }
}
