package com.motoristapro.capture

import android.util.Log

/**
 * RawOfferData
 * Resultado cru do parsing — ainda não é o RideOffer normalizado do
 * domínio (isso acontece no lado JS/TS, no RideOfferNormalizer).
 * Aqui só extraímos os 3 números essenciais do texto da tela.
 */
data class RawOfferData(
    val valueCents: Long?,      // valor da oferta em centavos (evita float)
    val distanceMeters: Long?,  // distância total estimada em metros
    val durationSeconds: Long?, // duração estimada em segundos
    val rawText: String         // texto original, para debug/auditoria
)

object RideOfferParser {

    private const val TAG = "OfferCapture"

    // R$ 23,50 | R$23.50 | R$ 1.234,56
    private val VALUE_REGEX = Regex("""R\$\s?([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?)""")

    // 12,3 km | 12.3km | 12 km
    private val DISTANCE_REGEX = Regex("""([0-9]+(?:[.,][0-9]+)?)\s?km""", RegexOption.IGNORE_CASE)

    // 15 min | 1h 5min | 15min
    private val DURATION_MIN_REGEX = Regex("""([0-9]+)\s?min""", RegexOption.IGNORE_CASE)
    private val DURATION_HOUR_REGEX = Regex("""([0-9]+)\s?h(?:r)?\b""", RegexOption.IGNORE_CASE)

    fun parse(texts: List<String>): RawOfferData? {
        val joined = texts.joinToString(" ")

        val value = VALUE_REGEX.find(joined)?.groupValues?.get(1)
            ?.replace(".", "")   // remove separador de milhar
            ?.replace(",", ".")  // vírgula decimal -> ponto
            ?.toDoubleOrNull()
            ?.let { (it * 100).toLong() }

        val distance = DISTANCE_REGEX.find(joined)?.groupValues?.get(1)
            ?.replace(",", ".")
            ?.toDoubleOrNull()
            ?.let { (it * 1000).toLong() }

        val hours = DURATION_HOUR_REGEX.find(joined)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val mins = DURATION_MIN_REGEX.find(joined)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val duration = if (hours > 0 || mins > 0) (hours * 3600 + mins * 60) else null

        if (value == null && distance == null && duration == null) {
            Log.d(TAG, "Parser não encontrou nenhum campo reconhecível em: $joined")
            return null
        }

        Log.d(TAG, "Parsed -> valor=${value}c distancia=${distance}m duracao=${duration}s")
        return RawOfferData(value, distance, duration, joined)
    }
}
