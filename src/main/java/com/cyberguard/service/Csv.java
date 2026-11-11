package com.cyberguard.service;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Utilidad para generar CSV seguro (protege contra inyección de fórmulas al abrirlo en Excel). */
public final class Csv {
    private Csv() {}

    public static String cell(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public static String row(Object... cells) {
        return Stream.of(cells).map(Csv::cell).collect(Collectors.joining(",")) + "\r\n";
    }
}
