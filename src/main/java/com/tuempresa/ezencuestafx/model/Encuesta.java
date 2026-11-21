package com.tuempresa.ezencuestafx.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Encuesta {
    private String titulo;
    private final ObservableList<Pregunta> preguntas;

    public Encuesta(String titulo) {
        this.titulo = titulo;
        this.preguntas = FXCollections.observableArrayList();
    }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public ObservableList<Pregunta> getPreguntas() { return preguntas; }

    public void agregarPregunta(Pregunta pregunta) {
        preguntas.add(pregunta);
    }

    public void eliminarPregunta(Pregunta pregunta) {
        preguntas.remove(pregunta);
    }
}