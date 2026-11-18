package com.example.control_academico

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class InformesActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var spinnerGrado: Spinner
    private lateinit var spinnerEstudiante: Spinner
    private lateinit var contenedorInforme: LinearLayout
    private lateinit var txtTitulo: TextView
    private lateinit var btnGenerarPdf: Button

    data class Opcion(
        val id: Int,
        val texto: String
    )

    data class DatoAsignatura(
        val nombre: String,
        val p1: Double,
        val p2: Double,
        val p3: Double,
        val promedioFinal: Double,
        val tieneP1: Boolean,
        val tieneP2: Boolean,
        val tieneP3: Boolean
    ) {
        val tieneNotas: Boolean
            get() = tieneP1 || tieneP2 || tieneP3
    }

    private val grados = mutableListOf<String>()
    private val estudiantes = mutableListOf<Opcion>()

    private val periodos = arrayOf(
        "Periodo 1",
        "Periodo 2",
        "Periodo 3"
    )

    private var ultimoEstudianteId = -1
    private var ultimoNombreEstudiante = ""
    private var ultimoGrado = ""

    private val datosInforme = mutableListOf<DatoAsignatura>()

    companion object {
        private const val SOLICITUD_PDF = 1001
        private const val NOMBRE_INSTITUCION =
            "LOS HIPOPOTAMOS DE PABLITO"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Sesion.esDocente) { finish(); return }
        setContentView(R.layout.activity_informes)

        db = DatabaseHelper(this)

        spinnerGrado = findViewById(R.id.spinnerGradoInforme)
        spinnerEstudiante = findViewById(R.id.spinnerEstudianteInforme)
        contenedorInforme = findViewById(R.id.contenedorInforme)
        txtTitulo = findViewById(R.id.txtTituloInforme)
        btnGenerarPdf = findViewById(R.id.btnGenerarPdf)

        btnGenerarPdf.isEnabled = false
        btnGenerarPdf.alpha = 0.5f

        findViewById<Button>(R.id.btnConsultarInforme)
            .setOnClickListener {
                consultarInforme()
            }

        btnGenerarPdf.setOnClickListener {
            solicitarGuardarPdf()
        }

        cargarGrados()
    }

    private fun limpiarInforme() {
        datosInforme.clear()

        ultimoEstudianteId = -1
        ultimoNombreEstudiante = ""
        ultimoGrado = ""

        btnGenerarPdf.isEnabled = false
        btnGenerarPdf.alpha = 0.5f

        contenedorInforme.removeAllViews()
    }

    private fun cargarGrados() {

        grados.clear()

        val cursor = db.obtenerGrados()

        cursor.use {
            while (it.moveToNext()) {
                grados.add(it.getString(0))
            }
        }

        val opciones = mutableListOf("Seleccione un grado")
        opciones.addAll(grados)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opciones
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerGrado.adapter = adapter

        spinnerGrado.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    limpiarInforme()

                    if (position == 0) {
                        estudiantes.clear()
                        spinnerEstudiante.adapter = null
                    } else {
                        cargarEstudiantes(grados[position - 1])
                    }
                }
            }
    }

    private fun cargarEstudiantes(grado: String) {

        estudiantes.clear()

        val cursor = db.obtenerEstudiantesPorCurso(grado)

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

                estudiantes.add(
                    Opcion(id, "$apellidos $nombres")
                )
            }
        }

        val opciones = mutableListOf("Seleccione un estudiante")
        opciones.addAll(estudiantes.map { it.texto })

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opciones
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerEstudiante.adapter = adapter

        limpiarInforme()
    }

    private fun consultarInforme() {

        val gradoPosicion =
            spinnerGrado.selectedItemPosition

        val estudiantePosicion =
            spinnerEstudiante.selectedItemPosition

        if (gradoPosicion <= 0 || grados.isEmpty()) {
            Toast.makeText(
                this,
                "Seleccione un grado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (
            estudiantes.isEmpty() ||
            estudiantePosicion <= 0 ||
            estudiantePosicion > estudiantes.size
        ) {
            Toast.makeText(
                this,
                "Seleccione un estudiante",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val estudiante = estudiantes[estudiantePosicion - 1]

        mostrarInforme(
            estudiante.id,
            estudiante.texto,
            grados[gradoPosicion - 1]
        )
    }

    private fun mostrarInforme(
        estudianteId: Int,
        nombreEstudiante: String,
        grado: String
    ) {

        limpiarInforme()

        ultimoEstudianteId = estudianteId
        ultimoNombreEstudiante = nombreEstudiante
        ultimoGrado = grado

        txtTitulo.text = "BOLETÍN ACADÉMICO"

        agregarTexto(
            NOMBRE_INSTITUCION,
            20f,
            true
        )

        agregarTexto(
            "ESTUDIANTE: $nombreEstudiante",
            18f,
            true
        )

        agregarTexto(
            "GRADO: $grado",
            16f,
            true
        )

        agregarSeparador()

        val cursor = db.obtenerAsignaturasPorGrado(grado)

        cursor.use {

            while (it.moveToNext()) {

                val asignaturaId = it.getInt(
                    it.getColumnIndexOrThrow("id")
                )

                val nombreAsignatura = it.getString(
                    it.getColumnIndexOrThrow("nombre")
                )

                val tieneP1 = tieneNotasPeriodo(
                    estudianteId,
                    asignaturaId,
                    periodos[0]
                )

                val tieneP2 = tieneNotasPeriodo(
                    estudianteId,
                    asignaturaId,
                    periodos[1]
                )

                val tieneP3 = tieneNotasPeriodo(
                    estudianteId,
                    asignaturaId,
                    periodos[2]
                )

                val promedio1 = if (tieneP1) {
                    db.obtenerPromedioPeriodo(
                        estudianteId,
                        asignaturaId,
                        periodos[0]
                    )
                } else {
                    0.0
                }

                val promedio2 = if (tieneP2) {
                    db.obtenerPromedioPeriodo(
                        estudianteId,
                        asignaturaId,
                        periodos[1]
                    )
                } else {
                    0.0
                }

                val promedio3 = if (tieneP3) {
                    db.obtenerPromedioPeriodo(
                        estudianteId,
                        asignaturaId,
                        periodos[2]
                    )
                } else {
                    0.0
                }

                val tieneNotas = tieneP1 || tieneP2 || tieneP3

                val promedioFinal = if (tieneNotas) {
                    db.obtenerPromedioFinal(
                        estudianteId,
                        asignaturaId
                    )
                } else {
                    0.0
                }

                val dato = DatoAsignatura(
                    nombreAsignatura,
                    promedio1,
                    promedio2,
                    promedio3,
                    promedioFinal,
                    tieneP1,
                    tieneP2,
                    tieneP3
                )

                datosInforme.add(dato)
                agregarTarjetaAsignatura(dato)
            }
        }

        if (datosInforme.isEmpty()) {

            agregarTexto(
                "No hay asignaturas registradas para este grado.",
                15f,
                false
            )

            return
        }

        agregarSeparador()

        val asignaturasConNotas =
            datosInforme.filter { it.tieneNotas }

        if (asignaturasConNotas.isNotEmpty()) {

            val promedioGeneral =
                asignaturasConNotas.map {
                    it.promedioFinal
                }.average()

            agregarTexto(
                "PROMEDIO GENERAL: ${formato(promedioGeneral)}",
                20f,
                true
            )

            agregarTexto(
                if (promedioGeneral >= 3.0) {
                    "ESTADO GENERAL: APROBADO"
                } else {
                    "ESTADO GENERAL: EN RIESGO ACADÉMICO"
                },
                16f,
                true
            )

        } else {

            agregarTexto(
                "Aún no hay calificaciones registradas.",
                16f,
                true
            )
        }

        agregarTexto(
            "Escala de calificación: 0.0 a 5.0",
            13f,
            false
        )

        agregarTexto(
            "Nota mínima aprobatoria: 3.0",
            13f,
            false
        )

        btnGenerarPdf.isEnabled = true
        btnGenerarPdf.alpha = 1f
    }

    private fun tieneNotasPeriodo(
        estudianteId: Int,
        asignaturaId: Int,
        periodo: String
    ): Boolean {

        val cursor = db.obtenerCalificaciones()
        var encontrado = false

        cursor.use {

            while (it.moveToNext()) {

                val idEstudiante = it.getInt(
                    it.getColumnIndexOrThrow("estudiante_id")
                )

                val idAsignatura = it.getInt(
                    it.getColumnIndexOrThrow("asignatura_id")
                )

                val periodoNota = it.getString(
                    it.getColumnIndexOrThrow("periodo")
                )

                if (
                    idEstudiante == estudianteId &&
                    idAsignatura == asignaturaId &&
                    periodoNota.equals(periodo, ignoreCase = true)
                ) {
                    encontrado = true
                    break
                }
            }
        }

        return encontrado
    }

    private fun agregarTarjetaAsignatura(
        dato: DatoAsignatura
    ) {

        val tarjeta = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundColor(
                Color.rgb(239, 247, 253)
            )
        }

        val titulo = TextView(this).apply {
            text = dato.nombre
            textSize = 18f
            setTextColor(Color.rgb(20, 70, 120))
            setTypeface(null, Typeface.BOLD)
        }

        tarjeta.addView(titulo)

        val datos = if (dato.tieneNotas) {

            """
            Periodo 1: ${if (dato.tieneP1) formato(dato.p1) else "Sin notas"}
            Periodo 2: ${if (dato.tieneP2) formato(dato.p2) else "Sin notas"}
            Periodo 3: ${if (dato.tieneP3) formato(dato.p3) else "Sin notas"}
            Promedio final: ${formato(dato.promedioFinal)}
            """.trimIndent()

        } else {
            "Sin calificaciones registradas"
        }

        val detalle = TextView(this).apply {
            text = datos
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, 12, 0, 0)
        }

        tarjeta.addView(detalle)

        if (dato.tieneNotas) {

            val estado = TextView(this).apply {

                text = if (dato.promedioFinal >= 3.0) {
                    "APROBADA"
                } else {
                    "REPROBADA"
                }

                textSize = 14f
                setTypeface(null, Typeface.BOLD)

                setTextColor(
                    if (dato.promedioFinal >= 3.0) {
                        Color.rgb(30, 130, 70)
                    } else {
                        Color.rgb(190, 50, 50)
                    }
                )

                setPadding(0, 10, 0, 0)
            }

            tarjeta.addView(estado)
        }

        val parametros = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        parametros.setMargins(0, 10, 0, 10)

        contenedorInforme.addView(tarjeta, parametros)
    }

    private fun agregarTexto(
        texto: String,
        tamano: Float,
        negrita: Boolean
    ) {

        val vista = TextView(this).apply {

            text = texto
            textSize = tamano

            setTextColor(
                Color.rgb(30, 60, 90)
            )

            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 10, 8, 10)

            if (negrita) {
                setTypeface(null, Typeface.BOLD)
            }
        }

        contenedorInforme.addView(vista)
    }

    private fun agregarSeparador() {

        val linea = View(this).apply {
            setBackgroundColor(
                Color.rgb(160, 200, 225)
            )
        }

        val parametros = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            2
        )

        parametros.setMargins(0, 12, 0, 12)

        contenedorInforme.addView(linea, parametros)
    }

    private fun formato(valor: Double): String {
        return String.format(
            Locale.getDefault(),
            "%.2f",
            valor
        )
    }

    // SOLICITAR UBICACIÓN PARA GUARDAR EL PDF

    private fun solicitarGuardarPdf() {

        if (
            ultimoEstudianteId == -1 ||
            datosInforme.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Primero consulte un boletín académico.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val estudianteSeleccionado =
            estudiantes.getOrNull(
                spinnerEstudiante.selectedItemPosition - 1
            )

        if (
            spinnerGrado.selectedItem?.toString() != ultimoGrado ||
            estudianteSeleccionado?.id != ultimoEstudianteId
        ) {

            limpiarInforme()

            Toast.makeText(
                this,
                "Consulte nuevamente el boletín del estudiante seleccionado.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val nombreArchivo = ultimoNombreEstudiante
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {

            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"

            putExtra(
                Intent.EXTRA_TITLE,
                "Boletin_$nombreArchivo.pdf"
            )
        }

        startActivityForResult(intent, SOLICITUD_PDF)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode == SOLICITUD_PDF &&
            resultCode == Activity.RESULT_OK
        ) {

            val uri = data?.data

            if (uri != null) {
                guardarPdf(uri)
            }
        }
    }

    // GENERAR PDF INSTITUCIONAL

    private fun guardarPdf(uri: Uri) {

        val documento = PdfDocument()

        try {

            val ancho = 595
            val alto = 842

            val pintura = Paint(Paint.ANTI_ALIAS_FLAG)

            var pagina: PdfDocument.Page? = null
            var lienzo: Canvas? = null

            var posicionY = 0f
            var numeroPagina = 0

            fun dibujarTexto(
                texto: String,
                x: Float,
                y: Float,
                tamano: Float,
                negrita: Boolean = false,
                color: Int = Color.rgb(30, 60, 90)
            ) {

                pintura.textSize = tamano
                pintura.color = color

                pintura.typeface = if (negrita) {
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
                } else {
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.NORMAL
                    )
                }

                lienzo?.drawText(
                    texto,
                    x,
                    y,
                    pintura
                )
            }

            fun nuevaPagina() {

                if (pagina != null) {
                    documento.finishPage(pagina!!)
                }

                numeroPagina++

                val informacion =
                    PdfDocument.PageInfo.Builder(
                        ancho,
                        alto,
                        numeroPagina
                    ).create()

                pagina = documento.startPage(informacion)
                lienzo = pagina!!.canvas

                lienzo?.drawColor(Color.WHITE)

                // ENCABEZADO INSTITUCIONAL

                pintura.color = Color.rgb(23, 74, 117)
                pintura.style = Paint.Style.FILL

                lienzo?.drawRoundRect(
                    RectF(25f, 15f, 570f, 115f),
                    10f,
                    10f,
                    pintura
                )

                // ESCUDO DEL COLEGIO

                val escudo = BitmapFactory.decodeResource(
                    resources,
                    R.drawable.escudo_colegio
                ) ?: throw Exception(
                    "No se encontró el escudo institucional."
                )

                try {

                    val destino = RectF(
                        40f,
                        22f,
                        125f,
                        107f
                    )

                    lienzo?.drawBitmap(
                        escudo,
                        null,
                        destino,
                        pintura
                    )

                } finally {
                    escudo.recycle()
                }

                // NOMBRE DE LA INSTITUCIÓN

                dibujarTexto(
                    NOMBRE_INSTITUCION,
                    140f,
                    48f,
                    16f,
                    true,
                    Color.WHITE
                )

                dibujarTexto(
                    "BOLETÍN ACADÉMICO",
                    140f,
                    75f,
                    15f,
                    true,
                    Color.WHITE
                )

                dibujarTexto(
                    "INFORME DE DESEMPEÑO ESCOLAR",
                    140f,
                    96f,
                    10f,
                    false,
                    Color.WHITE
                )

                // DATOS DEL ESTUDIANTE

                dibujarTexto(
                    "ESTUDIANTE: $ultimoNombreEstudiante",
                    40f,
                    145f,
                    11f,
                    true
                )

                dibujarTexto(
                    "GRADO: $ultimoGrado",
                    40f,
                    165f,
                    11f,
                    true
                )

                // SEPARADOR

                pintura.color = Color.rgb(160, 200, 225)
                pintura.strokeWidth = 1.5f

                lienzo?.drawLine(
                    40f,
                    180f,
                    555f,
                    180f,
                    pintura
                )

                // ENCABEZADOS DE CALIFICACIONES

                dibujarTexto(
                    "ASIGNATURA",
                    40f,
                    205f,
                    9f,
                    true
                )

                dibujarTexto("P1", 285f, 205f, 9f, true)
                dibujarTexto("P2", 335f, 205f, 9f, true)
                dibujarTexto("P3", 385f, 205f, 9f, true)
                dibujarTexto("FINAL", 435f, 205f, 9f, true)
                dibujarTexto("ESTADO", 495f, 205f, 9f, true)

                posicionY = 230f
            }

            // PRIMERA PÁGINA

            nuevaPagina()

            // RECORRER ASIGNATURAS

            for (dato in datosInforme) {

                if (posicionY > 750f) {
                    nuevaPagina()
                }

                val nombre = if (dato.nombre.length > 35) {
                    dato.nombre.take(32) + "..."
                } else {
                    dato.nombre
                }

                dibujarTexto(
                    nombre,
                    40f,
                    posicionY,
                    9f
                )

                dibujarTexto(
                    if (dato.tieneP1) formato(dato.p1) else "—",
                    285f,
                    posicionY,
                    9f
                )

                dibujarTexto(
                    if (dato.tieneP2) formato(dato.p2) else "—",
                    335f,
                    posicionY,
                    9f
                )

                dibujarTexto(
                    if (dato.tieneP3) formato(dato.p3) else "—",
                    385f,
                    posicionY,
                    9f
                )

                dibujarTexto(
                    if (dato.tieneNotas) {
                        formato(dato.promedioFinal)
                    } else {
                        "—"
                    },
                    435f,
                    posicionY,
                    9f
                )

                val estado = when {
                    !dato.tieneNotas -> "SIN NOTAS"
                    dato.promedioFinal >= 3.0 -> "APROB."
                    else -> "REPROB."
                }

                val colorEstado = when {
                    !dato.tieneNotas -> Color.DKGRAY
                    dato.promedioFinal >= 3.0 ->
                        Color.rgb(30, 130, 70)
                    else ->
                        Color.rgb(190, 50, 50)
                }

                dibujarTexto(
                    estado,
                    495f,
                    posicionY,
                    8f,
                    true,
                    colorEstado
                )

                // LÍNEA DE CADA ASIGNATURA

                pintura.color = Color.rgb(225, 232, 238)
                pintura.strokeWidth = 0.7f

                lienzo?.drawLine(
                    40f,
                    posicionY + 8f,
                    555f,
                    posicionY + 8f,
                    pintura
                )

                posicionY += 28f
            }

            // ESPACIO PARA RESUMEN

            if (posicionY > 650f) {
                nuevaPagina()
            }

            posicionY += 15f

            pintura.color = Color.rgb(160, 200, 225)
            pintura.strokeWidth = 1.5f

            lienzo?.drawLine(
                40f,
                posicionY,
                555f,
                posicionY,
                pintura
            )

            posicionY += 35f

            // PROMEDIO GENERAL

            val asignaturasConNotas =
                datosInforme.filter { it.tieneNotas }

            val promedioGeneral =
                if (asignaturasConNotas.isNotEmpty()) {
                    asignaturasConNotas
                        .map { it.promedioFinal }
                        .average()
                } else {
                    null
                }

            dibujarTexto(
                if (promedioGeneral != null) {
                    "PROMEDIO GENERAL: ${formato(promedioGeneral)}"
                } else {
                    "SIN CALIFICACIONES REGISTRADAS"
                },
                40f,
                posicionY,
                14f,
                true
            )

            posicionY += 30f

            // ESTADO GENERAL

            if (promedioGeneral != null) {

                val aprobado = promedioGeneral >= 3.0

                dibujarTexto(
                    if (aprobado) {
                        "ESTADO GENERAL: APROBADO"
                    } else {
                        "ESTADO GENERAL: EN RIESGO ACADEMICO"
                    },
                    40f,
                    posicionY,
                    12f,
                    true,
                    if (aprobado) {
                        Color.rgb(30, 130, 70)
                    } else {
                        Color.rgb(190, 50, 50)
                    }
                )
            }

            posicionY += 30f

            dibujarTexto(
                "Escala de calificación: 0.0 a 5.0",
                40f,
                posicionY,
                10f
            )

            posicionY += 20f

            dibujarTexto(
                "Nota mínima aprobatoria: 3.0",
                40f,
                posicionY,
                10f
            )

            posicionY += 75f

            // ESPACIOS PARA FIRMAS

            if (posicionY > 790f) {
                nuevaPagina()
                posicionY = 650f
            }

            pintura.color = Color.DKGRAY
            pintura.strokeWidth = 1f

            lienzo?.drawLine(
                55f,
                posicionY,
                245f,
                posicionY,
                pintura
            )

            lienzo?.drawLine(
                350f,
                posicionY,
                540f,
                posicionY,
                pintura
            )

            dibujarTexto(
                "Firma del docente",
                90f,
                posicionY + 18f,
                9f
            )

            dibujarTexto(
                "Firma del acudiente",
                385f,
                posicionY + 18f,
                9f
            )

            // PIE DE PÁGINA

            dibujarTexto(
                NOMBRE_INSTITUCION,
                40f,
                820f,
                8f,
                false,
                Color.GRAY
            )

            dibujarTexto(
                "Página $numeroPagina",
                500f,
                820f,
                8f,
                false,
                Color.GRAY
            )

            // FINALIZAR ÚLTIMA PÁGINA

            if (pagina != null) {
                documento.finishPage(pagina!!)
                pagina = null
            }

            // GUARDAR PDF

            val salida = contentResolver.openOutputStream(uri)
                ?: throw Exception(
                    "No se pudo abrir el archivo."
                )

            salida.use {
                documento.writeTo(it)
            }

            Toast.makeText(
                this,
                "Boletín PDF institucional guardado correctamente.",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Error al generar PDF: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

        } finally {

            documento.close()
        }
    }
}
