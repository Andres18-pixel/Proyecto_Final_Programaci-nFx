package com.tuempresa.ezencuestafx.model;

import java.util.ArrayList;
import java.util.List;

public class Pregunta {
    private String enunciado;
    private TipoPregunta tipo;
    private boolean obligatoria;
    private List<String> opciones;

    public Pregunta(String enunciado, TipoPregunta tipo, boolean obligatoria, List<String> opciones) {
        this.enunciado = enunciado;
        this.tipo = tipo;
        this.obligatoria = obligatoria;
        this.opciones = (opciones != null) ? new ArrayList<>(opciones) : new ArrayList<>();
    }

    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }

    public TipoPregunta getTipo() { return tipo; }
    public void setTipo(TipoPregunta tipo) { this.tipo = tipo; }

    public boolean isObligatoria() { return obligatoria; }
    public void setObligatoria(boolean obligatoria) { this.obligatoria = obligatoria; }

    public List<String> getOpciones() { return opciones; }
    public void setOpciones(List<String> opciones) { this.opciones = opciones; }
}