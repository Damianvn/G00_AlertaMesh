package pe.edu.ucsm.alertamesh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.ucsm.alertamesh.model.Validators

class ValidatorsTest {
    @Test
    fun acceptsNamesWithLettersAccentsAndSpaces() {
        assertTrue(Validators.isValidName("Alexandra Arce"))
        assertTrue(Validators.isValidName("María Núñez Peña"))
    }

    @Test
    fun rejectsDigitsAndSymbols() {
        assertFalse(Validators.isValidName("Juan123"))
        assertFalse(Validators.isValidName("12345678"))
        assertFalse(Validators.isValidName("Ana_Lopez"))
        assertFalse(Validators.isValidName("Ana-Lopez"))
        assertFalse(Validators.isValidName("Ana!"))
    }

    @Test
    fun rejectsTooShortOrEmpty() {
        assertFalse(Validators.isValidName(""))
        assertFalse(Validators.isValidName("Al"))
        assertFalse(Validators.isValidName("   "))
    }

    @Test
    fun normalizesSpaces() {
        assertEquals("Ana Lopez", Validators.normalizeName("  Ana    Lopez "))
        assertTrue(Validators.isValidName(Validators.normalizeName("  Ana    Lopez ")))
    }

    @Test
    fun networkCodeIgnoresCaseAndOuterSpaces() {
        assertEquals("alertamesh", Validators.normalizeCode("  AlertaMesh "))
    }
}
