package com.example.rondaapp.ui.publish;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Borrador de la carga guiada, persistido en SharedPreferences.
 *
 * El TP pide que si la persona interrumpe la carga y sale de la app, al volver
 * encuentre el borrador para retomarlo donde lo dejó: por eso se guarda también
 * el paso en el que iba, no solo los campos.
 *
 * Las fotos se guardan como nombres de archivo dentro de {@link PhotoStore},
 * no como URI de la galería, que no sobreviven al proceso.
 */
class PublishDraft {

    private static final String PREFS_NAME = "ronda_draft";
    private static final String KEY_PASO = "paso";
    private static final String KEY_TITULO = "titulo";
    private static final String KEY_DESCRIPCION = "descripcion";
    private static final String KEY_CATEGORIA = "categoria";
    private static final String KEY_PRECIO = "precio";
    private static final String KEY_CONDICION = "condicion";
    private static final String KEY_ZONA = "zona";
    private static final String KEY_FOTOS = "fotos";

    private final SharedPreferences prefs;
    private final Context context;

    int paso;
    String titulo = "";
    String descripcion = "";
    String categoria = "";
    String precio = "";
    String condicion = "";
    String zona = "";
    final List<File> fotos = new ArrayList<>();

    PublishDraft(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** True si hay algo cargado; se usa para ofrecer retomar o empezar de cero. */
    boolean hayBorrador() {
        return !titulo.isEmpty() || !descripcion.isEmpty() || !precio.isEmpty()
                || !categoria.isEmpty() || !condicion.isEmpty() || !zona.isEmpty()
                || !fotos.isEmpty();
    }

    void cargar() {
        paso = prefs.getInt(KEY_PASO, 0);
        titulo = prefs.getString(KEY_TITULO, "");
        descripcion = prefs.getString(KEY_DESCRIPCION, "");
        categoria = prefs.getString(KEY_CATEGORIA, "");
        precio = prefs.getString(KEY_PRECIO, "");
        condicion = prefs.getString(KEY_CONDICION, "");
        zona = prefs.getString(KEY_ZONA, "");

        fotos.clear();
        String json = prefs.getString(KEY_FOTOS, null);
        if (json != null) {
            List<String> nombres = new Gson().fromJson(json, new TypeToken<List<String>>() {}.getType());
            if (nombres != null) {
                for (String nombre : nombres) {
                    fotos.add(new File(PhotoStore.carpeta(context), nombre));
                }
            }
        }
        // Si el archivo ya no está (limpieza de almacenamiento, por ejemplo) se
        // descarta la entrada en vez de mostrar un hueco.
        for (Iterator<File> it = fotos.iterator(); it.hasNext(); ) {
            if (!it.next().exists()) it.remove();
        }
    }

    void guardar() {
        List<String> nombres = new ArrayList<>();
        for (File foto : fotos) {
            nombres.add(foto.getName());
        }

        prefs.edit()
                .putInt(KEY_PASO, paso)
                .putString(KEY_TITULO, titulo)
                .putString(KEY_DESCRIPCION, descripcion)
                .putString(KEY_CATEGORIA, categoria)
                .putString(KEY_PRECIO, precio)
                .putString(KEY_CONDICION, condicion)
                .putString(KEY_ZONA, zona)
                .putString(KEY_FOTOS, new Gson().toJson(nombres))
                .apply();
    }

    /** Borra el borrador y sus fotos. Se llama al publicar o al descartar. */
    void limpiar() {
        prefs.edit().clear().apply();
        PhotoStore.limpiar(context);

        paso = 0;
        titulo = descripcion = categoria = precio = condicion = zona = "";
        fotos.clear();
    }
}
