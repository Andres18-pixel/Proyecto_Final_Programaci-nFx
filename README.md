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
* **Inicio de sesión y registro:** Cada persona crea su cuenta como **Docente** o **Estudiante de investigación**. Las contraseñas se guardan cifradas (PBKDF2), nunca en texto plano.
* **Base de datos PostgreSQL:** Las encuestas, sus preguntas y los usuarios se guardan en PostgreSQL.
* **Banco de preguntas (reutilizar preguntas):** Toda pregunta que creas queda guardada en tu banco. Desde *Preguntas > Usar una pregunta que ya tenía* (o `Ctrl+B`) puedes buscarla, filtrarla por tipo, ver en qué encuestas la usaste y agregarla de nuevo a otra encuesta.
* **Menús:** *Archivo* (nueva, abrir, guardar, exportar, salir), *Preguntas* (banco, validar, limpiar), *Cuenta* (cerrar sesión) y *Ayuda*, con atajos de teclado.
* **Animaciones:** Fondo animado en el login, transiciones entre pantallas, tarjetas que aparecen y desaparecen suavemente y sacudida cuando hay un error.

---

## Tecnologías Utilizadas

| Tecnología | Versión | Propósito |
| :--- | :--- | :--- |
| **Java (JDK)** | 21 | Lenguaje principal y sistema de módulos (`JPMS`) |
| **JavaFX** | 21.0.6 | Interfaz gráfica de usuario (`Controls` y `FXML`) |
| **Scene Builder** | 21+ | Diseño visual de la vista (`encuesta-view.fxml`) |
| **Apache POI (`poi-ooxml`)** | 5.3.0 | Generación y estilizado de documentos `.docx` |
| **PostgreSQL** + driver JDBC | 42.7.4 | Usuarios, encuestas y banco de preguntas |
| **Apache Maven** | 3.9+ | Gestión de dependencias y construcción del proyecto |

---

## Estructura del Proyecto

```text
ezEncuestaFX/
├── pom.xml
├── database/ezencuesta.sql
└── src/
    └── main/
        ├── java/
        │   ├── module-info.java
        │   └── com/tuempresa/ezencuestafx/
        │       ├── Launcher.java                  # Punto de entrada de la aplicación
        │       ├── HelloApplication.java          # Abre la ventana empezando por el login
        │       ├── controller/
        │       │   ├── LoginController.java       # Inicio de sesión
        │       │   ├── SignupController.java      # Registro (Docente / Estudiante de investigación)
        │       │   ├── EncuestaController.java    # Constructor, menús, guardar/abrir y exportación Word
        │       │   └── BancoPreguntasController.java # Reutilizar preguntas guardadas
        │       ├── db/                            # Conexión a PostgreSQL y DAOs
        │       ├── util/                          # Sesión, navegación y animaciones
        │       └── model/
        │           ├── Encuesta.java              # Contenedor del título y lista de preguntas
        │           ├── Pregunta.java              # Entidad con enunciado, tipo, reglas y opciones
        │           └── TipoPregunta.java          # Enumeración de tipos de respuesta
        └── resources/
            └── com/tuempresa/ezencuestafx/
                ├── login-view.fxml / signup-view.fxml
                ├── encuesta-view.fxml             # Constructor de encuestas con menús
                ├── banco-preguntas-view.fxml      # Ventana del banco de preguntas
                ├── estilos.css
                └── db.properties                  # Datos de conexión a PostgreSQL

---

## Configurar la base de datos

1. Crear la base de datos en PostgreSQL (pgAdmin o `psql`):
   ```sql
   CREATE DATABASE ezencuesta;
   ```
2. Conectarse a `ezencuesta` y ejecutar el script [`database/ezencuesta.sql`](database/ezencuesta.sql).
3. Poner el usuario y la contraseña de **tu** PostgreSQL en
   `src/main/resources/com/tuempresa/ezencuestafx/db.properties`:
   ```properties
   db.url=jdbc:postgresql://localhost:5432/ezencuesta
   db.usuario=postgres
   db.contrasena=tu_contraseña
   ```
4. Recargar Maven (para descargar el driver de PostgreSQL) y ejecutar la aplicación. La primera pantalla es el inicio de sesión: crea una cuenta con *Regístrate aquí*.

### Tablas

| Tabla | Para qué sirve |
| :--- | :--- |
| `usuarios` | Cuentas (nombre, correo, usuario, contraseña cifrada, rol) |
| `preguntas` | Banco de preguntas de cada usuario |
| `opciones_pregunta` | Opciones de las preguntas de selección |
| `encuestas` | Encuestas guardadas por cada usuario |
| `encuesta_preguntas` | Qué preguntas usa cada encuesta y en qué orden (una pregunta puede estar en muchas encuestas) |
