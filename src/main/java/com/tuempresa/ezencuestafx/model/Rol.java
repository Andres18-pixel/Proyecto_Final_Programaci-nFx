package com.tuempresa.ezencuestafx.model;

public enum Rol {
    DOCENTE("Docente"),
    ESTUDIANTE_INVESTIGACION("Estudiante de investigación");

    private final String descripcion;

    Rol(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() { return descripcion; }

    @Override
    public String toString() {
        return descripcion;
    }
}
