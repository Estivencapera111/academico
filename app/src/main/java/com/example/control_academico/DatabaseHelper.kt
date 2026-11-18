package com.example.control_academico

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "Usuarios.db", null, 6) {

    companion object {
        const val TABLA_USUARIOS = "usuarios"
        const val TABLA_ESTUDIANTES = "estudiantes"
        const val TABLA_ASIGNATURAS = "asignaturas"
        const val TABLA_CALIFICACIONES = "calificaciones"

        // Código exigido para registrarse como docente (cámbialo aquí si prefieres otro)
        const val CODIGO_DOCENTE = "DOCENTE-2026"
    }

    enum class ResultadoRegistro {
        OK, CODIGO_INVALIDO, CORREO_EXISTE, ROL_INVALIDO, ERROR
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLA_USUARIOS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                correo TEXT NOT NULL UNIQUE,
                contrasena TEXT NOT NULL,
                sal TEXT NOT NULL DEFAULT '',
                rol TEXT NOT NULL DEFAULT 'ESTUDIANTE'
            )
        """.trimIndent())

        crearTablaEstudiantes(db)
        crearTablaAsignaturas(db)
        crearTablaCalificaciones(db)
    }

    private fun crearTablaEstudiantes(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLA_ESTUDIANTES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombres TEXT NOT NULL,
                apellidos TEXT NOT NULL,
                identificacion TEXT NOT NULL UNIQUE,
                curso TEXT NOT NULL,
                correo TEXT
            )
        """.trimIndent())
    }

    private fun crearTablaAsignaturas(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLA_ASIGNATURAS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                codigo TEXT NOT NULL UNIQUE,
                grado TEXT NOT NULL,
                docente TEXT NOT NULL
            )
        """.trimIndent())
    }

    private fun crearTablaCalificaciones(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLA_CALIFICACIONES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                estudiante_id INTEGER NOT NULL,
                asignatura_id INTEGER NOT NULL,
                actividad TEXT NOT NULL DEFAULT 'Actividad',
                nota REAL NOT NULL,
                periodo TEXT NOT NULL,
                FOREIGN KEY(estudiante_id)
                    REFERENCES $TABLA_ESTUDIANTES(id)
                    ON DELETE CASCADE,
                FOREIGN KEY(asignatura_id)
                    REFERENCES $TABLA_ASIGNATURAS(id)
                    ON DELETE CASCADE
            )
        """.trimIndent())
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) crearTablaEstudiantes(db)
        if (oldVersion < 3) crearTablaAsignaturas(db)
        if (oldVersion < 4) crearTablaCalificaciones(db)

        if (oldVersion < 6) {
            // Migración v6: roles y contraseñas con sal.
            // Las cuentas previas conservan su acceso completo (DOCENTE) y su
            // contraseña se convierte a hash en el siguiente inicio de sesión.
            if (!existeColumna(db, TABLA_USUARIOS, "sal")) {
                db.execSQL("ALTER TABLE $TABLA_USUARIOS ADD COLUMN sal TEXT NOT NULL DEFAULT ''")
            }
            if (!existeColumna(db, TABLA_USUARIOS, "rol")) {
                db.execSQL("ALTER TABLE $TABLA_USUARIOS ADD COLUMN rol TEXT NOT NULL DEFAULT 'ESTUDIANTE'")
                db.execSQL("UPDATE $TABLA_USUARIOS SET rol = 'DOCENTE'")
            }
        }

        if (oldVersion < 5) {
            var existeActividad = false

            val cursor = db.rawQuery(
                "PRAGMA table_info($TABLA_CALIFICACIONES)",
                null
            )

            cursor.use {
                while (it.moveToNext()) {
                    if (
                        it.getString(
                            it.getColumnIndexOrThrow("name")
                        ) == "actividad"
                    ) {
                        existeActividad = true
                        break
                    }
                }
            }

            if (!existeActividad) {
                db.execSQL("""
                    ALTER TABLE $TABLA_CALIFICACIONES
                    ADD COLUMN actividad TEXT NOT NULL
                    DEFAULT 'Actividad'
                """.trimIndent())
            }
        }
    }

    private fun existeColumna(db: SQLiteDatabase, tabla: String, columna: String): Boolean {
        db.rawQuery("PRAGMA table_info($tabla)", null).use {
            while (it.moveToNext()) {
                if (it.getString(it.getColumnIndexOrThrow("name")) == columna) return true
            }
        }
        return false
    }

    // =====================================
    // PERMISOS (capa de datos)
    // =====================================

    private fun vacio(tabla: String): Cursor =
        readableDatabase.rawQuery("SELECT * FROM $tabla WHERE 0", null)

    private fun filtroCalificaciones(): String = when {
        Sesion.esDocente -> "1=1"
        Sesion.esEstudiante -> "LOWER(TRIM(e.correo)) = ?"
        else -> "0=1"
    }

    private fun argsFiltro(): Array<String> =
        if (Sesion.esEstudiante) arrayOf(Sesion.correo.trim().lowercase()) else emptyArray()

    private fun puedeVerEstudiante(id: Int): Boolean {
        if (Sesion.esDocente) return true
        if (!Sesion.esEstudiante) return false
        return readableDatabase.rawQuery(
            "SELECT 1 FROM $TABLA_ESTUDIANTES WHERE id = ? AND LOWER(TRIM(correo)) = ?",
            arrayOf(id.toString(), Sesion.correo.trim().lowercase())
        ).use { it.moveToFirst() }
    }

    // =====================================
    // USUARIOS
    // =====================================

    fun registrarUsuario(
        nombre: String,
        correo: String,
        contrasena: String,
        rol: String,
        codigoDocente: String
    ): ResultadoRegistro {
        if (rol != Sesion.ROL_DOCENTE && rol != Sesion.ROL_ESTUDIANTE) {
            return ResultadoRegistro.ROL_INVALIDO
        }
        if (rol == Sesion.ROL_DOCENTE &&
            !Seguridad.iguales(codigoDocente.trim(), CODIGO_DOCENTE)
        ) return ResultadoRegistro.CODIGO_INVALIDO

        val sal = Seguridad.nuevaSal()
        val valores = ContentValues().apply {
            put("nombre", nombre.trim())
            put("correo", correo.trim().lowercase())
            put("contrasena", Seguridad.hash(contrasena, sal))
            put("sal", sal)
            put("rol", rol)
        }
        return if (writableDatabase.insert(TABLA_USUARIOS, null, valores) != -1L)
            ResultadoRegistro.OK else ResultadoRegistro.CORREO_EXISTE
    }

    /** Verifica credenciales y, si son correctas, abre la sesión con su rol. */
    fun iniciarSesion(correo: String, contrasena: String): Boolean {
        readableDatabase.rawQuery(
            "SELECT id, nombre, correo, contrasena, sal, rol FROM $TABLA_USUARIOS WHERE LOWER(correo) = ?",
            arrayOf(correo.trim().lowercase())
        ).use {
            if (!it.moveToFirst()) return false
            val id = it.getInt(0)
            val guardada = it.getString(3)
            val sal = it.getString(4)

            val ok = if (sal.isEmpty())
                Seguridad.iguales(contrasena, guardada)          // cuenta antigua (texto plano)
            else
                Seguridad.iguales(Seguridad.hash(contrasena, sal), guardada)
            if (!ok) return false

            if (sal.isEmpty()) {                                  // migrar a hash con sal
                val nuevaSal = Seguridad.nuevaSal()
                val v = ContentValues().apply {
                    put("contrasena", Seguridad.hash(contrasena, nuevaSal))
                    put("sal", nuevaSal)
                }
                writableDatabase.update(TABLA_USUARIOS, v, "id = ?", arrayOf(id.toString()))
            }
            Sesion.iniciar(id, it.getString(1), it.getString(2), it.getString(5))
            return true
        }
    }

    // =====================================
    // ESTUDIANTES
    // =====================================

    fun insertarEstudiante(
        nombres: String,
        apellidos: String,
        identificacion: String,
        curso: String,
        correo: String
    ): Boolean {
        if (!Sesion.esDocente) return false
        val valores = ContentValues().apply {
            put("nombres", nombres.trim())
            put("apellidos", apellidos.trim())
            put("identificacion", identificacion.trim())
            put("curso", curso.trim())
            put("correo", correo.trim())
        }

        return writableDatabase.insert(
            TABLA_ESTUDIANTES, null, valores
        ) != -1L
    }

    fun obtenerEstudiantes(): Cursor {
        if (!Sesion.esDocente) return vacio(TABLA_ESTUDIANTES)
        return readableDatabase.rawQuery(
            "SELECT * FROM $TABLA_ESTUDIANTES ORDER BY nombres",
            null
        )
    }

    fun obtenerGrados(): Cursor {
        if (!Sesion.esDocente) return vacio(TABLA_ESTUDIANTES)
        return readableDatabase.rawQuery(
            """
            SELECT DISTINCT TRIM(curso) AS curso
            FROM $TABLA_ESTUDIANTES
            WHERE curso IS NOT NULL
            AND TRIM(curso) != ''
            ORDER BY curso
            """.trimIndent(),
            null
        )
    }

    fun obtenerEstudiantesPorCurso(curso: String): Cursor {
        if (!Sesion.esDocente) return vacio(TABLA_ESTUDIANTES)
        return readableDatabase.rawQuery(
            """
            SELECT *
            FROM $TABLA_ESTUDIANTES
            WHERE TRIM(curso) = TRIM(?)
            ORDER BY apellidos, nombres
            """.trimIndent(),
            arrayOf(curso)
        )
    }

    fun buscarEstudiantes(texto: String): Cursor {
        if (!Sesion.esDocente) return vacio(TABLA_ESTUDIANTES)
        val busqueda = "%${texto.trim()}%"

        return readableDatabase.rawQuery(
            """
            SELECT * FROM $TABLA_ESTUDIANTES
            WHERE nombres LIKE ?
            OR apellidos LIKE ?
            OR identificacion LIKE ?
            OR curso LIKE ?
            OR correo LIKE ?
            ORDER BY nombres
            """.trimIndent(),
            arrayOf(
                busqueda, busqueda, busqueda,
                busqueda, busqueda
            )
        )
    }

    fun actualizarEstudiante(
        id: Int,
        nombres: String,
        apellidos: String,
        identificacion: String,
        curso: String,
        correo: String
    ): Boolean {
        if (!Sesion.esDocente) return false
        val valores = ContentValues().apply {
            put("nombres", nombres.trim())
            put("apellidos", apellidos.trim())
            put("identificacion", identificacion.trim())
            put("curso", curso.trim())
            put("correo", correo.trim())
        }

        return writableDatabase.update(
            TABLA_ESTUDIANTES,
            valores,
            "id = ?",
            arrayOf(id.toString())
        ) > 0
    }

    fun eliminarEstudiante(id: Int): Boolean {
        if (!Sesion.esDocente) return false
        return writableDatabase.delete(
            TABLA_ESTUDIANTES,
            "id = ?",
            arrayOf(id.toString())
        ) > 0
    }

    // =====================================
    // ASIGNATURAS
    // =====================================

    fun insertarAsignatura(
        nombre: String,
        codigo: String,
        grado: String,
        docente: String
    ): Boolean {
        if (!Sesion.esDocente) return false
        val valores = ContentValues().apply {
            put("nombre", nombre.trim())
            put("codigo", codigo.trim().uppercase())
            put("grado", grado.trim())
            put("docente", docente.trim())
        }

        return writableDatabase.insert(
            TABLA_ASIGNATURAS, null, valores
        ) != -1L
    }

    fun obtenerAsignaturas(): Cursor {
        if (!Sesion.esDocente) return vacio(TABLA_ASIGNATURAS)
        return readableDatabase.rawQuery(
            "SELECT * FROM $TABLA_ASIGNATURAS ORDER BY nombre",
            null
        )
    }

    // NUEVO: OBTENER SOLAMENTE LAS ASIGNATURAS DEL GRADO

    fun obtenerAsignaturasPorGrado(grado: String): Cursor {
        if (!Sesion.esDocente) return vacio(TABLA_ASIGNATURAS)
        return readableDatabase.rawQuery(
            """
            SELECT *
            FROM $TABLA_ASIGNATURAS
            WHERE TRIM(grado) = TRIM(?)
            ORDER BY nombre ASC
            """.trimIndent(),
            arrayOf(grado.trim())
        )
    }

    fun buscarAsignaturas(texto: String): Cursor {
        if (!Sesion.esDocente) return vacio(TABLA_ASIGNATURAS)
        val busqueda = "%${texto.trim()}%"

        return readableDatabase.rawQuery(
            """
            SELECT * FROM $TABLA_ASIGNATURAS
            WHERE nombre LIKE ?
            OR codigo LIKE ?
            OR grado LIKE ?
            OR docente LIKE ?
            ORDER BY nombre
            """.trimIndent(),
            arrayOf(busqueda, busqueda, busqueda, busqueda)
        )
    }

    fun actualizarAsignatura(
        id: Int,
        nombre: String,
        codigo: String,
        grado: String,
        docente: String
    ): Boolean {
        if (!Sesion.esDocente) return false
        val valores = ContentValues().apply {
            put("nombre", nombre.trim())
            put("codigo", codigo.trim().uppercase())
            put("grado", grado.trim())
            put("docente", docente.trim())
        }

        return writableDatabase.update(
            TABLA_ASIGNATURAS,
            valores,
            "id = ?",
            arrayOf(id.toString())
        ) > 0
    }

    fun eliminarAsignatura(id: Int): Boolean {
        if (!Sesion.esDocente) return false
        return writableDatabase.delete(
            TABLA_ASIGNATURAS,
            "id = ?",
            arrayOf(id.toString())
        ) > 0
    }

    // =====================================
    // CALIFICACIONES
    // =====================================

    fun insertarCalificacion(
        estudianteId: Int,
        asignaturaId: Int,
        nota: Double,
        periodo: String
    ): Boolean {
        if (!Sesion.esDocente) return false
        if (nota !in 0.0..5.0) return false

        val valores = ContentValues().apply {
            put("estudiante_id", estudianteId)
            put("asignatura_id", asignaturaId)
            put("actividad", "Actividad")
            put("nota", nota)
            put("periodo", periodo.trim())
        }

        return writableDatabase.insert(
            TABLA_CALIFICACIONES, null, valores
        ) != -1L
    }

    fun insertarCalificacionActividad(
        estudianteId: Int,
        asignaturaId: Int,
        actividad: String,
        nota: Double,
        periodo: String
    ): Boolean {
        if (!Sesion.esDocente) return false
        if (
            actividad.isBlank() ||
            nota !in 0.0..5.0 ||
            periodo.isBlank()
        ) return false

        val valores = ContentValues().apply {
            put("estudiante_id", estudianteId)
            put("asignatura_id", asignaturaId)
            put("actividad", actividad.trim())
            put("nota", nota)
            put("periodo", periodo.trim())
        }

        return writableDatabase.insert(
            TABLA_CALIFICACIONES, null, valores
        ) != -1L
    }

    fun obtenerCalificaciones(): Cursor {
        return readableDatabase.rawQuery(
            """
            SELECT
                c.id,
                c.estudiante_id,
                c.asignatura_id,
                c.actividad,
                c.nota,
                c.periodo,
                e.nombres || ' ' || e.apellidos AS estudiante,
                e.curso AS curso,
                a.nombre AS asignatura
            FROM $TABLA_CALIFICACIONES c
            INNER JOIN $TABLA_ESTUDIANTES e
                ON c.estudiante_id = e.id
            INNER JOIN $TABLA_ASIGNATURAS a
                ON c.asignatura_id = a.id
            WHERE ${filtroCalificaciones()}
            ORDER BY e.apellidos, e.nombres, a.nombre, c.periodo
            """.trimIndent(),
            argsFiltro()
        )
    }

    fun buscarCalificaciones(texto: String): Cursor {
        val busqueda = "%${texto.trim()}%"

        return readableDatabase.rawQuery(
            """
            SELECT
                c.id,
                c.estudiante_id,
                c.asignatura_id,
                c.actividad,
                c.nota,
                c.periodo,
                e.nombres || ' ' || e.apellidos AS estudiante,
                e.curso AS curso,
                a.nombre AS asignatura
            FROM $TABLA_CALIFICACIONES c
            INNER JOIN $TABLA_ESTUDIANTES e
                ON c.estudiante_id = e.id
            INNER JOIN $TABLA_ASIGNATURAS a
                ON c.asignatura_id = a.id
            WHERE ${filtroCalificaciones()}
                AND (e.nombres LIKE ?
                OR e.apellidos LIKE ?
                OR e.curso LIKE ?
                OR a.nombre LIKE ?
                OR c.periodo LIKE ?
                OR c.actividad LIKE ?)
            ORDER BY e.apellidos, e.nombres, a.nombre
            """.trimIndent(),
            argsFiltro() + arrayOf(
                busqueda, busqueda, busqueda,
                busqueda, busqueda, busqueda
            )
        )
    }

    fun actualizarCalificacion(
        id: Int,
        estudianteId: Int,
        asignaturaId: Int,
        nota: Double,
        periodo: String
    ): Boolean {
        if (!Sesion.esDocente) return false
        if (nota !in 0.0..5.0) return false

        val valores = ContentValues().apply {
            put("estudiante_id", estudianteId)
            put("asignatura_id", asignaturaId)
            put("nota", nota)
            put("periodo", periodo.trim())
        }

        return writableDatabase.update(
            TABLA_CALIFICACIONES,
            valores,
            "id = ?",
            arrayOf(id.toString())
        ) > 0
    }

    fun actualizarCalificacionActividad(
        id: Int,
        estudianteId: Int,
        asignaturaId: Int,
        actividad: String,
        nota: Double,
        periodo: String
    ): Boolean {
        if (!Sesion.esDocente) return false
        if (
            actividad.isBlank() ||
            nota !in 0.0..5.0 ||
            periodo.isBlank()
        ) return false

        val valores = ContentValues().apply {
            put("estudiante_id", estudianteId)
            put("asignatura_id", asignaturaId)
            put("actividad", actividad.trim())
            put("nota", nota)
            put("periodo", periodo.trim())
        }

        return writableDatabase.update(
            TABLA_CALIFICACIONES,
            valores,
            "id = ?",
            arrayOf(id.toString())
        ) > 0
    }

    fun eliminarCalificacion(id: Int): Boolean {
        if (!Sesion.esDocente) return false
        return writableDatabase.delete(
            TABLA_CALIFICACIONES,
            "id = ?",
            arrayOf(id.toString())
        ) > 0
    }

    // =====================================
    // PROMEDIOS
    // =====================================

    fun obtenerPromedio(
        estudianteId: Int,
        asignaturaId: Int
    ): Double {
        if (!puedeVerEstudiante(estudianteId)) return 0.0
        val cursor = readableDatabase.rawQuery(
            """
            SELECT AVG(nota)
            FROM $TABLA_CALIFICACIONES
            WHERE estudiante_id = ?
            AND asignatura_id = ?
            """.trimIndent(),
            arrayOf(
                estudianteId.toString(),
                asignaturaId.toString()
            )
        )

        return cursor.use {
            if (it.moveToFirst() && !it.isNull(0))
                it.getDouble(0)
            else 0.0
        }
    }

    fun obtenerPromedioPeriodo(
        estudianteId: Int,
        asignaturaId: Int,
        periodo: String
    ): Double {
        if (!puedeVerEstudiante(estudianteId)) return 0.0
        val cursor = readableDatabase.rawQuery(
            """
            SELECT AVG(nota)
            FROM $TABLA_CALIFICACIONES
            WHERE estudiante_id = ?
            AND asignatura_id = ?
            AND periodo = ?
            """.trimIndent(),
            arrayOf(
                estudianteId.toString(),
                asignaturaId.toString(),
                periodo
            )
        )

        return cursor.use {
            if (it.moveToFirst() && !it.isNull(0))
                it.getDouble(0)
            else 0.0
        }
    }

    fun obtenerPromedioFinal(
        estudianteId: Int,
        asignaturaId: Int
    ): Double {
        if (!puedeVerEstudiante(estudianteId)) return 0.0
        val cursor = readableDatabase.rawQuery(
            """
            SELECT AVG(promedio_periodo)
            FROM (
                SELECT AVG(nota) AS promedio_periodo
                FROM $TABLA_CALIFICACIONES
                WHERE estudiante_id = ?
                AND asignatura_id = ?
                GROUP BY periodo
            )
            """.trimIndent(),
            arrayOf(
                estudianteId.toString(),
                asignaturaId.toString()
            )
        )

        return cursor.use {
            if (it.moveToFirst() && !it.isNull(0))
                it.getDouble(0)
            else 0.0
        }
    }
}
