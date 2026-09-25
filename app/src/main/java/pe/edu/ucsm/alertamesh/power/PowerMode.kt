package pe.edu.ucsm.alertamesh.power

/**
 * Modo de energía. En LOW_POWER se escucha y transmite con ciclos más espaciados
 * y se limita cuántos mensajes se envían por ciclo (los SOS siempre salen primero).
 */
enum class PowerMode(
    val scanIntervalMs: Long,
    val flushIntervalMs: Long,
    val maxPerFlush: Int
) {
    NORMAL(scanIntervalMs = 1_000, flushIntervalMs = 500, maxPerFlush = 50),
    LOW_POWER(scanIntervalMs = 10_000, flushIntervalMs = 5_000, maxPerFlush = 10)
}
