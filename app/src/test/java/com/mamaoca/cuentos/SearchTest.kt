package com.mamaoca.cuentos

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTest {
    @Test fun spanishSearchIgnoresAccentsAndCase() {
        assertEquals("los musicos de bremen", "Los Músicos de Bremen".searchKey())
        assertEquals("mama oca", "Mamá Oca".searchKey())
        assertEquals("nino", "NIÑO".searchKey())
    }
}
