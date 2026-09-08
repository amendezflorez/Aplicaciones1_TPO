package com.example.rondaapp.ui.profile;

import android.content.Context;

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.Reputation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Formateo compartido entre el perfil propio y el perfil público:
 * reputación y antigüedad en la plataforma.
 */
final class ProfileFormatter {

    /** Formato en el que SQLite devuelve created_at. */
    private static final String FORMATO_BACKEND = "yyyy-MM-dd HH:mm:ss";

    private ProfileFormatter() {}

    /** "4.5 ★", o el texto de "sin calificaciones" si todavía no recibió ninguna. */
    static String promedio(Context context, Reputation reputation) {
        if (reputation == null || !reputation.tieneCalificaciones()) {
            return context.getString(R.string.reputation_none);
        }
        String promedio = String.format(Locale.getDefault(), "%.1f", reputation.getAverage());
        return context.getString(R.string.reputation_average, promedio);
    }

    /** "3 ventas · 1 compras". */
    static String operaciones(Context context, Reputation reputation) {
        int ventas = reputation != null ? reputation.getSalesCount() : 0;
        int compras = reputation != null ? reputation.getPurchasesCount() : 0;
        return context.getString(R.string.reputation_operations, ventas, compras);
    }

    static String cantidadCalificaciones(Context context, Reputation reputation) {
        int total = reputation != null ? reputation.getTotalRatings() : 0;
        return context.getString(R.string.reputation_ratings_count, total);
    }

    /**
     * Antigüedad en la plataforma a partir de la fecha de alta. Si la fecha no se
     * puede parsear se devuelve null y la vista simplemente no muestra el dato,
     * en vez de romper por un formato inesperado del backend.
     */
    static String antiguedad(Context context, String createdAt) {
        Date alta = parsear(createdAt);
        if (alta == null) return null;

        Calendar desde = Calendar.getInstance();
        desde.setTime(alta);
        Calendar ahora = Calendar.getInstance();

        int meses = (ahora.get(Calendar.YEAR) - desde.get(Calendar.YEAR)) * 12
                + (ahora.get(Calendar.MONTH) - desde.get(Calendar.MONTH));
        if (ahora.get(Calendar.DAY_OF_MONTH) < desde.get(Calendar.DAY_OF_MONTH)) {
            meses--;
        }

        if (meses < 1) {
            return context.getString(R.string.public_profile_seniority_days);
        }
        if (meses < 12) {
            return context.getString(R.string.public_profile_seniority_months, meses);
        }
        return context.getString(R.string.public_profile_seniority_years, meses / 12);
    }

    /** "Miembro desde 08/09/2026". */
    static String miembroDesde(Context context, String createdAt) {
        Date alta = parsear(createdAt);
        if (alta == null) return null;

        String fecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(alta);
        return context.getString(R.string.public_profile_member_since, fecha);
    }

    private static Date parsear(String createdAt) {
        if (createdAt == null || createdAt.trim().isEmpty()) return null;
        try {
            return new SimpleDateFormat(FORMATO_BACKEND, Locale.US).parse(createdAt);
        } catch (ParseException e) {
            return null;
        }
    }
}
