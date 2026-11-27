package com.tuempresa.ezencuestafx.db;

import com.tuempresa.ezencuestafx.model.Pregunta;
import com.tuempresa.ezencuestafx.model.PreguntaGuardada;
import com.tuempresa.ezencuestafx.model.TipoPregunta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Acceso al banco de preguntas de cada usuario. */
public class PreguntaDAO {

    /** Código de PostgreSQL cuando se viola una llave foránea. */
    public static final String SQLSTATE_EN_USO = "23503";

    /** Todas las preguntas que el usuario ha creado antes, con cuántas veces se han usado. */
    public List<PreguntaGuardada> listarBanco(int usuarioId) throws SQLException {
        String sql = "SELECT p.id, p.enunciado, p.tipo, p.obligatoria, p.creada_en, "
                + "       COUNT(DISTINCT ep.encuesta_id) AS veces_usada, "
                + "       COALESCE(STRING_AGG(DISTINCT e.titulo, ', '), '') AS usada_en "
                + "FROM preguntas p "
                + "LEFT JOIN encuesta_preguntas ep ON ep.pregunta_id = p.id "
                + "LEFT JOIN encuestas e ON e.id = ep.encuesta_id "
                + "WHERE p.usuario_id = ? "
                + "GROUP BY p.id "
                + "ORDER BY p.creada_en DESC, p.id DESC";

        List<PreguntaGuardada> resultado = new ArrayList<>();
        try (Connection con = Conexion.obtener()) {
            Map<Integer, List<String>> opciones = opcionesDelUsuario(con, usuarioId);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, usuarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        Pregunta p = new Pregunta(
                                id,
                                rs.getString("enunciado"),
                                TipoPregunta.valueOf(rs.getString("tipo")),
                                rs.getBoolean("obligatoria"),
                                opciones.getOrDefault(id, List.of()));
                        resultado.add(new PreguntaGuardada(
                                p,
                                rs.getInt("veces_usada"),
                                rs.getString("usada_en"),
                                rs.getTimestamp("creada_en").toLocalDateTime()));
                    }
                }
            }
        }
        return resultado;
    }

    /** Guarda la pregunta en el banco del usuario (si no existía ya) y le asigna su id. */
    public void guardarEnBanco(int usuarioId, Pregunta pregunta) throws SQLException {
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                obtenerOCrear(con, usuarioId, pregunta);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                pregunta.setId(null);
                throw e;
            }
        }
    }

    /**
     * Devuelve el id de la pregunta en el banco. Si el usuario ya tenía una pregunta
     * igual (mismo enunciado, tipo y opciones) se reutiliza esa, para no duplicarla.
     */
    int obtenerOCrear(Connection con, int usuarioId, Pregunta p) throws SQLException {
        if (p.getId() != null) {
            return p.getId();
        }

        String buscar = "SELECT id FROM preguntas "
                + "WHERE usuario_id = ? AND LOWER(TRIM(enunciado)) = LOWER(TRIM(?)) AND tipo = ?";
        try (PreparedStatement ps = con.prepareStatement(buscar)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, p.getEnunciado());
            ps.setString(3, p.getTipo().name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int candidato = rs.getInt("id");
                    if (opcionesDe(con, candidato).equals(p.getOpciones())) {
                        p.setId(candidato);
                        return candidato;
                    }
                }
            }
        }

        String insertar = "INSERT INTO preguntas (usuario_id, enunciado, tipo, obligatoria) "
                + "VALUES (?, ?, ?, ?) RETURNING id";
        int nuevoId;
        try (PreparedStatement ps = con.prepareStatement(insertar)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, p.getEnunciado().trim());
            ps.setString(3, p.getTipo().name());
            ps.setBoolean(4, p.isObligatoria());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                nuevoId = rs.getInt(1);
            }
        }

        String insertarOpcion = "INSERT INTO opciones_pregunta (pregunta_id, texto, orden) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(insertarOpcion)) {
            int orden = 1;
            for (String opcion : p.getOpciones()) {
                ps.setInt(1, nuevoId);
                ps.setString(2, opcion);
                ps.setInt(3, orden++);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        p.setId(nuevoId);
        return nuevoId;
    }

    /**
     * Elimina una pregunta del banco. Si ya se usó en alguna encuesta guardada,
     * PostgreSQL lo impide y se lanza una SQLException con estado {@link #SQLSTATE_EN_USO}.
     */
    public void eliminar(int usuarioId, int preguntaId) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("DELETE FROM preguntas WHERE id = ? AND usuario_id = ?")) {
            ps.setInt(1, preguntaId);
            ps.setInt(2, usuarioId);
            ps.executeUpdate();
        }
    }

    static List<String> opcionesDe(Connection con, int preguntaId) throws SQLException {
        List<String> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT texto FROM opciones_pregunta WHERE pregunta_id = ? ORDER BY orden")) {
            ps.setInt(1, preguntaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(rs.getString("texto"));
                }
            }
        }
        return lista;
    }

    private Map<Integer, List<String>> opcionesDelUsuario(Connection con, int usuarioId) throws SQLException {
        String sql = "SELECT op.pregunta_id, op.texto FROM opciones_pregunta op "
                + "JOIN preguntas p ON p.id = op.pregunta_id "
                + "WHERE p.usuario_id = ? ORDER BY op.pregunta_id, op.orden";
        Map<Integer, List<String>> mapa = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mapa.computeIfAbsent(rs.getInt("pregunta_id"), k -> new ArrayList<>()).add(rs.getString("texto"));
                }
            }
        }
        return mapa;
    }
}
