package com.example.control_academico

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Hash de contraseñas con sal (PBKDF2). Nunca se guarda texto plano. */
object Seguridad {
    private const val ITERACIONES = 20000
    private const val BITS = 256

    fun nuevaSal(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(clave: String, sal: String): String {
        val spec = PBEKeySpec(
            clave.toCharArray(),
            Base64.decode(sal, Base64.NO_WRAP),
            ITERACIONES,
            BITS
        )
        val bytes = SecretKeyFactory
            .getInstance("PBKDF2WithHmacSHA1")
            .generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun iguales(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
}
