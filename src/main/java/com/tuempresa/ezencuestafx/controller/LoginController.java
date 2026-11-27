package com.tuempresa.ezencuestafx.controller;

import com.tuempresa.ezencuestafx.HelloApplication;
import com.tuempresa.ezencuestafx.db.Conexion;
import com.tuempresa.ezencuestafx.db.UsuarioDAO;
import com.tuempresa.ezencuestafx.model.Usuario;
import com.tuempresa.ezencuestafx.util.Animaciones;
import com.tuempresa.ezencuestafx.util.Navegacion;
import com.tuempresa.ezencuestafx.util.Sesion;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML private StackPane raiz;
    @FXML private VBox tarjeta;
    @FXML private Circle burbuja1;
    @FXML private Circle burbuja2;
    @FXML private Circle burbuja3;
    @FXML private Circle burbuja4;

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private Label lblMensaje;
    @FXML private Button btnIngresar;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    public void initialize() {
        Animaciones.aparecer(tarjeta, 150);
        Animaciones.flotar(burbuja1, 40, 30, 6);
        Animaciones.flotar(burbuja2, -50, -35, 8);
        Animaciones.flotar(burbuja3, -25, 40, 5);
        Animaciones.flotar(burbuja4, 30, -25, 7);
        Animaciones.efectoHover(btnIngresar);
        Platform.runLater(txtUsuario::requestFocus);
    }

    /** Lo usa la pantalla de registro para dejar listo el usuario recién creado. */
    public void prellenarUsuario(String usuario) {
        txtUsuario.setText(usuario);
        mostrarMensaje("¡Cuenta creada! Ahora escribe tu contraseña para entrar.", false);
        Platform.runLater(txtContrasena::requestFocus);
    }

    @FXML
    private void handleIngresar() {
        String usuario = txtUsuario.getText().trim();
        String contrasena = txtContrasena.getText();

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            mostrarError("Escribe tu usuario (o correo) y tu contraseña.");
            return;
        }

        try {
            Usuario encontrado = usuarioDAO.autenticar(usuario, contrasena);
            if (encontrado == null) {
                txtContrasena.clear();
                txtContrasena.requestFocus();
                mostrarError("Usuario o contraseña incorrectos.");
                return;
            }
            Sesion.iniciar(encontrado);
            Navegacion.cambiarPantalla(ventana(), "encuesta-view.fxml",
                    "ezEncuestaFX - Constructor de Encuestas", 1100, 720);
        } catch (SQLException e) {
            mostrarError(Conexion.mensajeAmigable(e));
        } catch (IOException e) {
            mostrarError("No se pudo abrir el constructor de encuestas: " + e.getMessage());
        }
    }

    @FXML
    private void handleIrARegistro() {
        try {
            Navegacion.cambiarPantalla(ventana(), "signup-view.fxml", "ezEncuestaFX - Crear cuenta",
                    HelloApplication.ANCHO_AUTH, HelloApplication.ALTO_AUTH);
        } catch (IOException e) {
            mostrarError("No se pudo abrir el registro: " + e.getMessage());
        }
    }

    private void mostrarError(String texto) {
        mostrarMensaje(texto, true);
        Animaciones.sacudir(tarjeta);
    }

    private void mostrarMensaje(String texto, boolean esError) {
        lblMensaje.setText(texto);
        lblMensaje.getStyleClass().removeAll("mensaje-error", "mensaje-exito");
        lblMensaje.getStyleClass().add(esError ? "mensaje-error" : "mensaje-exito");
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }

    private Stage ventana() {
        return (Stage) raiz.getScene().getWindow();
    }
}
