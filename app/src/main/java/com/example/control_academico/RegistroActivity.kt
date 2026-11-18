package com.example.control_academico

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegistroActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        dbHelper = DatabaseHelper(this)

        val nombre = findViewById<EditText>(R.id.etNombreRegistro)
        val correo = findViewById<EditText>(R.id.etCorreoRegistro)
        val contrasena = findViewById<EditText>(R.id.etContrasenaRegistro)
        val confirmar = findViewById<EditText>(R.id.etConfirmarContrasena)
        val crearCuenta = findViewById<Button>(R.id.btnCrearCuenta)
        val rgRol = findViewById<RadioGroup>(R.id.rgRol)
        val codigo = findViewById<EditText>(R.id.etCodigoDocente)
        findViewById<Button>(R.id.btnVolver).setOnClickListener { finish() }

        rgRol.setOnCheckedChangeListener { _, id ->
            codigo.visibility =
                if (id == R.id.rbDocente) View.VISIBLE else View.GONE
        }

        crearCuenta.setOnClickListener {

            val nombreTexto = nombre.text.toString().trim()
            val correoTexto = correo.text.toString().trim()
            val contrasenaTexto = contrasena.text.toString()
            val confirmarTexto = confirmar.text.toString()

            if (nombreTexto.isEmpty() ||
                correoTexto.isEmpty() ||
                contrasenaTexto.isEmpty() ||
                confirmarTexto.isEmpty()
            ) {
                Toast.makeText(
                    this,
                    "Completa todos los campos",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(correoTexto).matches()) {
                correo.error = "Correo no válido"
                correo.requestFocus()
                return@setOnClickListener
            }

            if (contrasenaTexto.length < 6) {
                contrasena.error = "Mínimo 6 caracteres"
                contrasena.requestFocus()
                return@setOnClickListener
            }

            if (contrasenaTexto != confirmarTexto) {
                confirmar.error = "Las contraseñas no coinciden"
                confirmar.requestFocus()
                return@setOnClickListener
            }

            val esDocente = rgRol.checkedRadioButtonId == R.id.rbDocente
            val rol = if (esDocente) Sesion.ROL_DOCENTE else Sesion.ROL_ESTUDIANTE

            val resultado = dbHelper.registrarUsuario(
                nombreTexto,
                correoTexto,
                contrasenaTexto,
                rol,
                codigo.text.toString()
            )

            if (resultado == DatabaseHelper.ResultadoRegistro.CODIGO_INVALIDO) {
                codigo.error = "Código de acceso incorrecto"
                codigo.requestFocus()
                return@setOnClickListener
            }

            if (resultado == DatabaseHelper.ResultadoRegistro.OK) {

                Toast.makeText(
                    this,
                    "Cuenta creada correctamente",
                    Toast.LENGTH_LONG
                ).show()

                finish()

            } else {

                Toast.makeText(
                    this,
                    "El correo ya existe o ocurrió un error",
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
