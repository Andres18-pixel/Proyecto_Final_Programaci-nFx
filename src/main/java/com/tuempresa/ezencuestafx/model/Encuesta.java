package com.tuempresa.ezencuestafx.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Encuesta {
    /** Id en la base de datos (null si la encuesta todavía no se ha guardado). */
    private Integer id;
    private String titulo;
    private final ObservableList<Pregunta> preguntas;

    public Encuesta(String titulo) {
        this.titulo = titulo;
        this.preguntas = FXCollections.observableArrayList();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public ObservableList<Pregunta> getPreguntas() { return preguntas; }

    public void agregarPregunta(Pregunta pregunta) {
        preguntas.add(pregunta);
    }

    public void eliminarPregunta(Pregunta pregunta) {
        preguntas.remove(pregunta);
    }

    /** Indica si la encuesta ya contiene la pregunta del banco con ese id. */
    public boolean contienePreguntaConId(Integer idPregunta) {
        return idPregunta != null && preguntas.stream().anyMatch(p -> idPregunta.equals(p.getId()));
    }
}
