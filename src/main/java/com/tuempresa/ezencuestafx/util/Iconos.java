package com.tuempresa.ezencuestafx.util;

import javafx.scene.layout.Region;

/**
 * Íconos vectoriales (lápiz, papelera y agarre para arrastrar).
 * Se dibujan con -fx-shape, así se ven nítidos en cualquier tamaño y su color
 * se controla desde estilos.css (clases .icono, .icono-papelera, etc.).
 * Figuras basadas en Material Icons (licencia Apache 2.0).
 */
public final class Iconos {

    private static final String PAPELERA =
            "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";

    private static final String LAPIZ =
            "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41"
                    + "l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";

    /** Seis puntos (⠿) que indican "puedes arrastrar esto". */
    private static final String AGARRE =
            "M11 18c0 1.1-.9 2-2 2s-2-.9-2-2 .9-2 2-2 2 .9 2 2zm-2-8c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"
                    + "m0-6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm6 4c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2z"
                    + "m0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z";

    private Iconos() { }

    public static Region papelera() {
        return crear(PAPELERA, 12, 14, "icono-papelera");
    }

    public static Region lapiz() {
        return crear(LAPIZ, 13, 13, "icono-lapiz");
    }

    public static Region agarre() {
        return crear(AGARRE, 8, 13, "icono-agarre");
    }

    private static Region crear(String figura, double ancho, double alto, String clase) {
        Region icono = new Region();
        icono.setStyle("-fx-shape: \"" + figura + "\";");
        icono.getStyleClass().addAll("icono", clase);
        icono.setMinSize(ancho, alto);
        icono.setPrefSize(ancho, alto);
        icono.setMaxSize(ancho, alto);
        icono.setMouseTransparent(true);
        return icono;
    }
}
