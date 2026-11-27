package com.tuempresa.ezencuestafx.util;

import com.tuempresa.ezencuestafx.HelloApplication;
import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

/** Cambia de pantalla (login, registro, constructor) con una transición de desvanecido. */
public final class Navegacion {

    public static final String ESTILOS = "estilos.css";

    private Navegacion() { }

    /**
     * Carga el FXML indicado y lo muestra en la ventana.
     * @return el controlador de la nueva pantalla, por si hay que pasarle datos.
     */
    public static <T> T cambiarPantalla(Stage ventana, String fxml, String titulo, double ancho, double alto)
            throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxml));
        Parent nuevaRaiz = loader.load();

        Runnable mostrar = () -> {
            Scene escena = new Scene(nuevaRaiz, ancho, alto);
            aplicarEstilos(escena);
            ventana.setTitle(titulo);
            ventana.setScene(escena);
            ventana.centerOnScreen();
            Animaciones.fundidoEntrada(nuevaRaiz, 350);
        };

        Scene actual = ventana.getScene();
        if (actual == null) {
            mostrar.run();
        } else {
            FadeTransition salida = new FadeTransition(Duration.millis(200), actual.getRoot());
            salida.setToValue(0);
            salida.setOnFinished(e -> mostrar.run());
            salida.play();
        }
        return loader.getController();
    }

    public static void aplicarEstilos(Scene escena) {
        escena.getStylesheets().add(HelloApplication.class.getResource(ESTILOS).toExternalForm());
    }
}
