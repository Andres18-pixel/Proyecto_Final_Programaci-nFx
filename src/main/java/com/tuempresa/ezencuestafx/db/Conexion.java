package com.tuempresa.ezencuestafx.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Abre conexiones a PostgreSQL usando los datos de db.properties.
 * Cada DAO pide una conexión nueva y la cierra con try-with-resources.
 */
public final class Conexion {

    private static final String ARCHIVO = "/com/tuempresa/ezencuestafx/db.properties";
    private static Properties config;

    private Conexion() { }

    public static Connection obtener() throws SQLException {
        Properties p = cargarConfig();
        return DriverManager.getConnection(
                p.getProperty("db.url"),
                p.getProperty("db.usuario"),
                p.getProperty("db.contrasena"));
    }

    private static synchronized Properties cargarConfig() throws SQLException {
        if (config == null) {
            Properties p = new Properties();
            try (InputStream in = Conexion.class.getResourceAsStream(ARCHIVO)) {
                if (in == null) {
                    throw new SQLException("No se encontró el archivo de configuración " + ARCHIVO);
                }
                p.load(in);
            } catch (IOException e) {
                throw new SQLException("No se pudo leer " + ARCHIVO + ": " + e.getMessage(), e);
            }
            config = p;
        }
        return config;
    }

    /** Mensaje entendible para mostrar al usuario cuando falla la base de datos. */
    public static String mensajeAmigable(SQLException e) {
        String estado = e.getSQLState();
        if (estado != null && estado.startsWith("08")) {
            return "No se pudo conectar con PostgreSQL.\n\n"
                    + "Verifica que el servidor esté encendido, que exista la base \"ezencuesta\" "
                    + "y que el usuario y la contraseña de db.properties sean correctos.";
        }
        if ("28P01".equals(estado)) {
            return "PostgreSQL rechazó el usuario o la contraseña configurados en db.properties.";
        }
        if ("3D000".equals(estado)) {
            return "La base de datos \"ezencuesta\" no existe. Créala y ejecuta database/ezencuesta.sql.";
        }
        if ("42P01".equals(estado)) {
            return "Faltan tablas en la base de datos. Ejecuta el script database/ezencuesta.sql.";
        }
        return "Error de base de datos: " + e.getMessage();
    }
}
