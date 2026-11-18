package com.example.control_academico

import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class CalificacionesActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    private lateinit var spGrado: Spinner
    private lateinit var spEstudiante: Spinner
    private lateinit var spAsignatura: Spinner
    private lateinit var spPeriodo: Spinner

    private lateinit var etActividad: EditText
    private lateinit var etNota: EditText
    private lateinit var etBuscar: EditText

    private lateinit var btnGuardar: Button
    private lateinit var btnLimpiar: Button
    private lateinit var btnBuscar: Button
    private lateinit var btnMostrarTodas: Button

    private lateinit var lvCalificaciones: ListView

    private lateinit var tvPromedioP1: TextView
    private lateinit var tvPromedioP2: TextView
    private lateinit var tvPromedioP3: TextView
    private lateinit var tvPromedioFinal: TextView

    private data class Opcion(
        val id: Int,
        val texto: String
    ) {
        override fun toString(): String = texto
    }

    private val grados = ArrayList<String>()
    private val estudiantes = ArrayList<Opcion>()
    private val asignaturas = ArrayList<Opcion>()

    private val listaIds = ArrayList<Int>()
    private val listaTextos = ArrayList<String>()

    private lateinit var adaptadorLista: ArrayAdapter<String>

    private var idCalificacionSeleccionada = -1
    private var cursoActual = ""

    private var cargandoEdicion = false

    private val periodos = arrayOf(
        "Periodo 1",
        "Periodo 2",
        "Periodo 3"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Sesion.esDocente) { finish(); return }

        setContentView(R.layout.activity_calificaciones)

        db = DatabaseHelper(this)

        enlazarComponentes()
        configurarAdaptadores()
        configurarEventos()

        cargarGrados()
        cargarCalificaciones()
        actualizarPromedios()
    }

    // =====================================
    // ENLAZAR COMPONENTES
    // =====================================

    private fun enlazarComponentes() {

        spGrado = findViewById(R.id.spGrado)
        spEstudiante = findViewById(R.id.spEstudiante)
        spAsignatura = findViewById(R.id.spAsignatura)
        spPeriodo = findViewById(R.id.spPeriodo)

        etActividad = findViewById(R.id.etActividad)
        etNota = findViewById(R.id.etNota)
        etBuscar = findViewById(R.id.etBuscarCalificacion)

        btnGuardar = findViewById(R.id.btnGuardarCalificacion)
        btnLimpiar = findViewById(R.id.btnLimpiarCalificacion)
        btnBuscar = findViewById(R.id.btnBuscarCalificacion)
        btnMostrarTodas = findViewById(R.id.btnMostrarCalificaciones)

        lvCalificaciones = findViewById(R.id.lvCalificaciones)

        tvPromedioP1 = findViewById(R.id.tvPromedioP1)
        tvPromedioP2 = findViewById(R.id.tvPromedioP2)
        tvPromedioP3 = findViewById(R.id.tvPromedioP3)
        tvPromedioFinal = findViewById(R.id.tvPromedioFinal)
    }

    // =====================================
    // ADAPTADORES
    // =====================================

    private fun configurarAdaptadores() {

        val adaptadorPeriodos = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            periodos
        )

        adaptadorPeriodos.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spPeriodo.adapter = adaptadorPeriodos

        adaptadorLista = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            listaTextos
        )

        lvCalificaciones.adapter = adaptadorLista

        spEstudiante.isEnabled = false
        spAsignatura.isEnabled = false
    }

    // =====================================
    // CARGAR GRADOS
    // =====================================

    private fun cargarGrados() {

        grados.clear()
        grados.add("Selecciona un grado")

        val cursor = db.obtenerGrados()

        cursor.use {
            while (it.moveToNext()) {
                grados.add(
                    it.getString(
                        it.getColumnIndexOrThrow("curso")
                    )
                )
            }
        }

        val adaptador = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            grados
        )

        adaptador.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spGrado.adapter = adaptador
    }

    // =====================================
    // CARGAR ESTUDIANTES POR GRADO
    // =====================================

    private fun cargarEstudiantesPorGrado(curso: String) {

        estudiantes.clear()

        val cursor = db.obtenerEstudiantesPorCurso(curso)

        cursor.use {
            while (it.moveToNext()) {

                val id = it.getInt(
                    it.getColumnIndexOrThrow("id")
                )

                val nombres = it.getString(
                    it.getColumnIndexOrThrow("nombres")
                )

                val apellidos = it.getString(
                    it.getColumnIndexOrThrow("apellidos")
                )

                val grado = it.getString(
                    it.getColumnIndexOrThrow("curso")
                )

                estudiantes.add(
                    Opcion(
                        id,
                        "$nombres $apellidos - $grado"
                    )
                )
            }
        }

        val adaptador = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            estudiantes
        )

        adaptador.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spEstudiante.adapter = adaptador
        spEstudiante.isEnabled = estudiantes.isNotEmpty()

        actualizarPromedios()
    }

    // =====================================
    // CARGAR ASIGNATURAS POR GRADO
    // =====================================

    private fun cargarAsignaturasPorGrado(grado: String) {

        asignaturas.clear()

        val cursor = db.obtenerAsignaturasPorGrado(grado)

        cursor.use {
            while (it.moveToNext()) {

                val id = it.getInt(
                    it.getColumnIndexOrThrow("id")
                )

                val nombre = it.getString(
                    it.getColumnIndexOrThrow("nombre")
                )

                val codigo = it.getString(
                    it.getColumnIndexOrThrow("codigo")
                )

                asignaturas.add(
                    Opcion(id, "$nombre - $codigo")
                )
            }
        }

        val adaptador = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            asignaturas
        )

        adaptador.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spAsignatura.adapter = adaptador
        spAsignatura.isEnabled = asignaturas.isNotEmpty()

        actualizarPromedios()

        if (asignaturas.isEmpty()) {
            Toast.makeText(
                this,
                "Este grado no tiene asignaturas registradas",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =====================================
    // LIMPIAR SELECTORES
    // =====================================

    private fun limpiarSelectoresGrado() {

        estudiantes.clear()
        asignaturas.clear()

        spEstudiante.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            emptyList<String>()
        )

        spAsignatura.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            emptyList<String>()
        )

        spEstudiante.isEnabled = false
        spAsignatura.isEnabled = false

        actualizarPromedios()
    }

    // =====================================
    // EVENTOS
    // =====================================

    private fun configurarEventos() {

        btnGuardar.setOnClickListener {
            guardarCalificacion()
        }

        btnLimpiar.setOnClickListener {
            limpiarFormulario()
        }

        btnBuscar.setOnClickListener {
            buscarCalificaciones()
        }

        btnMostrarTodas.setOnClickListener {
            etBuscar.text.clear()
            cargarCalificaciones()
        }

        // CAMBIO DE GRADO

        spGrado.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (position > 0) {

                        val gradoSeleccionado = grados[position]

                        if (gradoSeleccionado != cursoActual) {

                            cursoActual = gradoSeleccionado

                            idCalificacionSeleccionada = -1

                            btnGuardar.text = "GUARDAR CALIFICACIÓN"

                            etActividad.text.clear()
                            etNota.text.clear()

                            cargarEstudiantesPorGrado(
                                gradoSeleccionado
                            )

                            cargarAsignaturasPorGrado(
                                gradoSeleccionado
                            )
                        }

                    } else {

                        cursoActual = ""

                        limpiarSelectoresGrado()

                        idCalificacionSeleccionada = -1

                        btnGuardar.text = "GUARDAR CALIFICACIÓN"

                        etActividad.text.clear()
                        etNota.text.clear()
                    }
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        // CAMBIO DE ESTUDIANTE

        spEstudiante.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    actualizarPromedios()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        // CAMBIO DE ASIGNATURA

        spAsignatura.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    actualizarPromedios()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        // EDITAR AL TOCAR

        lvCalificaciones.setOnItemClickListener {
                _, _, position, _ ->

            cargarCalificacionEnFormulario(position)
        }

        // ELIMINAR AL MANTENER PRESIONADO

        lvCalificaciones.setOnItemLongClickListener {
                _, _, position, _ ->

            confirmarEliminar(position)
            true
        }
    }

    // =====================================
    // GUARDAR CALIFICACIÓN
    // =====================================

    private fun guardarCalificacion() {

        if (spGrado.selectedItemPosition <= 0) {
            Toast.makeText(
                this,
                "Primero selecciona un grado",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (estudiantes.isEmpty()) {
            Toast.makeText(
                this,
                "No hay estudiantes registrados en este grado",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (spEstudiante.selectedItemPosition !in estudiantes.indices) {
            Toast.makeText(
                this,
                "Selecciona un estudiante",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (asignaturas.isEmpty()) {
            Toast.makeText(
                this,
                "Este grado no tiene asignaturas registradas",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (spAsignatura.selectedItemPosition !in asignaturas.indices) {
            Toast.makeText(
                this,
                "Selecciona una asignatura",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val actividad = etActividad.text.toString().trim()

        if (actividad.isEmpty()) {
            etActividad.error = "Escribe el nombre de la actividad"
            return
        }

        val textoNota = etNota.text.toString()
            .trim()
            .replace(",", ".")

        if (textoNota.isEmpty()) {
            etNota.error = "Ingresa una calificación"
            return
        }

        val nota = textoNota.toDoubleOrNull()

        if (nota == null || nota < 0.0 || nota > 5.0) {
            etNota.error = "La nota debe estar entre 0.0 y 5.0"
            return
        }

        val estudiante =
            estudiantes[spEstudiante.selectedItemPosition]

        val asignatura =
            asignaturas[spAsignatura.selectedItemPosition]

        val periodo = spPeriodo.selectedItem.toString()

        try {

            val resultado: Boolean

            if (idCalificacionSeleccionada == -1) {

                resultado = db.insertarCalificacionActividad(
                    estudiante.id,
                    asignatura.id,
                    actividad,
                    nota,
                    periodo
                )

            } else {

                resultado = db.actualizarCalificacionActividad(
                    idCalificacionSeleccionada,
                    estudiante.id,
                    asignatura.id,
                    actividad,
                    nota,
                    periodo
                )
            }

            if (resultado) {

                val mensaje =
                    if (idCalificacionSeleccionada == -1) {
                        "Actividad registrada correctamente"
                    } else {
                        "Actividad actualizada correctamente"
                    }

                Toast.makeText(
                    this,
                    mensaje,
                    Toast.LENGTH_SHORT
                ).show()

                limpiarFormulario()
                cargarCalificaciones()
                actualizarPromedios()

            } else {

                Toast.makeText(
                    this,
                    "No se pudo guardar la actividad",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =====================================
    // CARGAR CALIFICACIONES
    // =====================================

    private fun cargarCalificaciones() {

        val cursor = db.obtenerCalificaciones()
        mostrarResultados(cursor)
    }

    // =====================================
    // BUSCAR CALIFICACIONES
    // =====================================

    private fun buscarCalificaciones() {

        val texto = etBuscar.text.toString().trim()

        if (texto.isEmpty()) {
            Toast.makeText(
                this,
                "Escribe un estudiante, grado, asignatura, periodo o actividad",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val cursor = db.buscarCalificaciones(texto)

        mostrarResultados(cursor)
    }

    // =====================================
    // MOSTRAR RESULTADOS
    // =====================================

    private fun mostrarResultados(cursor: Cursor) {

        listaIds.clear()
        listaTextos.clear()

        cursor.use {

            while (it.moveToNext()) {

                val id = it.getInt(
                    it.getColumnIndexOrThrow("id")
                )

                val estudiante = it.getString(
                    it.getColumnIndexOrThrow("estudiante")
                )

                val grado = it.getString(
                    it.getColumnIndexOrThrow("curso")
                )

                val asignatura = it.getString(
                    it.getColumnIndexOrThrow("asignatura")
                )

                val actividad = it.getString(
                    it.getColumnIndexOrThrow("actividad")
                )

                val nota = it.getDouble(
                    it.getColumnIndexOrThrow("nota")
                )

                val periodo = it.getString(
                    it.getColumnIndexOrThrow("periodo")
                )

                listaIds.add(id)

                listaTextos.add(
                    "ESTUDIANTE: $estudiante\n" +
                            "GRADO: $grado\n" +
                            "ASIGNATURA: $asignatura\n" +
                            "ACTIVIDAD: $actividad\n" +
                            "NOTA: ${String.format(Locale.getDefault(), "%.2f", nota)} / 5.0\n" +
                            "PERIODO: $periodo\n\n" +
                            "Toca para editar | Mantén presionado para eliminar"
                )
            }
        }

        adaptadorLista.notifyDataSetChanged()

        if (listaIds.isEmpty()) {
            Toast.makeText(
                this,
                "No se encontraron calificaciones",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =====================================
    // EDITAR CALIFICACIÓN
    // =====================================

    private fun cargarCalificacionEnFormulario(position: Int) {

        if (position !in listaIds.indices) return

        val id = listaIds[position]

        val cursor = db.obtenerCalificaciones()

        cursor.use {

            while (it.moveToNext()) {

                val idRegistro = it.getInt(
                    it.getColumnIndexOrThrow("id")
                )

                if (idRegistro == id) {

                    val estudianteId = it.getInt(
                        it.getColumnIndexOrThrow("estudiante_id")
                    )

                    val asignaturaId = it.getInt(
                        it.getColumnIndexOrThrow("asignatura_id")
                    )

                    val actividad = it.getString(
                        it.getColumnIndexOrThrow("actividad")
                    )

                    val nota = it.getDouble(
                        it.getColumnIndexOrThrow("nota")
                    )

                    val periodo = it.getString(
                        it.getColumnIndexOrThrow("periodo")
                    )

                    val grado = it.getString(
                        it.getColumnIndexOrThrow("curso")
                    )

                    val posicionGrado = grados.indexOf(grado)

                    if (posicionGrado <= 0) {
                        Toast.makeText(
                            this,
                            "No se encontró el grado de esta calificación",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    cargandoEdicion = true

                    cursoActual = grado

                    spGrado.setSelection(posicionGrado)

                    cargarEstudiantesPorGrado(grado)
                    cargarAsignaturasPorGrado(grado)

                    val posicionEstudiante =
                        estudiantes.indexOfFirst { opcion ->
                            opcion.id == estudianteId
                        }

                    val posicionAsignatura =
                        asignaturas.indexOfFirst { opcion ->
                            opcion.id == asignaturaId
                        }

                    if (posicionEstudiante < 0 || posicionAsignatura < 0) {
                        cargandoEdicion = false

                        Toast.makeText(
                            this,
                            "No se encontraron los datos asociados a esta calificación",
                            Toast.LENGTH_LONG
                        ).show()
                        return
                    }

                    spEstudiante.setSelection(posicionEstudiante)
                    spAsignatura.setSelection(posicionAsignatura)

                    etActividad.setText(actividad)

                    etNota.setText(
                        String.format(Locale.US, "%.2f", nota)
                    )

                    val posicionPeriodo = periodos.indexOf(periodo)

                    if (posicionPeriodo >= 0) {
                        spPeriodo.setSelection(posicionPeriodo)
                    }

                    idCalificacionSeleccionada = id

                    btnGuardar.text = "ACTUALIZAR ACTIVIDAD"

                    cargandoEdicion = false

                    actualizarPromedios()

                    Toast.makeText(
                        this,
                        "Modifica los datos y pulsa actualizar",
                        Toast.LENGTH_SHORT
                    ).show()

                    return
                }
            }
        }
    }

    // =====================================
    // ACTUALIZAR PROMEDIOS
    // =====================================

    private fun actualizarPromedios() {

        if (
            estudiantes.isEmpty() ||
            asignaturas.isEmpty() ||
            spEstudiante.selectedItemPosition !in estudiantes.indices ||
            spAsignatura.selectedItemPosition !in asignaturas.indices
        ) {

            tvPromedioP1.text = "0.00 / 5.0"
            tvPromedioP2.text = "0.00 / 5.0"
            tvPromedioP3.text = "0.00 / 5.0"
            tvPromedioFinal.text = "0.00 / 5.0"

            return
        }

        val estudiante =
            estudiantes[spEstudiante.selectedItemPosition]

        val asignatura =
            asignaturas[spAsignatura.selectedItemPosition]

        val promedio1 = db.obtenerPromedioPeriodo(
            estudiante.id,
            asignatura.id,
            "Periodo 1"
        )

        val promedio2 = db.obtenerPromedioPeriodo(
            estudiante.id,
            asignatura.id,
            "Periodo 2"
        )

        val promedio3 = db.obtenerPromedioPeriodo(
            estudiante.id,
            asignatura.id,
            "Periodo 3"
        )

        val promedioFinal = db.obtenerPromedioFinal(
            estudiante.id,
            asignatura.id
        )

        tvPromedioP1.text = String.format(
            Locale.US, "%.2f / 5.0", promedio1
        )

        tvPromedioP2.text = String.format(
            Locale.US, "%.2f / 5.0", promedio2
        )

        tvPromedioP3.text = String.format(
            Locale.US, "%.2f / 5.0", promedio3
        )

        tvPromedioFinal.text = String.format(
            Locale.US, "%.2f / 5.0", promedioFinal
        )
    }

    // =====================================
    // ELIMINAR CALIFICACIÓN
    // =====================================

    private fun confirmarEliminar(position: Int) {

        if (position !in listaIds.indices) return

        val id = listaIds[position]

        AlertDialog.Builder(this)
            .setTitle("Eliminar actividad")
            .setMessage(
                "¿Deseas eliminar esta calificación?"
            )
            .setPositiveButton("Eliminar") { _, _ ->

                val resultado = db.eliminarCalificacion(id)

                if (resultado) {

                    Toast.makeText(
                        this,
                        "Actividad eliminada",
                        Toast.LENGTH_SHORT
                    ).show()

                    limpiarFormulario()
                    cargarCalificaciones()
                    actualizarPromedios()

                } else {

                    Toast.makeText(
                        this,
                        "No se pudo eliminar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // =====================================
    // LIMPIAR FORMULARIO
    // =====================================

    private fun limpiarFormulario() {

        etActividad.text.clear()
        etNota.text.clear()

        if (estudiantes.isNotEmpty()) {
            spEstudiante.setSelection(0)
        }

        if (asignaturas.isNotEmpty()) {
            spAsignatura.setSelection(0)
        }

        spPeriodo.setSelection(0)

        idCalificacionSeleccionada = -1

        btnGuardar.text = "GUARDAR CALIFICACIÓN"

        actualizarPromedios()
    }
}
