package com.example.control_academico

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

/** Panel del ESTUDIANTE: solo lectura de sus propias notas (filtradas en DatabaseHelper). */
class EstudianteActivity : AppCompatActivity() {

    private data class Nota(val asignatura: String, val periodo: String, val actividad: String, val nota: Double)

    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Sesion.esEstudiante) {
            irALogin()
            return
        }
        setContentView(R.layout.activity_estudiante)
        db = DatabaseHelper(this)

        findViewById<TextView>(R.id.tvSaludo).text =
            "Hola, ${Sesion.nombre.substringBefore(' ')}"
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            Sesion.cerrar()
            irALogin()
        }
        cargar()
    }

    private fun cargar() {
        val notas = mutableListOf<Nota>()
        db.obtenerCalificaciones().use { c ->
            val iA = c.getColumnIndexOrThrow("asignatura")
            val iP = c.getColumnIndexOrThrow("periodo")
            val iT = c.getColumnIndexOrThrow("actividad")
            val iN = c.getColumnIndexOrThrow("nota")
            while (c.moveToNext()) {
                notas.add(Nota(c.getString(iA), c.getString(iP), c.getString(iT), c.getDouble(iN)))
            }
        }

        val cont = findViewById<LinearLayout>(R.id.llMaterias)
        cont.removeAllViews()
        findViewById<View>(R.id.tvVacio).visibility = if (notas.isEmpty()) View.VISIBLE else View.GONE
        if (notas.isEmpty()) return

        val promediosMateria = mutableListOf<Double>()
        notas.groupBy { it.asignatura }.forEach { (materia, lista) ->
            val porPeriodo = lista.groupBy { it.periodo }
            val promMateria = porPeriodo.values.map { p -> p.map { it.nota }.average() }.average()
            promediosMateria.add(promMateria)
            cont.addView(tarjetaMateria(materia, promMateria, porPeriodo))
        }

        val general = promediosMateria.average()
        findViewById<TextView>(R.id.tvPromedio).apply {
            text = formato(general)
            setTextColor(colorNota(general))
        }
        findViewById<TextView>(R.id.tvResumen).text =
            "${promediosMateria.size} asignaturas · ${notas.size} notas"
    }

    private fun tarjetaMateria(materia: String, prom: Double, periodos: Map<String, List<Nota>>): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_card)
            setPadding(dp(22), dp(20), dp(22), dp(20))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
        }
        card.addView(fila(materia, formato(prom), 17f, 22f, Color.WHITE, colorNota(prom)))

        periodos.toSortedMap().forEach { (periodo, lista) ->
            card.addView(
                texto("Periodo $periodo · prom. ${formato(lista.map { it.nota }.average())}", 12f,
                    ContextCompat.getColor(this, R.color.accent)).apply {
                    setPadding(0, dp(14), 0, dp(4))
                    letterSpacing = 0.05f
                }
            )
            lista.forEach { n ->
                card.addView(
                    fila(n.actividad, formato(n.nota), 14f, 14f,
                        ContextCompat.getColor(this, R.color.texto_suave), colorNota(n.nota))
                )
            }
        }
        return card
    }

    private fun fila(izq: String, der: String, tamIzq: Float, tamDer: Float, cIzq: Int, cDer: Int) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(3), 0, dp(3))
            addView(texto(izq, tamIzq, cIzq).apply {
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            addView(texto(der, tamDer, cDer))
        }

    private fun texto(t: String, size: Float, color: Int) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(color)
    }

    private fun colorNota(n: Double) =
        if (n >= 3.0) ContextCompat.getColor(this, R.color.accent)
        else ContextCompat.getColor(this, R.color.peligro)

    private fun formato(v: Double) = String.format(Locale.getDefault(), "%.1f", v)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun irALogin() {
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::db.isInitialized) db.close()
    }
}
