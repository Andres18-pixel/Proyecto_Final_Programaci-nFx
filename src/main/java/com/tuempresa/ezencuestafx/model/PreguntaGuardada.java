package com.tuempresa.ezencuestafx.model;

import java.time.LocalDateTime;

/**
 * Una pregunta del banco del usuario junto con datos de su uso anterior.
 *
 * @param pregunta   la pregunta (con id, tipo y opciones)
 * @param vecesUsada en cuántas encuestas guardadas se ha usado
 * @param usadaEn    títulos de las encuestas donde se usó (separados por coma)
 * @param creadaEn   cuándo se guardó por primera vez
 */
public record PreguntaGuardada(Pregunta pregunta, int vecesUsada, String usadaEn, LocalDateTime creadaEn) {
}
