
package com.example.control_academico

import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast

class EstudiantesActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etIdentificacion: EditText
    private lateinit var etCurso: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etBuscar: EditText

    private lateinit var btnGuardar: Button
    private lateinit var btnLimpiar: Button
    private lateinit var btnBuscar: Button
    private lateinit var btnMostrarTodos: Button

    private lateinit var lvEstudiantes: ListView
    private lateinit var adapter: ArrayAdapter<String>

    private val listaEstudiantes = mutableListOf<Estudiante>()

    private var estudianteSeleccionadoId: Int? = null

    data class Estudiante(
        val id: Int,
        val nombres: String,
        val apellidos: String,
        val identificacion: String,
        val curso: String,
        val correo: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Sesion.esDocente) { finish(); return }
        setContentView(R.layout.activity_estudiantes)

        dbHelper = DatabaseHelper(this)

        inicializarComponentes()
        configurarEventos()
        cargarEstudiantes()
    }

    private fun inicializarComponentes() {

        etNombres = findViewById(R.id.etNombres)
        etApellidos = findViewById(R.id.etApellidos)
        etIdentificacion = findViewById(R.id.etIdentificacion)
        etCurso = findViewById(R.id.etCurso)
        etCorreo = findViewById(R.id.etCorreoEstudiante)
        etBuscar = findViewById(R.id.etBuscar)

        btnGuardar = findViewById(R.id.btnGuardarEstudiante)
        btnLimpiar = findViewById(R.id.btnLimpiar)
        btnBuscar = findViewById(R.id.btnBuscar)
        btnMostrarTodos = findViewById(R.id.btnMostrarTodos)

        lvEstudiantes = findViewById(R.id.lvEstudiantes)

        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf<String>()
        )

        lvEstudiantes.adapter = adapter
    }

    private fun configurarEventos() {

        btnGuardar.setOnClickListener {
            guardarEstudiante()
        }

        btnLimpiar.setOnClickListener {
            limpiarFormulario()
        }

        btnBuscar.setOnClickListener {
            buscarEstudiantes()
        }

        btnMostrarTodos.setOnClickListener {
            etBuscar.text.clear()
            cargarEstudiantes()
        }

        lvEstudiantes.setOnItemClickListener { _, _, position, _ ->

            val estudiante = listaEstudiantes[position]

            estudianteSeleccionadoId = estudiante.id

            etNombres.setText(estudiante.nombres)
            etApellidos.setText(estudiante.apellidos)
            etIdentificacion.setText(estudiante.identificacion)
            etCurso.setText(estudiante.curso)
            etCorreo.setText(estudiante.correo)

            btnGuardar.text = "ACTUALIZAR ESTUDIANTE"

            Toast.makeText(
                this,
                "Estudiante seleccionado para editar",
                Toast.LENGTH_SHORT
            ).show()
        }

        lvEstudiantes.setOnItemLongClickListener { _, _, position, _ ->

            val estudiante = listaEstudiantes[position]

            confirmarEliminacion(estudiante)

            true
        }
    }

    private fun guardarEstudiante() {

        val nombres = etNombres.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val identificacion = etIdentificacion.text.toString().trim()
        val curso = etCurso.text.toString().trim()
        val correo = etCorreo.text.toString().trim()

        if (
            nombres.isEmpty() ||
            apellidos.isEmpty() ||
            identificacion.isEmpty() ||
            curso.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Completa los campos obligatorios",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val guardado: Boolean

        if (estudianteSeleccionadoId == null) {

            guardado = dbHelper.insertarEstudiante(
                nombres,
                apellidos,
                identificacion,
                curso,
                correo
            )

        } else {

            guardado = dbHelper.actualizarEstudiante(
                estudianteSeleccionadoId!!,
                nombres,
                apellidos,
                identificacion,
                curso,
                correo
            )
        }

        if (guardado) {

            Toast.makeText(
                this,
                if (estudianteSeleccionadoId == null)
                    "Estudiante registrado correctamente"
                else
                    "Estudiante actualizado correctamente",
                Toast.LENGTH_SHORT
            ).show()

            limpiarFormulario()
            cargarEstudiantes()

        } else {

            Toast.makeText(
                this,
                "No se pudo guardar. Verifica que la identificación no esté duplicada.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun cargarEstudiantes() {

        val cursor = dbHelper.obtenerEstudiantes()

        val estudiantes = mutableListOf<Estudiante>()

        cursor.use {
            while (it.moveToNext()) {

                estudiantes.add(
                    Estudiante(
                        id = it.getInt(it.getColumnIndexOrThrow("id")),
                        nombres = it.getString(
                            it.getColumnIndexOrThrow("nombres")
                        ),
                        apellidos = it.getString(
                            it.getColumnIndexOrThrow("apellidos")
                        ),
                        identificacion = it.getString(
                            it.getColumnIndexOrThrow("identificacion")
                        ),
                        curso = it.getString(
                            it.getColumnIndexOrThrow("curso")
                        ),
                        correo = it.getString(
                            it.getColumnIndexOrThrow("correo")
                        ) ?: ""
                    )
                )
            }
        }

        mostrarEnLista(estudiantes)
    }

    private fun buscarEstudiantes() {

        val texto = etBuscar.text.toString().trim()

        if (texto.isEmpty()) {
            cargarEstudiantes()
            return
        }

        val cursor = dbHelper.buscarEstudiantes(texto)

        val estudiantes = mutableListOf<Estudiante>()

        cursor.use {
            while (it.moveToNext()) {

                estudiantes.add(
                    Estudiante(
                        id = it.getInt(it.getColumnIndexOrThrow("id")),
                        nombres = it.getString(
                            it.getColumnIndexOrThrow("nombres")
                        ),
                        apellidos = it.getString(
                            it.getColumnIndexOrThrow("apellidos")
                        ),
                        identificacion = it.getString(
                            it.getColumnIndexOrThrow("identificacion")
                        ),
                        curso = it.getString(
                            it.getColumnIndexOrThrow("curso")
                        ),
                        correo = it.getString(
                            it.getColumnIndexOrThrow("correo")
                        ) ?: ""
                    )
                )
            }
        }

        mostrarEnLista(estudiantes)

        if (estudiantes.isEmpty()) {
            Toast.makeText(
                this,
                "No se encontraron estudiantes",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarEnLista(estudiantes: List<Estudiante>) {

        listaEstudiantes.clear()
        listaEstudiantes.addAll(estudiantes)

        adapter.clear()

        estudiantes.forEach { estudiante ->

            adapter.add(
                """
                ${estudiante.nombres} ${estudiante.apellidos}
                Identificación: ${estudiante.identificacion}
                Curso: ${estudiante.curso}
                Correo: ${estudiante.correo.ifEmpty { "No registrado" }}
                """.trimIndent()
            )
        }

        adapter.notifyDataSetChanged()
    }

    private fun confirmarEliminacion(estudiante: Estudiante) {

        AlertDialog.Builder(this)
            .setTitle("Eliminar estudiante")
            .setMessage(
                "¿Deseas eliminar a ${estudiante.nombres} ${estudiante.apellidos}?"
            )
            .setPositiveButton("ELIMINAR") { _, _ ->

                val eliminado = dbHelper.eliminarEstudiante(estudiante.id)

                if (eliminado) {

                    Toast.makeText(
                        this,
                        "Estudiante eliminado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    limpiarFormulario()
                    cargarEstudiantes()

                } else {

                    Toast.makeText(
                        this,
                        "No se pudo eliminar el estudiante",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun limpiarFormulario() {

        etNombres.text.clear()
        etApellidos.text.clear()
        etIdentificacion.text.clear()
        etCurso.text.clear()
        etCorreo.text.clear()

        estudianteSeleccionadoId = null

        btnGuardar.text = "GUARDAR ESTUDIANTE"
    }
}
