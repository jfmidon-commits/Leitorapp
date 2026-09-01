package com.motoristapro.capture

import android.util.Log

data class RawOfferData(
    val valueCents: Long?,
    val distanceMeters: Long?,
    val durationSeconds: Long?,
    val rawText: String
)

object RideOfferParser {

    private const val TAG = "OfferCapture"

    private val VALUE_REGEX = Regex("""R\$\s?([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?)""")
    private val DISTANCE_REGEX = Regex("""([0-9]+(?:[.,][0-9]+)?)\s?km""", RegexOption.IGNORE_CASE)
    private val DURATION_MIN_REGEX = Regex("""([0-9]+)\s?min""", RegexOption.IGNORE_CASE)
    private val DURATION_HOUR_REGEX = Regex("""([0-9]+)\s?h(?:r)?\b""", RegexOption.IGNORE_CASE)

    fun parse(texts: List<String>): RawOfferData? {
        val joined = texts.joinToString(" ")

        val value = VALUE_REGEX.find(joined)?.groupValues?.get(1)
            ?.replace(".", "")
            ?.replace(",", ".")
            ?.toDoubleOrNull()
            ?.let { (it * 100).toLong() }

        val distance = DISTANCE_REGEX.find(joined)?.groupValues?.get(1)
            ?.replace(",", ".")
            ?.toDoubleOrNull()
            ?.let { (it * 1000).toLong() }

        val hours = DURATION_HOUR_REGEX.find(joined)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val mins = DURATION_MIN_REGEX.find(joined)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val duration = if (hours > 0 || mins > 0) (hours * 3600 + mins * 60) else null

        // Exige "R$" (marcador quase inequívoco) OU km+min juntos, para
        // evitar falso positivo de duração isolada (ex: números de
        // armazenamento/bateria da tela sendo confundidos com minutos).
        val looksLikeRealOffer = value != null || (distance != null && duration != null)
        if (!looksLikeRealOffer) return null

        Log.d(TAG, "Parsed -> valor=${value}c distancia=${distance}m duracao=${duration}s")
        return RawOfferData(value, distance, duration, joined)
    }
}
