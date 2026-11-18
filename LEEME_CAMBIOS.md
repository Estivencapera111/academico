# Cambios del rediseño
Código docente: DOCENTE-2026 (constante CODIGO_DOCENTE en DatabaseHelper.kt). BD migra a v6 sin perder datos.
Cuentas antiguas pasan a DOCENTE y su clave se convierte a hash al iniciar sesión.

## Cómo probar
- Docente: Crear cuenta > Docente > código DOCENTE-2026 > panel con 4 módulos (CRUD completo).
  Registra un estudiante con correo X en "Estudiantes" y notas en "Calificaciones".
- Estudiante: Crear cuenta > Estudiante con el MISMO correo X > solo ve sus notas (lectura).
