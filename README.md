# ezEncuestaFX

Aplicación de escritorio desarrollada en **JavaFX** para la creación, validación en tiempo real y exportación de encuestas personalizadas a formato **Microsoft Word (`.docx`)**.

---

## Características Principales

* **Constructor Dinámico de Preguntas:** Permite definir el título de la encuesta y agregar múltiples preguntas con configuración individual.
* **Tipos de Respuesta Soportados:**
  * **Texto (Abierta):** Campo de entrada libre para respuestas alfanuméricas.
  * **Numérica:** Filtro activo en tiempo real que bloquea letras o caracteres especiales, permitiendo únicamente números enteros y decimales.
  * **Selección Única:** Grupo exclusivo de opciones (`RadioButton`) donde solo se puede marcar una alternativa.
  * **Selección Múltiple:** Lista independiente de casillas (`CheckBox`) para marcar varias opciones simultáneamente.
* **Reglas de Obligatoriedad:** Cada pregunta puede marcarse como **obligatoria** (`*`) o **opcional**.
* **Vista Previa Interactiva y Simulador de Validación:** Renderiza la encuesta al instante en el panel derecho y permite ejecutar pruebas de validación para verificar que ninguna pregunta obligatoria quede sin responder.
* **Exportación a Word (`.docx`):** Genera automáticamente un documento con formato limpio, fecha de creación, indicadores visuales de obligatoriedad y espacios estructurados para cada tipo de respuesta, nombrando el archivo según el título de la encuesta.

---

## Tecnologías Utilizadas

| Tecnología | Versión | Propósito |
| :--- | :--- | :--- |
| **Java (JDK)** | 21 | Lenguaje principal y sistema de módulos (`JPMS`) |
| **JavaFX** | 21.0.6 | Interfaz gráfica de usuario (`Controls` y `FXML`) |
| **Scene Builder** | 21+ | Diseño visual de la vista (`encuesta-view.fxml`) |
| **Apache POI (`poi-ooxml`)** | 5.3.0 | Generación y estilizado de documentos `.docx` |
| **Apache Maven** | 3.9+ | Gestión de dependencias y construcción del proyecto |

---

## Estructura del Proyecto

```text
ezEncuestaFX/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   ├── module-info.java
        │   └── com/tuempresa/ezencuestafx/
        │       ├── Launcher.java                  # Punto de entrada de la aplicación
        │       ├── HelloApplication.java          # Carga la ventana principal y el FXML
        │       ├── controller/
        │       │   └── EncuestaController.java    # Lógica de UI, validaciones y exportación Word
        │       └── model/
        │           ├── Encuesta.java              # Contenedor del título y lista de preguntas
        │           ├── Pregunta.java              # Entidad con enunciado, tipo, reglas y opciones
        │           └── TipoPregunta.java          # Enumeración de tipos de respuesta
        └── resources/
            └── com/tuempresa/ezencuestafx/
                └── encuesta-view.fxml             # Diseño de interfaz para Scene Builder
