package com.tuempresa.ezencuestafx.controller;

import com.tuempresa.ezencuestafx.HelloApplication;
import com.tuempresa.ezencuestafx.db.Conexion;
import com.tuempresa.ezencuestafx.db.UsuarioDAO;
import com.tuempresa.ezencuestafx.model.Rol;
import com.tuempresa.ezencuestafx.util.Animaciones;
import com.tuempresa.ezencuestafx.util.Navegacion;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class SignupController {

    private static final String PATRON_CORREO = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$";
    private static final String PATRON_USUARIO = "^[A-Za-z0-9._]{4,20}$";

    @FXML private StackPane raiz;
    @FXML private VBox tarjeta;
    @FXML private Circle burbuja1;
    @FXML private Circle burbuja2;
    @FXML private Circle burbuja3;
    @FXML private Circle burbuja4;

    @FXML private TextField txtNombre;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private PasswordField txtConfirmar;
    @FXML private ToggleButton tglDocente;
    @FXML private ToggleButton tglEstudiante;
    @FXML private Label lblMensaje;
    @FXML private Button btnRegistrar;

    private final ToggleGroup grupoRol = new ToggleGroup();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    public void initialize() {
        tglDocente.setToggleGroup(grupoRol);
        tglEstudiante.setToggleGroup(grupoRol);
        tglDocente.setUserData(Rol.DOCENTE);
        tglEstudiante.setUserData(Rol.ESTUDIANTE_INVESTIGACION);

        // Evita que el usuario deje ningún rol marcado al volver a hacer clic en el seleccionado
        grupoRol.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null && anterior != null) {
                anterior.setSelected(true);
            }
        });

        Animaciones.aparecer(tarjeta, 150);
        Animaciones.flotar(burbuja1, 40, 30, 6);
        Animaciones.flotar(burbuja2, -50, -35, 8);
        Animaciones.flotar(burbuja3, -25, 40, 5);
        Animaciones.flotar(burbuja4, 30, -25, 7);
        Animaciones.efectoHover(btnRegistrar);
        Platform.runLater(txtNombre::requestFocus);
    }

    @FXML
    private void handleRegistrar() {
        String nombre = txtNombre.getText().trim();
        String correo = txtCorreo.getText().trim();
        String usuario = txtUsuario.getText().trim();
        String contrasena = txtContrasena.getText();
        String confirmar = txtConfirmar.getText();

        if (nombre.isEmpty() || correo.isEmpty() || usuario.isEmpty() || contrasena.isEmpty() || confirmar.isEmpty()) {
            mostrarError("Completa todos los campos.");
            return;
        }
        if (!correo.matches(PATRON_CORREO)) {
            mostrarError("El correo no tiene un formato válido.");
            txtCorreo.requestFocus();
            return;
        }
        if (!usuario.matches(PATRON_USUARIO)) {
            mostrarError("El usuario debe tener de 4 a 20 caracteres: letras, números, punto o guion bajo (sin espacios).");
            txtUsuario.requestFocus();
            return;
        }
        if (contrasena.length() < 6) {
            mostrarError("La contraseña debe tener al menos 6 caracteres.");
            txtContrasena.requestFocus();
            return;
        }
        if (!contrasena.equals(confirmar)) {
            mostrarError("Las contraseñas no coinciden.");
            txtConfirmar.clear();
            txtConfirmar.requestFocus();
            return;
        }
        if (grupoRol.getSelectedToggle() == null) {
            mostrarError("Elige si eres Docente o Estudiante de investigación.");
            return;
        }
        Rol rol = (Rol) grupoRol.getSelectedToggle().getUserData();

        try {
            if (usuarioDAO.existeUsuario(usuario)) {
                mostrarError("El usuario \"" + usuario + "\" ya está en uso. Prueba con otro.");
                txtUsuario.requestFocus();
                return;
            }
            if (usuarioDAO.existeCorreo(correo)) {
                mostrarError("Ya existe una cuenta con ese correo.");
                txtCorreo.requestFocus();
                return;
            }

            usuarioDAO.registrar(nombre, correo, usuario, contrasena, rol);

            LoginController login = Navegacion.cambiarPantalla(ventana(), "login-view.fxml",
                    "ezEncuestaFX - Iniciar sesión", HelloApplication.ANCHO_AUTH, HelloApplication.ALTO_AUTH);
            login.prellenarUsuario(usuario);
        } catch (SQLException e) {
            mostrarError(Conexion.mensajeAmigable(e));
        } catch (IOException e) {
            mostrarError("No se pudo volver al inicio de sesión: " + e.getMessage());
        }
    }

    @FXML
    private void handleIrALogin() {
        try {
            Navegacion.cambiarPantalla(ventana(), "login-view.fxml", "ezEncuestaFX - Iniciar sesión",
                    HelloApplication.ANCHO_AUTH, HelloApplication.ALTO_AUTH);
        } catch (IOException e) {
            mostrarError("No se pudo abrir el inicio de sesión: " + e.getMessage());
        }
    }

    private void mostrarError(String texto) {
        lblMensaje.setText(texto);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
        Animaciones.sacudir(tarjeta);
    }

    private Stage ventana() {
        return (Stage) raiz.getScene().getWindow();
    }
}
