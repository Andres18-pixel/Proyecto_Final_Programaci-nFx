package com.tuempresa.ezencuestafx.db;

import com.tuempresa.ezencuestafx.model.Encuesta;
import com.tuempresa.ezencuestafx.model.EncuestaResumen;
import com.tuempresa.ezencuestafx.model.Pregunta;
import com.tuempresa.ezencuestafx.model.TipoPregunta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EncuestaDAO {

    private final PreguntaDAO preguntaDAO = new PreguntaDAO();

    /**
     * Guarda (o actualiza) la encuesta con todas sus preguntas en una sola transacción.
     * Las preguntas nuevas se agregan al banco del usuario para poder reutilizarlas después.
     */
    public void guardar(Encuesta encuesta, int usuarioId) throws SQLException {
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            // Preguntas que reciben id en esta transacción; si algo falla se les quita
            List<Pregunta> idsNuevos = new ArrayList<>();
            try {
                int encuestaId;
                if (encuesta.getId() == null) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO encuestas (usuario_id, titulo) VALUES (?, ?) RETURNING id")) {
                        ps.setInt(1, usuarioId);
                        ps.setString(2, encuesta.getTitulo());
                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            encuestaId = rs.getInt(1);
                        }
                    }
                } else {
                    encuestaId = encuesta.getId();
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE encuestas SET titulo = ?, actualizada_en = CURRENT_TIMESTAMP "
                                    + "WHERE id = ? AND usuario_id = ?")) {
                        ps.setString(1, encuesta.getTitulo());
                        ps.setInt(2, encuestaId);
                        ps.setInt(3, usuarioId);
                        if (ps.executeUpdate() == 0) {
                            throw new SQLException("La encuesta ya no existe o no pertenece a este usuario.");
                        }
                    }
                    try (PreparedStatement ps = con.prepareStatement(
                            "DELETE FROM encuesta_preguntas WHERE encuesta_id = ?")) {
                        ps.setInt(1, encuestaId);
                        ps.executeUpdate();
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO encuesta_preguntas (encuesta_id, pregunta_id, orden, obligatoria) "
                                + "VALUES (?, ?, ?, ?)")) {
                    int orden = 1;
                    for (Pregunta p : encuesta.getPreguntas()) {
                        if (p.getId() == null) {
                            idsNuevos.add(p);
                        }
                        int preguntaId = preguntaDAO.obtenerOCrear(con, usuarioId, p);
                        ps.setInt(1, encuestaId);
                        ps.setInt(2, preguntaId);
                        ps.setInt(3, orden++);
                        ps.setBoolean(4, p.isObligatoria());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                con.commit();
                encuesta.setId(encuestaId);
            } catch (SQLException e) {
                con.rollback();
                idsNuevos.forEach(p -> p.setId(null));
                throw e;
            }
        }
    }

    public List<EncuestaResumen> listar(int usuarioId) throws SQLException {
        String sql = "SELECT e.id, e.titulo, e.actualizada_en, COUNT(ep.pregunta_id) AS total "
                + "FROM encuestas e LEFT JOIN encuesta_preguntas ep ON ep.encuesta_id = e.id "
                + "WHERE e.usuario_id = ? GROUP BY e.id ORDER BY e.actualizada_en DESC";
        List<EncuestaResumen> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new EncuestaResumen(
                            rs.getInt("id"),
                            rs.getString("titulo"),
                            rs.getInt("total"),
                            rs.getTimestamp("actualizada_en").toLocalDateTime()));
                }
            }
        }
        return lista;
    }

    /** Carga una encuesta guardada con sus preguntas en el orden en que se guardaron. */
    public Encuesta cargar(int encuestaId, int usuarioId) throws SQLException {
        try (Connection con = Conexion.obtener()) {
            Encuesta encuesta;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT titulo FROM encuestas WHERE id = ? AND usuario_id = ?")) {
                ps.setInt(1, encuestaId);
                ps.setInt(2, usuarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("La encuesta no existe o no pertenece a este usuario.");
                    }
                    encuesta = new Encuesta(rs.getString("titulo"));
                    encuesta.setId(encuestaId);
                }
            }

            String sql = "SELECT p.id, p.enunciado, p.tipo, ep.obligatoria "
                    + "FROM encuesta_preguntas ep JOIN preguntas p ON p.id = ep.pregunta_id "
                    + "WHERE ep.encuesta_id = ? ORDER BY ep.orden";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, encuestaId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        encuesta.agregarPregunta(new Pregunta(
                                id,
                                rs.getString("enunciado"),
                                TipoPregunta.valueOf(rs.getString("tipo")),
                                rs.getBoolean("obligatoria"),
                                PreguntaDAO.opcionesDe(con, id)));
                    }
                }
            }
            return encuesta;
        }
    }
}
