package com.tuempresa.ezencuestafx.model;

public class Usuario {
    private final int id;
    private final String nombreCompleto;
    private final String correo;
    private final String usuario;
    private final Rol rol;

    public Usuario(int id, String nombreCompleto, String correo, String usuario, Rol rol) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.usuario = usuario;
        this.rol = rol;
    }

    public int getId() { return id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getCorreo() { return correo; }
    public String getUsuario() { return usuario; }
    public Rol getRol() { return rol; }

    /** Primer nombre, para saludar al usuario en la interfaz. */
    public String getPrimerNombre() {
        String limpio = nombreCompleto == null ? "" : nombreCompleto.trim();
        int espacio = limpio.indexOf(' ');
        return espacio > 0 ? limpio.substring(0, espacio) : limpio;
    }
}
