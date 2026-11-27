package com.tuempresa.ezencuestafx.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/** Animaciones reutilizables para que la interfaz se sienta más fluida. */
public final class Animaciones {

    private Animaciones() { }

    /** El nodo aparece desvaneciéndose y subiendo un poco. */
    public static void aparecer(Node nodo, double retrasoMs) {
        nodo.setOpacity(0);
        nodo.setTranslateY(18);

        FadeTransition fade = new FadeTransition(Duration.millis(420), nodo);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition subir = new TranslateTransition(Duration.millis(420), nodo);
        subir.setFromY(18);
        subir.setToY(0);
        subir.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition animacion = new ParallelTransition(fade, subir);
        animacion.setDelay(Duration.millis(retrasoMs));
        animacion.play();
    }

    /** Solo un desvanecimiento de entrada (para ventanas completas). */
    public static void fundidoEntrada(Node nodo, double duracionMs) {
        nodo.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(duracionMs), nodo);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    /** El nodo se encoge y desvanece; al terminar se ejecuta la acción indicada. */
    public static void desaparecer(Node nodo, Runnable alTerminar) {
        FadeTransition fade = new FadeTransition(Duration.millis(220), nodo);
        fade.setToValue(0);

        ScaleTransition encoger = new ScaleTransition(Duration.millis(220), nodo);
        encoger.setToX(0.94);
        encoger.setToY(0.94);

        ParallelTransition animacion = new ParallelTransition(fade, encoger);
        animacion.setOnFinished(e -> alTerminar.run());
        animacion.play();
    }

    /** Sacude el nodo de lado a lado (por ejemplo, cuando la contraseña es incorrecta). */
    public static void sacudir(Node nodo) {
        TranslateTransition sacudida = new TranslateTransition(Duration.millis(55), nodo);
        sacudida.setFromX(0);
        sacudida.setByX(10);
        sacudida.setCycleCount(6);
        sacudida.setAutoReverse(true);
        sacudida.setOnFinished(e -> nodo.setTranslateX(0));
        sacudida.play();
    }

    /** Pequeño "latido" para llamar la atención sobre un cambio (ej. el contador de preguntas). */
    public static void resaltar(Node nodo) {
        ScaleTransition crecer = new ScaleTransition(Duration.millis(140), nodo);
        crecer.setToX(1.15);
        crecer.setToY(1.15);
        ScaleTransition volver = new ScaleTransition(Duration.millis(160), nodo);
        volver.setToX(1);
        volver.setToY(1);
        new SequentialTransition(crecer, volver).play();
    }

    /** El nodo crece un poquito cuando el mouse pasa por encima. */
    public static void efectoHover(Node nodo) {
        ScaleTransition entrar = new ScaleTransition(Duration.millis(120), nodo);
        entrar.setToX(1.03);
        entrar.setToY(1.03);
        ScaleTransition salir = new ScaleTransition(Duration.millis(120), nodo);
        salir.setToX(1);
        salir.setToY(1);
        nodo.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> { salir.stop(); entrar.playFromStart(); });
        nodo.addEventHandler(MouseEvent.MOUSE_EXITED, e -> { entrar.stop(); salir.playFromStart(); });
    }

    /** Movimiento suave e infinito de ida y vuelta (fondo decorativo del login). */
    public static void flotar(Node nodo, double desplazamientoX, double desplazamientoY, double segundos) {
        TranslateTransition flotar = new TranslateTransition(Duration.seconds(segundos), nodo);
        flotar.setByX(desplazamientoX);
        flotar.setByY(desplazamientoY);
        flotar.setAutoReverse(true);
        flotar.setCycleCount(Timeline.INDEFINITE);
        flotar.setInterpolator(Interpolator.EASE_BOTH);
        flotar.play();
    }
}
