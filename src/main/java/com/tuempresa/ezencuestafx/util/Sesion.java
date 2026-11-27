package com.tuempresa.ezencuestafx.util;

import com.tuempresa.ezencuestafx.model.Usuario;

/** Guarda quién inició sesión mientras la aplicación está abierta. */
public final class Sesion {

    private static Usuario usuarioActual;

    private Sesion() { }

    public static void iniciar(Usuario usuario) { usuarioActual = usuario; }

    public static Usuario getUsuario() { return usuarioActual; }

    public static boolean activa() { return usuarioActual != null; }

    public static void cerrar() { usuarioActual = null; }
}
