package com.example.control_academico

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        // Conectar campos
        val usuario = findViewById<EditText>(R.id.etUsuario)
        val contrasena = findViewById<EditText>(R.id.etContrasena)

        // Conectar botones
        val ingresar = findViewById<Button>(R.id.btnIngresar)
        val crearCuenta = findViewById<Button>(R.id.btnCrearCuenta)

        // BOTÓN CREAR CUENTA
        crearCuenta.setOnClickListener {

            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)

        }

        // BOTÓN INGRESAR
        ingresar.setOnClickListener {

            val correoTexto = usuario.text.toString().trim()
            val claveTexto = contrasena.text.toString()

            if (correoTexto.isEmpty()) {

                usuario.error = "Ingresa tu correo"
                usuario.requestFocus()
                return@setOnClickListener

            }

            if (claveTexto.isEmpty()) {

                contrasena.error = "Ingresa tu contraseña"
                contrasena.requestFocus()
                return@setOnClickListener

            }

            if (dbHelper.iniciarSesion(correoTexto, claveTexto)) {
                val destino = if (Sesion.esDocente)
                    DashboardActivity::class.java
                else
                    EstudianteActivity::class.java

                startActivity(Intent(this, destino))
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Correo o contraseña incorrectos",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::dbHelper.isInitialized) {
            dbHelper.close()
        }
    }
}
