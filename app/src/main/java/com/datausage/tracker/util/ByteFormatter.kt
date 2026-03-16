package com.datausage.tracker.util

/**
 * Convierte bytes a la unidad legible más apropiada.
 * Reglas definidas en METRICS.md § Presentación de valores.
 *
 * < 1.024 B       → "512 B"
 * 1.024–1.048.575 → "768 KB"
 * 1.048.576–1 GB  → "45,3 MB"
 * > 1 GB          → "1,2 GB"
 */
object ByteFormatter {

    private const val KB = 1_024L
    private const val MB = 1_048_576L
    private const val GB = 1_073_741_824L

    fun format(bytes: Long): String {
        return when {
            bytes < KB -> "$bytes B"
            bytes < MB -> "%.1f KB".format(bytes.toFloat() / KB)
            bytes < GB -> "%.1f MB".format(bytes.toFloat() / MB)
            else       -> "%.2f GB".format(bytes.toFloat() / GB)
        }
    }

    /** Devuelve el porcentaje formateado con 1 decimal, ej: "34,5%" */
    fun formatPercent(ratio: Float): String = "%.1f%%".format(ratio * 100f)
}
