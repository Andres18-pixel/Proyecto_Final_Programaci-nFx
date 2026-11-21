package com.tuempresa.ezencuestafx.model;

public enum TipoPregunta {
    TEXTO("Texto (Letras / Abierta)"),
    NUMERICA("Numérica (Solo números)"),
    SELECCION_UNICA("Selección Única (Una opción)"),
    SELECCION_MULTIPLE("Selección Múltiple (Varias opciones)");

    private final String descripcion;

    TipoPregunta(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}