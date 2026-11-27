package com.tuempresa.ezencuestafx.model;

import java.util.ArrayList;
import java.util.List;

public class Pregunta {
    /** Id en la base de datos (null mientras la pregunta no se haya guardado en el banco). */
    private Integer id;
    private String enunciado;
    private TipoPregunta tipo;
    private boolean obligatoria;
    private List<String> opciones;
    /** true si la pregunta se trajo del banco de preguntas (se usó antes). */
    private boolean reutilizada;

    public Pregunta(String enunciado, TipoPregunta tipo, boolean obligatoria, List<String> opciones) {
        this(null, enunciado, tipo, obligatoria, opciones);
    }

    public Pregunta(Integer id, String enunciado, TipoPregunta tipo, boolean obligatoria, List<String> opciones) {
        this.id = id;
        this.enunciado = enunciado;
        this.tipo = tipo;
        this.obligatoria = obligatoria;
        this.opciones = (opciones != null) ? new ArrayList<>(opciones) : new ArrayList<>();
    }

    /** Crea una copia independiente (para usar una pregunta del banco sin modificar la original). */
    public Pregunta copia() {
        Pregunta c = new Pregunta(id, enunciado, tipo, obligatoria, opciones);
        c.reutilizada = reutilizada;
        return c;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }

    public TipoPregunta getTipo() { return tipo; }
    public void setTipo(TipoPregunta tipo) { this.tipo = tipo; }

    public boolean isObligatoria() { return obligatoria; }
    public void setObligatoria(boolean obligatoria) { this.obligatoria = obligatoria; }

    public List<String> getOpciones() { return opciones; }
    public void setOpciones(List<String> opciones) { this.opciones = opciones; }

    public boolean isReutilizada() { return reutilizada; }
    public void setReutilizada(boolean reutilizada) { this.reutilizada = reutilizada; }
}
