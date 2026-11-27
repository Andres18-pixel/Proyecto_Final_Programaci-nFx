package com.tuempresa.ezencuestafx.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Datos básicos de una encuesta guardada, para listarla en "Abrir encuesta". */
public record EncuestaResumen(int id, String titulo, int totalPreguntas, LocalDateTime actualizadaEn) {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public String toString() {
        return titulo + "  ·  " + totalPreguntas + (totalPreguntas == 1 ? " pregunta" : " preguntas")
                + "  ·  " + actualizadaEn.format(FORMATO);
    }
}
