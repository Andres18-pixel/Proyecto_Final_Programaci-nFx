package com.tuempresa.ezencuestafx.controller;

import com.tuempresa.ezencuestafx.HelloApplication;
import com.tuempresa.ezencuestafx.db.Conexion;
import com.tuempresa.ezencuestafx.db.EncuestaDAO;
import com.tuempresa.ezencuestafx.db.PreguntaDAO;
import com.tuempresa.ezencuestafx.model.Encuesta;
import com.tuempresa.ezencuestafx.model.EncuestaResumen;
import com.tuempresa.ezencuestafx.model.Pregunta;
import com.tuempresa.ezencuestafx.model.TipoPregunta;
import com.tuempresa.ezencuestafx.model.Usuario;
import com.tuempresa.ezencuestafx.util.Animaciones;
import com.tuempresa.ezencuestafx.util.Iconos;
import com.tuempresa.ezencuestafx.util.Navegacion;
import com.tuempresa.ezencuestafx.util.Sesion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.poi.xwpf.usermodel.*;
import java.util.Collections;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class EncuestaController {

    @FXML private TextField txtTituloEncuesta;
    @FXML private TextField txtEnunciado;
    @FXML private ComboBox<TipoPregunta> cmbTipoPregunta;
    @FXML private CheckBox chkObligatoria;

    @FXML private VBox boxOpcionesConfig;
    @FXML private TextField txtNuevaOpcion;
    @FXML private ListView<String> lstOpciones;

    @FXML private Label lblPreviewTitulo;
    @FXML private VBox contenedorPreguntasPreview;
    @FXML private Label lblContador;

    @FXML private Label lblUsuario;
    @FXML private Label lblEstado;
    @FXML private MenuItem itemDatosUsuario;
    @FXML private Button btnAgregarPregunta;
    @FXML private Button btnUsarGuardada;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelarEdicion;
    @FXML private Label lblTituloFormulario;

    private Encuesta encuestaActual = new Encuesta("Nueva Encuesta");
    private final ObservableList<String> opcionesTemporales = FXCollections.observableArrayList();
    private final Map<Pregunta, Node> controlesRespuesta = new HashMap<>();

    private final PreguntaDAO preguntaDAO = new PreguntaDAO();
    private final EncuestaDAO encuestaDAO = new EncuestaDAO();

    /** Tarjetas que deben aparecer con animación la próxima vez que se dibuje la vista previa. */
    private final Set<Pregunta> porAnimar = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean cambiosSinGuardar = false;
    private boolean cambiandoTituloPorCodigo = false;

    /** Pregunta que se está editando en el panel izquierdo (null si se está creando una nueva). */
    private Pregunta preguntaEnEdicion;
    /** Posición de la pregunta / opción que se está arrastrando (-1 si no se arrastra nada). */
    private int indicePreguntaArrastrada = -1;
    private int indiceOpcionArrastrada = -1;

    @FXML
    public void initialize() {
        cmbTipoPregunta.setItems(FXCollections.observableArrayList(TipoPregunta.values()));
        cmbTipoPregunta.getSelectionModel().select(TipoPregunta.TEXTO);
        lstOpciones.setItems(opcionesTemporales);
        lstOpciones.setCellFactory(lista -> new CeldaOpcion());

        txtTituloEncuesta.textProperty().addListener((obs, oldVal, newVal) -> {
            encuestaActual.setTitulo(newVal);
            lblPreviewTitulo.setText(newVal.isBlank() ? "Sin Título" : newVal);
            if (!cambiandoTituloPorCodigo) {
                marcarCambios();
            }
        });

        cmbTipoPregunta.valueProperty().addListener((obs, oldTipo, nuevoTipo) -> {
            boolean requiereOpciones = (nuevoTipo == TipoPregunta.SELECCION_UNICA || nuevoTipo == TipoPregunta.SELECCION_MULTIPLE);
            boxOpcionesConfig.setVisible(requiereOpciones);
            boxOpcionesConfig.setManaged(requiereOpciones);
        });

        Usuario usuario = Sesion.getUsuario();
        if (usuario != null) {
            lblUsuario.setText(usuario.getPrimerNombre() + "  ·  " + usuario.getRol());
            itemDatosUsuario.setText(usuario.getNombreCompleto() + " (" + usuario.getUsuario() + ")");
        }

        Animaciones.efectoHover(btnAgregarPregunta);
        Animaciones.efectoHover(btnUsarGuardada);
        Animaciones.efectoHover(btnGuardar);

        // Preguntar por cambios sin guardar si cierran la ventana con la X
        txtTituloEncuesta.sceneProperty().addListener((obsEscena, escenaAnterior, escena) -> {
            if (escena != null) {
                escena.windowProperty().addListener((obsVentana, anterior, ventana) -> {
                    if (ventana != null) {
                        ventana.setOnCloseRequest(evento -> {
                            if (!confirmarDescartarCambios()) {
                                evento.consume();
                            }
                        });
                    }
                });
            }
        });

        actualizarEstado();
        renderizarVistaPrevia();
    }

    @FXML
    private void handleAgregarOpcion() {
        String opcion = txtNuevaOpcion.getText().trim();

        if (opcion.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Opción vacía", "Escribe el texto de la opción antes de añadirla.");
            return;
        }

        boolean yaExiste = opcionesTemporales.stream().anyMatch(o -> o.equalsIgnoreCase(opcion));
        if (yaExiste) {
            mostrarAlerta(Alert.AlertType.WARNING, "Opción repetida", "La opción \"" + opcion + "\" ya fue agregada a esta pregunta.");
            txtNuevaOpcion.selectAll();
            txtNuevaOpcion.requestFocus();
            return;
        }

        opcionesTemporales.add(opcion);
        txtNuevaOpcion.clear();
        txtNuevaOpcion.requestFocus();
    }

    @FXML
    private void handleAgregarPregunta() {
        Pregunta nueva = leerFormulario();
        if (nueva == null) {
            return;
        }
        if (preguntaEnEdicion != null) {
            guardarEdicion(nueva);
            return;
        }

        if (yaEstaEnLaEncuesta(nueva, null)) {
            mostrarAlerta(Alert.AlertType.WARNING, "Pregunta repetida",
                    "Esa misma pregunta (con el mismo tipo y opciones) ya está en la encuesta.");
            return;
        }
        guardarEnBanco(nueva);
        if (encuestaActual.contienePreguntaConId(nueva.getId())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Pregunta repetida",
                    "Esa pregunta ya está en la encuesta (es la misma que tienes guardada en tu banco).");
            return;
        }
        encuestaActual.agregarPregunta(nueva);
        porAnimar.add(nueva);
        marcarCambios();

        limpiarFormulario();
        renderizarVistaPrevia();
        Animaciones.resaltar(lblContador);
    }

    /** Lee y valida el formulario del panel izquierdo. @return la pregunta, o null si falta algo. */
    private Pregunta leerFormulario() {
        String enunciado = txtEnunciado.getText().trim();
        TipoPregunta tipo = cmbTipoPregunta.getValue();
        boolean obligatoria = chkObligatoria.isSelected();

        if (enunciado.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo requerido", "Debes escribir el enunciado de la pregunta.");
            return null;
        }

        boolean requiereOpciones = (tipo == TipoPregunta.SELECCION_UNICA || tipo == TipoPregunta.SELECCION_MULTIPLE);
        if (requiereOpciones && opcionesTemporales.size() < 2) {
            mostrarAlerta(Alert.AlertType.WARNING, "Faltan opciones", "Agrega al menos 2 opciones para preguntas de selección.");
            return null;
        }

        // Si el tipo no es de selección, no se guardan opciones aunque hayan quedado escritas
        List<String> opciones = requiereOpciones ? new ArrayList<>(opcionesTemporales) : new ArrayList<>();
        return new Pregunta(enunciado, tipo, obligatoria, opciones);
    }

    /**
     * Compara enunciado (sin importar mayúsculas), tipo y opciones con las preguntas de la encuesta.
     * @param ignorar pregunta que no se toma en cuenta (la que se está editando), o null
     */
    private boolean yaEstaEnLaEncuesta(Pregunta nueva, Pregunta ignorar) {
        return encuestaActual.getPreguntas().stream().anyMatch(p -> p != ignorar
                && p.getEnunciado().trim().equalsIgnoreCase(nueva.getEnunciado().trim())
                && p.getTipo() == nueva.getTipo()
                && p.getOpciones().equals(nueva.getOpciones()));
    }

    // ==================== EDITAR UNA PREGUNTA (ÍCONO DE LÁPIZ) ====================

    /** Carga la pregunta en el panel izquierdo para modificarla. */
    private void entrarEnEdicion(Pregunta p, int numero) {
        preguntaEnEdicion = p;
        txtEnunciado.setText(p.getEnunciado());
        cmbTipoPregunta.setValue(p.getTipo());
        chkObligatoria.setSelected(p.isObligatoria());
        opcionesTemporales.setAll(p.getOpciones());
        txtNuevaOpcion.clear();

        lblTituloFormulario.setText("Editando pregunta #" + numero);
        btnAgregarPregunta.setText("Guardar cambios de la pregunta");
        btnCancelarEdicion.setVisible(true);
        btnCancelarEdicion.setManaged(true);

        renderizarVistaPrevia();
        txtEnunciado.requestFocus();
        txtEnunciado.positionCaret(txtEnunciado.getText().length());
        Animaciones.resaltar(lblTituloFormulario);
    }

    private void guardarEdicion(Pregunta datos) {
        Pregunta p = preguntaEnEdicion;
        if (yaEstaEnLaEncuesta(datos, p)) {
            mostrarAlerta(Alert.AlertType.WARNING, "Pregunta repetida",
                    "Ya hay otra pregunta igual (mismo texto, tipo y opciones) en la encuesta.");
            return;
        }

        boolean cambioContenido = !p.getEnunciado().equals(datos.getEnunciado())
                || p.getTipo() != datos.getTipo()
                || !p.getOpciones().equals(datos.getOpciones());

        p.setObligatoria(datos.isObligatoria());
        if (cambioContenido) {
            p.setEnunciado(datos.getEnunciado());
            p.setTipo(datos.getTipo());
            p.setOpciones(new ArrayList<>(datos.getOpciones()));
            // La versión editada se guarda como otra pregunta del banco, para no
            // modificar las encuestas guardadas que usan la pregunta original.
            p.setId(null);
            p.setReutilizada(false);
            guardarEnBanco(p);
            controlesRespuesta.remove(p); // se vuelve a crear el campo de respuesta con el nuevo tipo
        }

        salirDeEdicion();
        porAnimar.add(p);
        marcarCambios();
        renderizarVistaPrevia();
    }

    @FXML
    private void handleCancelarEdicion() {
        salirDeEdicion();
        renderizarVistaPrevia();
    }

    private void salirDeEdicion() {
        preguntaEnEdicion = null;
        limpiarFormulario();
        lblTituloFormulario.setText("1. Configurar Nueva Pregunta");
        btnAgregarPregunta.setText("Agregar Pregunta a la Encuesta");
        btnCancelarEdicion.setVisible(false);
        btnCancelarEdicion.setManaged(false);
    }

    /** Guarda la pregunta en el banco del usuario para que pueda reutilizarla en otras encuestas. */
    private void guardarEnBanco(Pregunta pregunta) {
        if (!Sesion.activa()) {
            return;
        }
        try {
            preguntaDAO.guardarEnBanco(Sesion.getUsuario().getId(), pregunta);
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.WARNING, "Banco de preguntas",
                    "La pregunta se agregó a la encuesta, pero no se pudo guardar en tu banco de preguntas.\n\n"
                            + Conexion.mensajeAmigable(e));
        }
    }

    private void renderizarVistaPrevia() {
        contenedorPreguntasPreview.getChildren().clear();

        int numero = 1;
        int animadas = 0;
        for (Pregunta p : encuestaActual.getPreguntas()) {
            VBox tarjeta = new VBox(8);
            tarjeta.setPadding(new Insets(12));
            tarjeta.getStyleClass().add("tarjeta-pregunta");
            if (p == preguntaEnEdicion) {
                tarjeta.getStyleClass().add("en-edicion");
            }

            HBox header = new HBox(8);
            String textoObligatoria = p.isObligatoria() ? " *" : " (Opcional)";
            Label lblPregunta = new Label(numero + ". " + p.getEnunciado() + textoObligatoria);
            lblPregunta.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + (p.isObligatoria() ? "#0F172A;" : "#475569;"));
            lblPregunta.setWrapText(true);
            HBox.setHgrow(lblPregunta, Priority.ALWAYS);

            int indice = numero - 1;
            int numeroPregunta = numero;

            // Manija ⠿: se arrastra para cambiar la pregunta de lugar
            StackPane manija = new StackPane(Iconos.agarre());
            manija.getStyleClass().add("manija-arrastre");
            Tooltip.install(manija, new Tooltip("Arrastra para cambiar el orden"));
            configurarArrastrePregunta(manija, tarjeta, indice);

            Button btnEditar = botonIcono(Iconos.lapiz(), "Editar pregunta", false);
            btnEditar.setOnAction(e -> entrarEnEdicion(p, numeroPregunta));

            Button btnBorrar = botonIcono(Iconos.papelera(), "Eliminar pregunta", true);
            btnBorrar.setOnAction(e -> {
                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                        "¿Seguro que deseas eliminar la pregunta \"" + p.getEnunciado() + "\"?",
                        ButtonType.YES, ButtonType.NO);
                confirmacion.setTitle("Eliminar pregunta");
                confirmacion.setHeaderText(null);
                if (confirmacion.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                    Animaciones.desaparecer(tarjeta, () -> {
                        if (p == preguntaEnEdicion) {
                            salirDeEdicion();
                        }
                        encuestaActual.eliminarPregunta(p);
                        controlesRespuesta.remove(p);
                        marcarCambios();
                        renderizarVistaPrevia();
                        Animaciones.resaltar(lblContador);
                    });
                }
            });

            header.setAlignment(Pos.CENTER_LEFT);
            header.getChildren().addAll(manija, lblPregunta);
            if (p.isReutilizada()) {
                Label badge = new Label("Reutilizada");
                badge.getStyleClass().add("badge-reutilizada");
                badge.setTooltip(new Tooltip("Esta pregunta viene de tu banco de preguntas"));
                header.getChildren().add(badge);
            }
            header.getChildren().addAll(btnEditar, btnBorrar);
            tarjeta.getChildren().add(header);

            Node controlEntrada = controlesRespuesta.computeIfAbsent(
                    p, this::crearControlParaPregunta
            );
            tarjeta.getChildren().add(controlEntrada);

            contenedorPreguntasPreview.getChildren().add(tarjeta);
            if (porAnimar.remove(p)) {
                Animaciones.aparecer(tarjeta, 60.0 * animadas++);
            }
            numero++;
        }

        int total = encuestaActual.getPreguntas().size();
        lblContador.setText("(" + total + (total == 1 ? " pregunta)" : " preguntas)"));

        if (total == 0) {
            Label vacio = new Label("Aún no hay preguntas. Agrega una desde el panel izquierdo.");
            vacio.setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
            contenedorPreguntasPreview.getChildren().add(vacio);
            Animaciones.fundidoEntrada(vacio, 400);
        }
    }

    /** Botón transparente que solo muestra un ícono (lápiz, papelera...). */
    private Button botonIcono(Region icono, String ayuda, boolean peligro) {
        Button boton = new Button();
        boton.setGraphic(icono);
        boton.getStyleClass().add("boton-icono");
        if (peligro) {
            boton.getStyleClass().add("peligro");
        }
        boton.setTooltip(new Tooltip(ayuda));
        boton.setFocusTraversable(false);
        return boton;
    }

    // ==================== ARRASTRAR PREGUNTAS PARA CAMBIAR EL ORDEN ====================

    private void configurarArrastrePregunta(StackPane manija, VBox tarjeta, int indice) {
        manija.setOnDragDetected(e -> {
            Dragboard tablero = manija.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent contenido = new ClipboardContent();
            contenido.putString("pregunta-" + indice);
            tablero.setContent(contenido);
            SnapshotParameters parametros = new SnapshotParameters();
            parametros.setFill(Color.TRANSPARENT);
            tablero.setDragView(tarjeta.snapshot(parametros, null), 20, 20);
            indicePreguntaArrastrada = indice;
            tarjeta.setOpacity(0.4);
            e.consume();
        });

        manija.setOnDragDone(e -> {
            tarjeta.setOpacity(1);
            indicePreguntaArrastrada = -1;
            e.consume();
        });

        tarjeta.setOnDragOver(e -> {
            if (indicePreguntaArrastrada >= 0 && indicePreguntaArrastrada != indice) {
                e.acceptTransferModes(TransferMode.MOVE);
                boolean abajo = e.getY() > tarjeta.getHeight() / 2;
                tarjeta.getStyleClass().removeAll("destino-arriba", "destino-abajo");
                tarjeta.getStyleClass().add(abajo ? "destino-abajo" : "destino-arriba");
            }
            e.consume();
        });

        tarjeta.setOnDragExited(e -> {
            tarjeta.getStyleClass().removeAll("destino-arriba", "destino-abajo");
            e.consume();
        });

        tarjeta.setOnDragDropped(e -> {
            boolean movida = false;
            if (indicePreguntaArrastrada >= 0) {
                boolean abajo = e.getY() > tarjeta.getHeight() / 2;
                moverPregunta(indicePreguntaArrastrada, abajo ? indice + 1 : indice);
                movida = true;
            }
            e.setDropCompleted(movida);
            e.consume();
        });
    }

    private void moverPregunta(int desde, int hacia) {
        List<Pregunta> lista = encuestaActual.getPreguntas();
        int destino = hacia > desde ? hacia - 1 : hacia;
        if (destino == desde) {
            return;
        }
        Pregunta movida = lista.remove(desde);
        lista.add(destino, movida);
        porAnimar.add(movida);
        marcarCambios();
        Platform.runLater(this::renderizarVistaPrevia);
    }

    // ==================== OPCIONES: ARRASTRAR PARA ORDENAR Y PAPELERA ====================

    private void moverOpcion(int desde, int hacia) {
        int destino = hacia > desde ? hacia - 1 : hacia;
        if (destino == desde || desde < 0 || desde >= opcionesTemporales.size()) {
            return;
        }
        String opcion = opcionesTemporales.remove(desde);
        opcionesTemporales.add(Math.min(destino, opcionesTemporales.size()), opcion);
        lstOpciones.getSelectionModel().select(opcion);
    }

    /** Cada fila de la lista de opciones: manija ⠿ para arrastrar, el texto y una papelera. */
    private class CeldaOpcion extends ListCell<String> {
        private final HBox caja = new HBox(8);
        private final Label texto = new Label();
        private final StackPane manija = new StackPane(Iconos.agarre());
        private final Button borrar = botonIcono(Iconos.papelera(), "Quitar opción", true);

        CeldaOpcion() {
            getStyleClass().add("celda-opcion");
            manija.getStyleClass().add("manija-arrastre");
            Tooltip.install(manija, new Tooltip("Arrastra para cambiar el orden"));
            texto.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(texto, Priority.ALWAYS);
            caja.setAlignment(Pos.CENTER_LEFT);
            caja.getChildren().addAll(manija, texto, borrar);

            borrar.setOnAction(e -> {
                if (!isEmpty()) {
                    opcionesTemporales.remove(getIndex());
                }
            });

            manija.setOnDragDetected(e -> {
                if (isEmpty()) {
                    return;
                }
                Dragboard tablero = manija.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent contenido = new ClipboardContent();
                contenido.putString(getItem());
                tablero.setContent(contenido);
                SnapshotParameters parametros = new SnapshotParameters();
                parametros.setFill(Color.TRANSPARENT);
                tablero.setDragView(caja.snapshot(parametros, null), 10, 10);
                indiceOpcionArrastrada = getIndex();
                e.consume();
            });
            manija.setOnDragDone(e -> {
                indiceOpcionArrastrada = -1;
                e.consume();
            });

            setOnDragOver(e -> {
                if (indiceOpcionArrastrada >= 0) {
                    e.acceptTransferModes(TransferMode.MOVE);
                    getStyleClass().removeAll("destino-arriba", "destino-abajo");
                    if (!isEmpty()) {
                        getStyleClass().add(e.getY() > getHeight() / 2 ? "destino-abajo" : "destino-arriba");
                    }
                }
                e.consume();
            });
            setOnDragExited(e -> {
                getStyleClass().removeAll("destino-arriba", "destino-abajo");
                e.consume();
            });
            setOnDragDropped(e -> {
                boolean movida = false;
                if (indiceOpcionArrastrada >= 0) {
                    int desde = indiceOpcionArrastrada;
                    int hacia = isEmpty() ? opcionesTemporales.size()
                            : (e.getY() > getHeight() / 2 ? getIndex() + 1 : getIndex());
                    Platform.runLater(() -> moverOpcion(desde, hacia));
                    movida = true;
                }
                getStyleClass().removeAll("destino-arriba", "destino-abajo");
                e.setDropCompleted(movida);
                e.consume();
            });
        }

        @Override
        protected void updateItem(String item, boolean vacia) {
            super.updateItem(item, vacia);
            setText(null);
            if (vacia || item == null) {
                setGraphic(null);
            } else {
                texto.setText(item);
                setGraphic(caja);
            }
        }
    }

    private Node crearControlParaPregunta(Pregunta p) {
        switch (p.getTipo()) {
            case TEXTO:
                TextField txtLibre = new TextField();
                txtLibre.setPromptText("Escribe tu respuesta aquí...");
                return txtLibre;

            case NUMERICA:
                TextField txtNumero = new TextField();
                txtNumero.setPromptText("Ingresa solo números (ej. 25 o 14.5)...");
                txtNumero.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal.matches("\\d*(\\.\\d*)?")) {
                        txtNumero.setText(oldVal);
                    }
                });
                return txtNumero;

            case SELECCION_UNICA:
                VBox boxRadios = new VBox(6);
                ToggleGroup grupoUnico = new ToggleGroup();
                for (String opcion : p.getOpciones()) {
                    RadioButton rb = new RadioButton(opcion);
                    rb.setToggleGroup(grupoUnico);
                    boxRadios.getChildren().add(rb);
                }
                boxRadios.setUserData(grupoUnico);
                return boxRadios;

            case SELECCION_MULTIPLE:
                VBox boxChecks = new VBox(6);
                for (String opcion : p.getOpciones()) {
                    CheckBox cb = new CheckBox(opcion);
                    boxChecks.getChildren().add(cb);
                }
                return boxChecks;

            default:
                return new Label("Tipo no soportado");
        }
    }

    @FXML
    private void handleValidarRespuestas() {
        if (encuestaActual.getPreguntas().isEmpty()) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Encuesta vacía", "Agrega al menos una pregunta para probar la validación.");
            return;
        }

        StringBuilder errores = new StringBuilder();
        int index = 1;

        for (Pregunta p : encuestaActual.getPreguntas()) {
            Node control = controlesRespuesta.get(p);
            String mensaje = null;

            if (p.isObligatoria() && !verificarSiTieneRespuesta(p, control)) {
                mensaje = "es obligatoria.";
            } else if (p.getTipo() == TipoPregunta.NUMERICA) {
                String valor = ((TextField) control).getText().trim();

                if (!valor.isEmpty() && !valor.matches("(?:\\d+(?:\\.\\d+)?|\\.\\d+)")) {
                    mensaje = "requiere un número válido, por ejemplo 25 o 14.5.";
                }
            }

            if (mensaje != null) {
                errores.append("- La pregunta #").append(index)
                        .append(" (\"").append(p.getEnunciado()).append("\") ")
                        .append(mensaje).append('\n');
            }

            index++;
        }

        if (errores.length() > 0) {
            mostrarAlerta(Alert.AlertType.ERROR, "Validación Fallida", "Se encontraron los siguientes problemas:\n\n" + errores);
        } else {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Validación Exitosa", "¡Todas las reglas de la encuesta se cumplieron correctamente!");
        }
    }

    private boolean verificarSiTieneRespuesta(Pregunta p, Node control) {
        switch (p.getTipo()) {
            case TEXTO:
            case NUMERICA:
                return !((TextField) control).getText().trim().isEmpty();

            case SELECCION_UNICA:
                ToggleGroup grupo = (ToggleGroup) control.getUserData();
                return grupo.getSelectedToggle() != null;

            case SELECCION_MULTIPLE:
                VBox boxChecks = (VBox) control;
                return boxChecks.getChildren().stream()
                        .filter(node -> node instanceof CheckBox)
                        .anyMatch(node -> ((CheckBox) node).isSelected());

            default:
                return true;
        }
    }
    @FXML
    private void handleLimpiarRespuestas() {
        controlesRespuesta.clear();
        renderizarVistaPrevia();
    }

    // ==================== BANCO DE PREGUNTAS (REUTILIZAR) ====================

    @FXML
    private void handleUsarPreguntaGuardada() {
        if (!Sesion.activa()) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("banco-preguntas-view.fxml"));
            Parent raiz = loader.load();
            BancoPreguntasController banco = loader.getController();
            banco.cargar(encuestaActual);

            Stage dialogo = new Stage();
            dialogo.initOwner(ventana());
            dialogo.initModality(Modality.WINDOW_MODAL);
            dialogo.setTitle("Banco de preguntas - Reutilizar");
            Scene escena = new Scene(raiz);
            Navegacion.aplicarEstilos(escena);
            dialogo.setScene(escena);
            Animaciones.fundidoEntrada(raiz, 250);
            dialogo.showAndWait();

            List<Pregunta> elegidas = banco.getSeleccionadas();
            if (elegidas.isEmpty()) {
                return;
            }
            for (Pregunta p : elegidas) {
                encuestaActual.agregarPregunta(p);
                porAnimar.add(p);
            }
            marcarCambios();
            renderizarVistaPrevia();
            Animaciones.resaltar(lblContador);
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir el banco de preguntas:\n" + e.getMessage());
        }
    }

    // ==================== GUARDAR / ABRIR / NUEVA ENCUESTA ====================

    @FXML
    private void handleGuardarEncuesta() {
        if (guardarEncuesta()) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Encuesta guardada",
                    "\"" + encuestaActual.getTitulo().trim() + "\" se guardó correctamente.\n"
                            + "Sus preguntas quedaron en tu banco para reutilizarlas en otras encuestas.");
        }
    }

    /** @return true si la encuesta quedó guardada en la base de datos. */
    private boolean guardarEncuesta() {
        if (!Sesion.activa()) {
            return false;
        }
        if (encuestaActual.getPreguntas().isEmpty()) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Encuesta vacía", "Agrega al menos una pregunta antes de guardar.");
            return false;
        }
        if (encuestaActual.getTitulo() == null || encuestaActual.getTitulo().isBlank()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Falta el título", "Escribe un título para la encuesta antes de guardarla.");
            txtTituloEncuesta.requestFocus();
            return false;
        }

        String tituloOriginal = encuestaActual.getTitulo();
        encuestaActual.setTitulo(tituloOriginal.trim());
        try {
            encuestaDAO.guardar(encuestaActual, Sesion.getUsuario().getId());
            cambiosSinGuardar = false;
            actualizarEstado();
            Animaciones.resaltar(lblEstado);
            return true;
        } catch (SQLException e) {
            encuestaActual.setTitulo(tituloOriginal);
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo guardar", Conexion.mensajeAmigable(e));
            return false;
        }
    }

    @FXML
    private void handleNuevaEncuesta() {
        if (!confirmarDescartarCambios()) {
            return;
        }
        mostrarEncuesta(new Encuesta("Nueva Encuesta"), false);
        limpiarFormulario();
        txtTituloEncuesta.requestFocus();
        txtTituloEncuesta.selectAll();
    }

    @FXML
    private void handleAbrirEncuesta() {
        if (!Sesion.activa() || !confirmarDescartarCambios()) {
            return;
        }

        List<EncuestaResumen> guardadas;
        try {
            guardadas = encuestaDAO.listar(Sesion.getUsuario().getId());
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Base de datos", Conexion.mensajeAmigable(e));
            return;
        }
        if (guardadas.isEmpty()) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Sin encuestas", "Todavía no has guardado ninguna encuesta.");
            return;
        }

        ListView<EncuestaResumen> lista = new ListView<>(FXCollections.observableArrayList(guardadas));
        lista.setPrefSize(560, 320);
        lista.getSelectionModel().selectFirst();

        ButtonType abrir = new ButtonType("Abrir", ButtonBar.ButtonData.OK_DONE);
        Dialog<EncuestaResumen> dialogo = new Dialog<>();
        dialogo.initOwner(ventana());
        dialogo.setTitle("Abrir encuesta guardada");
        dialogo.setHeaderText("Elige una de tus encuestas guardadas");
        dialogo.getDialogPane().getButtonTypes().addAll(abrir, ButtonType.CANCEL);
        dialogo.getDialogPane().setContent(lista);
        dialogo.setResultConverter(boton -> boton == abrir ? lista.getSelectionModel().getSelectedItem() : null);
        lista.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && lista.getSelectionModel().getSelectedItem() != null) {
                ((Button) dialogo.getDialogPane().lookupButton(abrir)).fire();
            }
        });

        Optional<EncuestaResumen> elegida = dialogo.showAndWait();
        if (elegida.isEmpty() || elegida.get() == null) {
            return;
        }

        try {
            Encuesta cargada = encuestaDAO.cargar(elegida.get().id(), Sesion.getUsuario().getId());
            mostrarEncuesta(cargada, true);
            limpiarFormulario();
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Base de datos", Conexion.mensajeAmigable(e));
        }
    }

    /** Reemplaza la encuesta que se está editando por otra (nueva o abierta). */
    private void mostrarEncuesta(Encuesta encuesta, boolean yaGuardada) {
        if (preguntaEnEdicion != null) {
            salirDeEdicion();
        }
        encuestaActual = encuesta;
        controlesRespuesta.clear();
        porAnimar.clear();
        porAnimar.addAll(encuesta.getPreguntas());

        cambiandoTituloPorCodigo = true;
        txtTituloEncuesta.setText(encuesta.getTitulo());
        cambiandoTituloPorCodigo = false;
        lblPreviewTitulo.setText(encuesta.getTitulo().isBlank() ? "Sin Título" : encuesta.getTitulo());

        cambiosSinGuardar = false;
        actualizarEstado();
        renderizarVistaPrevia();
    }

    /**
     * Si hay cambios sin guardar pregunta qué hacer.
     * @return true si se puede continuar (se guardó o se decidió descartar).
     */
    private boolean confirmarDescartarCambios() {
        if (!cambiosSinGuardar || encuestaActual.getPreguntas().isEmpty()) {
            return true;
        }
        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.YES);
        ButtonType noGuardar = new ButtonType("No guardar", ButtonBar.ButtonData.NO);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "La encuesta \"" + encuestaActual.getTitulo() + "\" tiene cambios sin guardar.\n¿Deseas guardarlos?",
                guardar, noGuardar, cancelar);
        alerta.setTitle("Cambios sin guardar");
        alerta.setHeaderText(null);
        alerta.initOwner(ventana());

        ButtonType respuesta = alerta.showAndWait().orElse(cancelar);
        if (respuesta == guardar) {
            return guardarEncuesta();
        }
        return respuesta == noGuardar;
    }

    private void marcarCambios() {
        cambiosSinGuardar = true;
        actualizarEstado();
    }

    private void actualizarEstado() {
        lblEstado.getStyleClass().removeAll("estado-guardado", "estado-sin-guardar");
        if (cambiosSinGuardar || encuestaActual.getId() == null) {
            lblEstado.setText("● Sin guardar");
            lblEstado.getStyleClass().add("estado-sin-guardar");
        } else {
            lblEstado.setText("✓ Guardada");
            lblEstado.getStyleClass().add("estado-guardado");
        }
    }

    // ==================== CUENTA Y AYUDA ====================

    @FXML
    private void handleCerrarSesion() {
        if (!confirmarDescartarCambios()) {
            return;
        }
        Stage ventana = (Stage) ventana();
        ventana.setOnCloseRequest(null);
        Sesion.cerrar();
        try {
            Navegacion.cambiarPantalla(ventana, "login-view.fxml", "ezEncuestaFX - Iniciar sesión",
                    HelloApplication.ANCHO_AUTH, HelloApplication.ALTO_AUTH);
        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo volver al inicio de sesión:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleSalir() {
        if (confirmarDescartarCambios()) {
            Platform.exit();
        }
    }

    @FXML
    private void handleAcercaDe() {
        mostrarAlerta(Alert.AlertType.INFORMATION, "Acerca de ezEncuestaFX",
                "ezEncuestaFX - Constructor de encuestas\n\n"
                        + "Crea encuestas, prueba su validación, expórtalas a Word y guárdalas en PostgreSQL.\n"
                        + "Cada pregunta que creas queda en tu banco de preguntas para reutilizarla después "
                        + "(menú Preguntas > Usar una pregunta que ya tenía, o Ctrl+B).");
    }

    private Window ventana() {
        return txtTituloEncuesta.getScene().getWindow();
    }

    // ==================== EXPORTAR ENCUESTA A UN ARCHIVO WORD DOCX ====================

    @FXML
    private void handleExportarWord() {
        if (encuestaActual.getPreguntas().isEmpty()) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Encuesta vacía", "Agrega al menos una pregunta para exportar.");
            return;
        }

        // Generar nombre dinámico limpio basado en el título de la encuesta
        String tituloRaw = encuestaActual.getTitulo();
        String nombreBase = (tituloRaw == null || tituloRaw.isBlank())
                ? "encuesta"
                : tituloRaw.trim().replaceAll("[\\\\/:*?\"<>|]", "_");

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar encuesta como Word");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento Word (*.docx)", "*.docx"));
        fc.setInitialFileName(nombreBase + ".docx");

        File archivo = fc.showSaveDialog(txtTituloEncuesta.getScene().getWindow());
        if (archivo != null) {
            try {
                exportarWord(archivo);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Exportación exitosa", "Documento guardado en:\n" + archivo.getAbsolutePath());
            } catch (IOException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo guardar el archivo:\n" + e.getMessage());
            }
        }
    }

    private void exportarWord(File destino) throws IOException {
        try (FileOutputStream out = new FileOutputStream(destino);
             XWPFDocument doc = new XWPFDocument()) {

            // 1. Título Principal
            String titulo = (encuestaActual.getTitulo() == null || encuestaActual.getTitulo().isBlank())
                    ? "Encuesta Sin Título" : encuestaActual.getTitulo().trim();

            XWPFParagraph pTitulo = doc.createParagraph();
            pTitulo.setAlignment(ParagraphAlignment.CENTER);
            pTitulo.setSpacingAfter(100);
            XWPFRun rTitulo = pTitulo.createRun();
            rTitulo.setFontFamily("Calibri");
            rTitulo.setBold(true);
            rTitulo.setFontSize(20);
            rTitulo.setColor("1E293B");
            rTitulo.setText(titulo);

            // 2. Subtítulo con Fecha e Instrucciones
            String fechaActual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            XWPFParagraph pSubtitulo = doc.createParagraph();
            pSubtitulo.setAlignment(ParagraphAlignment.CENTER);
            pSubtitulo.setBorderBottom(Borders.SINGLE);
            pSubtitulo.setSpacingAfter(300);
            XWPFRun rSubtitulo = pSubtitulo.createRun();
            rSubtitulo.setFontFamily("Calibri");
            rSubtitulo.setFontSize(10);
            rSubtitulo.setColor("64748B");
            rSubtitulo.setText("Fecha: " + fechaActual + "   |   Complete cada apartado según se indica. Los campos con (*) son obligatorios.");

            // 3. Preguntas
            int numero = 1;
            for (Pregunta p : encuestaActual.getPreguntas()) {
                XWPFParagraph pEnunciado = doc.createParagraph();
                pEnunciado.setSpacingBefore(240);
                pEnunciado.setSpacingAfter(80);
                pEnunciado.setKeepNext(true); // Evita que la pregunta quede separada de sus opciones al cambiar de página

                XWPFRun rEnunciado = pEnunciado.createRun();
                rEnunciado.setFontFamily("Calibri");
                rEnunciado.setBold(true);
                rEnunciado.setFontSize(12);
                rEnunciado.setColor("0F172A");
                rEnunciado.setText(numero + ". " + p.getEnunciado());

                XWPFRun rEstado = pEnunciado.createRun();
                rEstado.setFontFamily("Calibri");
                rEstado.setFontSize(11);
                if (p.isObligatoria()) {
                    rEstado.setBold(true);
                    rEstado.setColor("DC2626"); // Rojo para obligatoria
                    rEstado.setText(" *");
                } else {
                    rEstado.setItalic(true);
                    rEstado.setColor("64748B"); // Gris para opcional
                    rEstado.setText(" (Opcional)");
                }

                // Renderizar espacio de respuesta según tipo
                switch (p.getTipo()) {
                    case TEXTO:
                        crearCajaRespuesta(doc, "100%", " ", 2);
                        break;

                    case NUMERICA:
                        crearCajaRespuesta(doc, "35%", "Valor numérico:", 1);
                        break;

                    case SELECCION_UNICA:
                        for (String opcion : p.getOpciones()) {
                            agregarOpcionWord(doc, "○   " + opcion);
                        }
                        break;

                    case SELECCION_MULTIPLE:
                        for (String opcion : p.getOpciones()) {
                            agregarOpcionWord(doc, "☐   " + opcion);
                        }
                        break;
                }
                numero++;
            }

            // 4. Pie de documento
            XWPFParagraph pNota = doc.createParagraph();
            pNota.setSpacingBefore(400);
            XWPFRun rNota = pNota.createRun();
            rNota.setFontFamily("Calibri");
            rNota.setItalic(true);
            rNota.setFontSize(9);
            rNota.setColor("64748B");
            rNota.setText("* Pregunta obligatoria");

            doc.write(out);
        }
    }

    private void crearCajaRespuesta(XWPFDocument doc, String anchoPorcentaje, String textoGuia, int lineasAltura) {
        XWPFTable tabla = doc.createTable(1, 1);
        tabla.setWidth(anchoPorcentaje);
        XWPFTableCell celda = tabla.getRow(0).getCell(0);
        celda.setColor("F8FAFC");

        XWPFParagraph pCelda = celda.getParagraphs().get(0);
        pCelda.setSpacingBefore(60);
        pCelda.setSpacingAfter(60 * lineasAltura);

        XWPFRun rGuia = pCelda.createRun();
        rGuia.setFontFamily("Calibri");
        rGuia.setFontSize(9);
        rGuia.setItalic(true);
        rGuia.setColor("94A3B8");
        rGuia.setText(textoGuia);
    }

    private void agregarOpcionWord(XWPFDocument doc, String texto) {
        XWPFParagraph pOpcion = doc.createParagraph();
        pOpcion.setIndentationLeft(360);
        pOpcion.setSpacingAfter(50);
        XWPFRun rOpcion = pOpcion.createRun();
        rOpcion.setFontFamily("Calibri");
        rOpcion.setFontSize(11);
        rOpcion.setColor("1E293B");
        rOpcion.setText(texto);
    }

    // =========================================================

    private void limpiarFormulario() {
        txtEnunciado.clear();
        chkObligatoria.setSelected(false);
        opcionesTemporales.clear();
        txtNuevaOpcion.clear();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.getDialogPane().setPrefWidth(600); // Ancho inicial de la alerta
        alert.showAndWait();
    }
}