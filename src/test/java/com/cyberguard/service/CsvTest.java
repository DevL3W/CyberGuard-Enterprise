package com.cyberguard.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CsvTest {
    @Test
    void nuloSeConvierteEnVacio() { assertEquals("", Csv.cell(null)); }

    @Test
    void protegeContraInyeccionDeFormulas() {
        assertEquals("'=1+1", Csv.cell("=1+1"));
        assertEquals("'@cmd", Csv.cell("@cmd"));
        assertEquals("'+34", Csv.cell("+34"));
    }

    @Test
    void entrecomillaComasYDuplicaComillas() {
        assertEquals("\"a,b\"", Csv.cell("a,b"));
        assertEquals("\"di \"\"hola\"\"\"", Csv.cell("di \"hola\""));
    }

    @Test
    void laFilaTerminaEnSaltoDeLinea() {
        assertEquals("a,,3\r\n", Csv.row("a", null, 3));
    }
}
