package com.example.control_academico

import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast

class AsignaturasActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    private lateinit var etNombre: EditText
    private lateinit var etCodigo: EditText
    private lateinit var etGrado: EditText
    private lateinit var etDocente: EditText
    private lateinit var etBuscar: EditText

    private lateinit var btnGuardar: Button
    private lateinit var btnLimpiar: Button
    private lateinit var btnBuscar: Button
    private lateinit var btnMostrarTodas: Button

    private lateinit var lvAsignaturas: ListView

    private var idAsignaturaSeleccionada: Int = -1

    private val listaIds = ArrayList<Int>()
    private val listaTextos = ArrayList<String>()
    private lateinit var adaptador: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Sesion.esDocente) { finish(); return }
        setContentView(R.layout.activity_asignaturas)

        db = DatabaseHelper(this)

        etNombre = findViewById(R.id.etNombreAsignatura)
        etCodigo = findViewById(R.id.etCodigoAsignatura)
        etGrado = findViewById(R.id.etGradoAsignatura)
        etDocente = findViewById(R.id.etDocenteAsignatura)
        etBuscar = findViewById(R.id.etBuscarAsignatura)

        btnGuardar = findViewById(R.id.btnGuardarAsignatura)
        btnLimpiar = findViewById(R.id.btnLimpiarAsignatura)
        btnBuscar = findViewById(R.id.btnBuscarAsignatura)
        btnMostrarTodas = findViewById(R.id.btnMostrarTodas)

        lvAsignaturas = findViewById(R.id.lvAsignaturas)

        adaptador = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            listaTextos
        )

        lvAsignaturas.adapter = adaptador

        btnGuardar.setOnClickListener {
            guardarAsignatura()
        }

        btnLimpiar.setOnClickListener {
            limpiarFormulario()
        }

        btnBuscar.setOnClickListener {
            buscarAsignaturas()
        }

        btnMostrarTodas.setOnClickListener {
            etBuscar.text.clear()
            cargarAsignaturas()
        }

        lvAsignaturas.setOnItemClickListener { _, _, position, _ ->
            cargarEnFormulario(position)
        }

        lvAsignaturas.setOnItemLongClickListener { _, _, position, _ ->
            confirmarEliminar(position)
            true
        }

        cargarAsignaturas()
    }

    private fun guardarAsignatura() {
        val nombre = etNombre.text.toString().trim()
        val codigo = etCodigo.text.toString().trim()
        val grado = etGrado.text.toString().trim()
        val docente = etDocente.text.toString().trim()

        if (nombre.isEmpty() || codigo.isEmpty() ||
            grado.isEmpty() || docente.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Completa todos los campos",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val resultado: Boolean

            if (idAsignaturaSeleccionada == -1) {
                resultado = db.insertarAsignatura(
                    nombre, codigo, grado, docente
                )
            } else {
                resultado = db.actualizarAsignatura(
                    idAsignaturaSeleccionada,
                    nombre,
                    codigo,
                    grado,
                    docente
                )
            }

            if (resultado) {
                Toast.makeText(
                    this,
                    if (idAsignaturaSeleccionada == -1)
                        "Asignatura registrada correctamente"
                    else
                        "Asignatura actualizada correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                limpiarFormulario()
                cargarAsignaturas()
            } else {
                Toast.makeText(
                    this,
                    "No se pudo guardar. Verifica que el código no esté repetido.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error al guardar: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun cargarAsignaturas() {
        listaIds.clear()
        listaTextos.clear()

        val cursor = db.obtenerAsignaturas()

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                val nombre = it.getString(it.getColumnIndexOrThrow("nombre"))
                val codigo = it.getString(it.getColumnIndexOrThrow("codigo"))
                val grado = it.getString(it.getColumnIndexOrThrow("grado"))
                val docente = it.getString(it.getColumnIndexOrThrow("docente"))

                listaIds.add(id)

                listaTextos.add(
                    "Asignatura: $nombre\n" +
                            "Código: $codigo\n" +
                            "Grado: $grado\n" +
                            "Docente: $docente\n\n" +
                            "Toca para editar | Mantén presionado para eliminar"
                )
            }
        }

        adaptador.notifyDataSetChanged()
    }

    private fun buscarAsignaturas() {
        val texto = etBuscar.text.toString().trim()

        if (texto.isEmpty()) {
            Toast.makeText(
                this,
                "Escribe un nombre, código, grado o docente",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        listaIds.clear()
        listaTextos.clear()

        val cursor = db.buscarAsignaturas(texto)

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow("id"))
                val nombre = it.getString(it.getColumnIndexOrThrow("nombre"))
                val codigo = it.getString(it.getColumnIndexOrThrow("codigo"))
                val grado = it.getString(it.getColumnIndexOrThrow("grado"))
                val docente = it.getString(it.getColumnIndexOrThrow("docente"))

                listaIds.add(id)

                listaTextos.add(
                    "Asignatura: $nombre\n" +
                            "Código: $codigo\n" +
                            "Grado: $grado\n" +
                            "Docente: $docente\n\n" +
                            "Toca para editar | Mantén presionado para eliminar"
                )
            }
        }

        adaptador.notifyDataSetChanged()

        if (listaIds.isEmpty()) {
            Toast.makeText(
                this,
                "No se encontraron asignaturas",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun cargarEnFormulario(position: Int) {
        val id = listaIds[position]
        val cursor = db.obtenerAsignaturas()

        cursor.use {
            while (it.moveToNext()) {
                if (it.getInt(it.getColumnIndexOrThrow("id")) == id) {
                    etNombre.setText(
                        it.getString(it.getColumnIndexOrThrow("nombre"))
                    )
                    etCodigo.setText(
                        it.getString(it.getColumnIndexOrThrow("codigo"))
                    )
                    etGrado.setText(
                        it.getString(it.getColumnIndexOrThrow("grado"))
                    )
                    etDocente.setText(
                        it.getString(it.getColumnIndexOrThrow("docente"))
                    )

                    idAsignaturaSeleccionada = id
                    btnGuardar.text = "ACTUALIZAR ASIGNATURA"

                    Toast.makeText(
                        this,
                        "Edita los datos y pulsa ACTUALIZAR",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            }
        }
    }

    private fun confirmarEliminar(position: Int) {
        val id = listaIds[position]
        val nombre = listaTextos[position]
            .substringAfter("Asignatura: ")
            .substringBefore("\n")

        AlertDialog.Builder(this)
            .setTitle("Eliminar asignatura")
            .setMessage("¿Deseas eliminar la asignatura \"$nombre\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                val resultado = db.eliminarAsignatura(id)

                if (resultado) {
                    Toast.makeText(
                        this,
                        "Asignatura eliminada",
                        Toast.LENGTH_SHORT
                    ).show()

                    limpiarFormulario()
                    cargarAsignaturas()
                } else {
                    Toast.makeText(
                        this,
                        "No se pudo eliminar la asignatura",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun limpiarFormulario() {
        etNombre.text.clear()
        etCodigo.text.clear()
        etGrado.text.clear()
        etDocente.text.clear()

        idAsignaturaSeleccionada = -1
        btnGuardar.text = "GUARDAR ASIGNATURA"
    }
}
