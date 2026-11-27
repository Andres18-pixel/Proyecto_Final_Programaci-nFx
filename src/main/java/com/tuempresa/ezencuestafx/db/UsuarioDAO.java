package com.tuempresa.ezencuestafx.db;

import com.tuempresa.ezencuestafx.model.Rol;
import com.tuempresa.ezencuestafx.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDAO {

    /**
     * Busca al usuario por nombre de usuario o correo y comprueba la contraseña.
     * @return el usuario si las credenciales son correctas, o null si no lo son.
     */
    public Usuario autenticar(String usuarioOCorreo, String contrasena) throws SQLException {
        String sql = "SELECT id, nombre_completo, correo, usuario, contrasena_hash, rol "
                + "FROM usuarios WHERE LOWER(usuario) = LOWER(?) OR LOWER(correo) = LOWER(?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, usuarioOCorreo);
            ps.setString(2, usuarioOCorreo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && Seguridad.verificar(contrasena, rs.getString("contrasena_hash"))) {
                    return new Usuario(
                            rs.getInt("id"),
                            rs.getString("nombre_completo"),
                            rs.getString("correo"),
                            rs.getString("usuario"),
                            Rol.valueOf(rs.getString("rol")));
                }
            }
        }
        return null;
    }

    public boolean existeUsuario(String usuario) throws SQLException {
        return existe("SELECT 1 FROM usuarios WHERE LOWER(usuario) = LOWER(?)", usuario);
    }

    public boolean existeCorreo(String correo) throws SQLException {
        return existe("SELECT 1 FROM usuarios WHERE LOWER(correo) = LOWER(?)", correo);
    }

    public void registrar(String nombreCompleto, String correo, String usuario, String contrasena, Rol rol)
            throws SQLException {
        String sql = "INSERT INTO usuarios (nombre_completo, correo, usuario, contrasena_hash, rol) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreCompleto);
            ps.setString(2, correo);
            ps.setString(3, usuario);
            ps.setString(4, Seguridad.generarHash(contrasena));
            ps.setString(5, rol.name());
            ps.executeUpdate();
        }
    }

    private boolean existe(String sql, String valor) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
