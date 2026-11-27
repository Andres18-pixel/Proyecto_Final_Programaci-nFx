package com.tuempresa.ezencuestafx.controller;

import com.tuempresa.ezencuestafx.db.Conexion;
import com.tuempresa.ezencuestafx.db.PreguntaDAO;
import com.tuempresa.ezencuestafx.model.Encuesta;
import com.tuempresa.ezencuestafx.model.Pregunta;
import com.tuempresa.ezencuestafx.model.PreguntaGuardada;
import com.tuempresa.ezencuestafx.model.Rol;
import com.tuempresa.ezencuestafx.model.TipoPregunta;
import com.tuempresa.ezencuestafx.model.Usuario;
import com.tuempresa.ezencuestafx.util.Animaciones;
import com.tuempresa.ezencuestafx.util.Sesion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Ventana donde el usuario ve las preguntas que ya creó antes y elige
 * cuáles volver a usar en la encuesta actual.
 */
public class BancoPreguntasController {

    private static final String TODOS_LOS_TIPOS = "Todos los tipos";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private BorderPane raiz;
    @FXML private Label lblDescripcion;
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbFiltroTipo;

    @FXML private TableView<PreguntaGuardada> tblPreguntas;
    @FXML private TableColumn<PreguntaGuardada, String> colEnunciado;
    @FXML private TableColumn<PreguntaGuardada, String> colTipo;
    @FXML private TableColumn<PreguntaGuardada, String> colUsos;

    @FXML private Label lblDetalleEnunciado;
    @FXML private Label lblDetalleTipo;
    @FXML private Label lblDetalleUsos;
    @FXML private Label lblTituloOpciones;
    @FXML private ListView<String> lstDetalleOpciones;

    @FXML private Label lblResumen;
    @FXML private Button btnEliminar;
    @FXML private Button btnAgregar;

    private final PreguntaDAO preguntaDAO = new PreguntaDAO();
    private final ObservableList<PreguntaGuardada> todas = FXCollections.observableArrayList();
    private final FilteredList<PreguntaGuardada> filtradas = new FilteredList<>(todas, p -> true);
    private final List<Pregunta> seleccionadas = new ArrayList<>();
    private Encuesta encuestaDestino;

    @FXML
    public void initialize() {
        colEnunciado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().pregunta().getEnunciado()));
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().pregunta().getTipo().toString()));
        colUsos.setCellValueFactory(c -> {
            int veces = c.getValue().vecesUsada();
            return new SimpleStringProperty(veces + (veces == 1 ? " encuesta" : " encuestas"));
        });

        tblPreguntas.setItems(filtradas);
        tblPreguntas.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tblPreguntas.setPlaceholder(new Label("Todavía no tienes preguntas guardadas.\n"
                + "Las preguntas que agregues a tus encuestas aparecerán aquí."));

        // Las preguntas que ya están en la encuesta se ven en gris; doble clic agrega la pregunta
        tblPreguntas.setRowFactory(tabla -> {
            TableRow<PreguntaGuardada> fila = new TableRow<>() {
                @Override
                protected void updateItem(PreguntaGuardada item, boolean vacia) {
                    super.updateItem(item, vacia);
                    boolean yaEsta = !vacia && item != null && yaEstaEnLaEncuesta(item);
                    setStyle(yaEsta ? "-fx-opacity: 0.55;" : "");
                }
            };
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    handleAgregar();
                }
            });
            return fila;
        });

        tblPreguntas.getSelectionModel().selectedItemProperty().addListener((obs, a, nueva) -> mostrarDetalle(nueva));

        List<String> filtros = new ArrayList<>();
        filtros.add(TODOS_LOS_TIPOS);
        for (TipoPregunta t : TipoPregunta.values()) {
            filtros.add(t.toString());
        }
        cmbFiltroTipo.setItems(FXCollections.observableArrayList(filtros));
        cmbFiltroTipo.getSelectionModel().selectFirst();

        txtBuscar.textProperty().addListener((obs, a, b) -> aplicarFiltro());
        cmbFiltroTipo.valueProperty().addListener((obs, a, b) -> aplicarFiltro());

        Animaciones.efectoHover(btnAgregar);
        mostrarDetalle(null);
    }

    /** Carga las preguntas del usuario que inició sesión. */
    public void cargar(Encuesta encuestaActual) {
        this.encuestaDestino = encuestaActual;
        Usuario usuario = Sesion.getUsuario();

        String quien = usuario.getRol() == Rol.DOCENTE ? "tus clases" : "tus investigaciones";
        lblDescripcion.setText("Hola " + usuario.getPrimerNombre() + ", aquí están las preguntas que ya usaste en "
                + quien + ". Selecciona una o varias (Ctrl + clic) y agrégalas a \"" + encuestaActual.getTitulo() + "\".");

        try {
            todas.setAll(preguntaDAO.listarBanco(usuario.getId()));
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Base de datos", Conexion.mensajeAmigable(e));
        }
        actualizarResumen();
    }

    /** Preguntas elegidas por el usuario (copias listas para agregar a la encuesta). */
    public List<Pregunta> getSeleccionadas() {
        return seleccionadas;
    }

    @FXML
    private void handleAgregar() {
        List<PreguntaGuardada> marcadas = new ArrayList<>(tblPreguntas.getSelectionModel().getSelectedItems());
        if (marcadas.isEmpty()) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Nada seleccionado",
                    "Selecciona al menos una pregunta de la lista para agregarla.");
            return;
        }

        int repetidas = 0;
        for (PreguntaGuardada g : marcadas) {
            if (yaEstaEnLaEncuesta(g)) {
                repetidas++;
                continue;
            }
            Pregunta copia = g.pregunta().copia();
            copia.setReutilizada(true);
            seleccionadas.add(copia);
        }

        if (seleccionadas.isEmpty()) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Ya está en la encuesta",
                    repetidas == 1 ? "Esa pregunta ya forma parte de la encuesta actual."
                            : "Esas preguntas ya forman parte de la encuesta actual.");
            return;
        }
        cerrar();
    }

    @FXML
    private void handleEliminar() {
        PreguntaGuardada elegida = tblPreguntas.getSelectionModel().getSelectedItem();
        if (elegida == null) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Nada seleccionado", "Selecciona la pregunta que deseas eliminar del banco.");
            return;
        }
        if (elegida.vecesUsada() > 0) {
            mostrarAlerta(Alert.AlertType.WARNING, "No se puede eliminar",
                    "Esta pregunta ya se usó en: " + elegida.usadaEn()
                            + ".\nPara no dañar esas encuestas, no se puede quitar del banco.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar \"" + elegida.pregunta().getEnunciado() + "\" de tu banco de preguntas?",
                ButtonType.YES, ButtonType.NO);
        confirmacion.setTitle("Eliminar del banco");
        confirmacion.setHeaderText(null);
        confirmacion.initOwner(ventana());
        if (confirmacion.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            preguntaDAO.eliminar(Sesion.getUsuario().getId(), elegida.pregunta().getId());
            todas.remove(elegida);
            actualizarResumen();
        } catch (SQLException e) {
            if (PreguntaDAO.SQLSTATE_EN_USO.equals(e.getSQLState())) {
                mostrarAlerta(Alert.AlertType.WARNING, "No se puede eliminar",
                        "La pregunta se usa en alguna encuesta guardada.");
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Base de datos", Conexion.mensajeAmigable(e));
            }
        }
    }

    @FXML
    private void handleCancelar() {
        seleccionadas.clear();
        cerrar();
    }

    private void aplicarFiltro() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        String tipo = cmbFiltroTipo.getValue();

        filtradas.setPredicate(g -> {
            Pregunta p = g.pregunta();
            boolean coincideTipo = tipo == null || TODOS_LOS_TIPOS.equals(tipo) || p.getTipo().toString().equals(tipo);
            boolean coincideTexto = texto.isEmpty()
                    || p.getEnunciado().toLowerCase().contains(texto)
                    || p.getOpciones().stream().anyMatch(o -> o.toLowerCase().contains(texto));
            return coincideTipo && coincideTexto;
        });
        actualizarResumen();
    }

    private void mostrarDetalle(PreguntaGuardada g) {
        if (g == null) {
            lblDetalleEnunciado.setText("Selecciona una pregunta de la lista para ver su detalle.");
            lblDetalleTipo.setText("");
            lblDetalleUsos.setText("");
            lblTituloOpciones.setVisible(false);
            lstDetalleOpciones.setVisible(false);
            return;
        }

        Pregunta p = g.pregunta();
        lblDetalleEnunciado.setText(p.getEnunciado());
        lblDetalleTipo.setText("Tipo: " + p.getTipo() + (p.isObligatoria() ? "   ·   Obligatoria" : "   ·   Opcional"));

        String usos = g.vecesUsada() == 0
                ? "Guardada el " + g.creadaEn().format(FORMATO_FECHA) + ", aún no se usa en encuestas guardadas."
                : "Usada en: " + g.usadaEn();
        if (yaEstaEnLaEncuesta(g)) {
            usos += "\n(Ya está en la encuesta actual)";
        }
        lblDetalleUsos.setText(usos);

        boolean tieneOpciones = !p.getOpciones().isEmpty();
        lblTituloOpciones.setVisible(tieneOpciones);
        lstDetalleOpciones.setVisible(tieneOpciones);
        lstDetalleOpciones.setItems(FXCollections.observableArrayList(p.getOpciones()));
    }

    private boolean yaEstaEnLaEncuesta(PreguntaGuardada g) {
        return encuestaDestino != null && encuestaDestino.contienePreguntaConId(g.pregunta().getId());
    }

    private void actualizarResumen() {
        int total = todas.size();
        int visibles = filtradas.size();
        String texto = total + (total == 1 ? " pregunta en tu banco" : " preguntas en tu banco");
        if (visibles != total) {
            texto += "  ·  mostrando " + visibles;
        }
        lblResumen.setText(texto);
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        if (raiz.getScene() != null) {
            alert.initOwner(ventana());
        }
        alert.showAndWait();
    }

    private Stage ventana() {
        return (Stage) raiz.getScene().getWindow();
    }

    private void cerrar() {
        ventana().close();
    }
}
