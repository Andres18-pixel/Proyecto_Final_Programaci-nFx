package com.tuempresa.ezencuestafx.controller;

import com.tuempresa.ezencuestafx.model.Encuesta;
import com.tuempresa.ezencuestafx.model.Pregunta;
import com.tuempresa.ezencuestafx.model.TipoPregunta;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    private final Encuesta encuestaActual = new Encuesta("Nueva Encuesta");
    private final ObservableList<String> opcionesTemporales = FXCollections.observableArrayList();
    private final Map<Pregunta, Node> controlesRespuesta = new HashMap<>();

    @FXML
    public void initialize() {
        cmbTipoPregunta.setItems(FXCollections.observableArrayList(TipoPregunta.values()));
        cmbTipoPregunta.getSelectionModel().select(TipoPregunta.TEXTO);
        lstOpciones.setItems(opcionesTemporales);

        txtTituloEncuesta.textProperty().addListener((obs, oldVal, newVal) -> {
            encuestaActual.setTitulo(newVal);
            lblPreviewTitulo.setText(newVal.isBlank() ? "Sin Título" : newVal);
        });

        cmbTipoPregunta.valueProperty().addListener((obs, oldTipo, nuevoTipo) -> {
            boolean requiereOpciones = (nuevoTipo == TipoPregunta.SELECCION_UNICA || nuevoTipo == TipoPregunta.SELECCION_MULTIPLE);
            boxOpcionesConfig.setVisible(requiereOpciones);
            boxOpcionesConfig.setManaged(requiereOpciones);
        });
    }

    @FXML
    private void handleAgregarOpcion() {
        String opcion = txtNuevaOpcion.getText().trim();
        if (!opcion.isEmpty() && !opcionesTemporales.contains(opcion)) {
            opcionesTemporales.add(opcion);
            txtNuevaOpcion.clear();
            txtNuevaOpcion.requestFocus();
        }
    }

    @FXML
    private void handleEliminarOpcion() {
        String seleccionada = lstOpciones.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            opcionesTemporales.remove(seleccionada);
        }
    }

    @FXML
    private void handleAgregarPregunta() {
        String enunciado = txtEnunciado.getText().trim();
        TipoPregunta tipo = cmbTipoPregunta.getValue();
        boolean obligatoria = chkObligatoria.isSelected();

        if (enunciado.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo requerido", "Debes escribir el enunciado de la pregunta.");
            return;
        }

        boolean requiereOpciones = (tipo == TipoPregunta.SELECCION_UNICA || tipo == TipoPregunta.SELECCION_MULTIPLE);
        if (requiereOpciones && opcionesTemporales.size() < 2) {
            mostrarAlerta(Alert.AlertType.WARNING, "Faltan opciones", "Agrega al menos 2 opciones para preguntas de selección.");
            return;
        }

        Pregunta nueva = new Pregunta(enunciado, tipo, obligatoria, new ArrayList<>(opcionesTemporales));
        encuestaActual.agregarPregunta(nueva);

        limpiarFormulario();
        renderizarVistaPrevia();
    }

    private void renderizarVistaPrevia() {
        contenedorPreguntasPreview.getChildren().clear();
        controlesRespuesta.clear();

        int numero = 1;
        for (Pregunta p : encuestaActual.getPreguntas()) {
            VBox tarjeta = new VBox(8);
            tarjeta.setPadding(new Insets(12));
            tarjeta.setStyle("-fx-border-color: #CBD5E1; -fx-border-radius: 6; -fx-background-color: #F8FAFC; -fx-background-radius: 6;");

            HBox header = new HBox(10);
            String textoObligatoria = p.isObligatoria() ? " *" : " (Opcional)";
            Label lblPregunta = new Label(numero + ". " + p.getEnunciado() + textoObligatoria);
            lblPregunta.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + (p.isObligatoria() ? "#0F172A;" : "#475569;"));
            lblPregunta.setWrapText(true);
            HBox.setHgrow(lblPregunta, Priority.ALWAYS);

            Button btnBorrar = new Button("X");
            btnBorrar.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 10px;");
            btnBorrar.setOnAction(e -> {
                encuestaActual.eliminarPregunta(p);
                renderizarVistaPrevia();
            });

            header.getChildren().addAll(lblPregunta, btnBorrar);
            tarjeta.getChildren().add(header);

            Node controlEntrada = crearControlParaPregunta(p);
            controlesRespuesta.put(p, controlEntrada);
            tarjeta.getChildren().add(controlEntrada);

            contenedorPreguntasPreview.getChildren().add(tarjeta);
            numero++;
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
            if (p.isObligatoria()) {
                Node control = controlesRespuesta.get(p);
                if (!verificarSiTieneRespuesta(p, control)) {
                    errores.append("- La pregunta #").append(index).append(" (\"").append(p.getEnunciado()).append("\") es obligatoria.\n");
                }
            }
            index++;
        }

        if (errores.length() > 0) {
            mostrarAlerta(Alert.AlertType.ERROR, "Validación Fallida", "Faltan respuestas obligatorias:\n\n" + errores);
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

    // ==================== EXPORTAR A WORD ====================

    @FXML
    private void handleExportarWord() {
        if (encuestaActual.getPreguntas().isEmpty()) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Encuesta vacía", "Agrega al menos una pregunta para exportar.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar encuesta como Word");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento Word (*.docx)", "*.docx"));
        fc.setInitialFileName("encuesta.docx");

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
        try (XWPFDocument doc = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(destino)) {

            // Título
            String titulo = (encuestaActual.getTitulo() == null || encuestaActual.getTitulo().isBlank())
                    ? "Sin Título" : encuestaActual.getTitulo();
            XWPFParagraph pTitulo = doc.createParagraph();
            pTitulo.setAlignment(ParagraphAlignment.CENTER);
            pTitulo.setSpacingAfter(300);
            XWPFRun rTitulo = pTitulo.createRun();
            rTitulo.setBold(true);
            rTitulo.setFontSize(20);
            rTitulo.setText(titulo);

            // Preguntas
            int numero = 1;
            for (Pregunta p : encuestaActual.getPreguntas()) {
                XWPFParagraph pEnunciado = doc.createParagraph();
                pEnunciado.setSpacingBefore(200);
                XWPFRun rEnunciado = pEnunciado.createRun();
                rEnunciado.setBold(true);
                rEnunciado.setFontSize(12);
                rEnunciado.setText(numero + ". " + p.getEnunciado() + (p.isObligatoria() ? " *" : " (Opcional)"));

                switch (p.getTipo()) {
                    case TEXTO:
                        doc.createParagraph().createRun().setText("Respuesta: ______________________________________");
                        break;
                    case NUMERICA:
                        doc.createParagraph().createRun().setText("Respuesta (numérica): ______________");
                        break;
                    case SELECCION_UNICA:
                        for (String opcion : p.getOpciones()) {
                            doc.createParagraph().createRun().setText("(   )  " + opcion);
                        }
                        break;
                    case SELECCION_MULTIPLE:
                        for (String opcion : p.getOpciones()) {
                            doc.createParagraph().createRun().setText("[   ]  " + opcion);
                        }
                        break;
                    default:
                        break;
                }
                numero++;
            }

            // Nota final
            XWPFParagraph pNota = doc.createParagraph();
            pNota.setSpacingBefore(400);
            XWPFRun rNota = pNota.createRun();
            rNota.setItalic(true);
            rNota.setFontSize(9);
            rNota.setText("* Pregunta obligatoria");

            doc.write(out);
        }
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
        alert.showAndWait();
    }
}
