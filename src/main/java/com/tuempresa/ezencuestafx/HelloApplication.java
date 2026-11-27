package com.tuempresa.ezencuestafx;

import com.tuempresa.ezencuestafx.util.Navegacion;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    public static final double ANCHO_AUTH = 980;
    public static final double ALTO_AUTH = 700;

    @Override
    public void start(Stage stage) throws IOException {
        stage.setMinWidth(820);
        stage.setMinHeight(600);
        Navegacion.cambiarPantalla(stage, "login-view.fxml", "ezEncuestaFX - Iniciar sesión", ANCHO_AUTH, ALTO_AUTH);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
