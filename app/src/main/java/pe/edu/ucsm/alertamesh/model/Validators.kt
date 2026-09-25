package pe.edu.ucsm.alertamesh.model

/** Validaciones y normalización de los datos que escribe el usuario. */
object Validators {
    /** Solo letras (con tildes, ñ, etc.) separadas por espacios simples. */
    private val NAME = Regex("^\\p{L}+( \\p{L}+)*$")
    private val SPACES = Regex("\\s+")

    /** Quita espacios de más: "  Ana   Lopez " -> "Ana Lopez". */
    fun normalizeName(raw: String): String = raw.trim().replace(SPACES, " ")

    /** Nombre válido: solo letras y espacios, mínimo 3 caracteres. */
    fun isValidName(name: String): Boolean = name.length >= 3 && NAME.matches(name)

    /** El código de red no distingue mayúsculas ni espacios en los extremos. */
    fun normalizeCode(raw: String): String = raw.trim().lowercase()
}
