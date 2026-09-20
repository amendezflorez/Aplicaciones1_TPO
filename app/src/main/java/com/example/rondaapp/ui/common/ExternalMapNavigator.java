package com.example.rondaapp.ui.common;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.rondaapp.R;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Servicio inyectable mediante Hilt responsable de la navegación externa hacia Google Maps.
 *
 * Encapsula la lógica de:
 * 1. Diálogo de confirmación alertando al usuario que saldrá de Ronda hacia una app externa.
 * 2. Construcción y despacho del Intent de navegación directa en Google Maps (turn-by-turn).
 * 3. Fallback seguro a navegador o aplicación de mapas universal ante la falta de la app de Google Maps.
 */
@Singleton
public class ExternalMapNavigator {

    @Inject
    public ExternalMapNavigator() {
        // Constructor inyectable por Hilt
    }

    /**
     * Muestra un diálogo de advertencia informando al usuario que abandonará la aplicación
     * antes de lanzar la navegación hacia la dirección de destino.
     *
     * @param context Contexto de la interfaz de usuario (generalmente Activity o requireContext()).
     * @param destination Dirección física o punto de encuentro predeterminado.
     */
    public void navigateWithWarning(@NonNull Context context, @NonNull String destination) {
        if (destination.trim().isEmpty()) {
            Toast.makeText(context, R.string.detail_delivery_point_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        String mensaje = context.getString(R.string.dialog_exit_app_message, destination.trim());

        new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_exit_app_title)
                .setMessage(mensaje)
                .setPositiveButton(R.string.dialog_exit_app_confirm, (dialog, which) -> {
                    openMapsNavigation(context, destination.trim());
                })
                .setNegativeButton(R.string.dialog_exit_app_cancel, null)
                .show();
    }

    /**
     * Abre Google Maps (o app de mapas/navegador web por defecto) con la dirección precargada.
     * Utiliza la URI estándar 'geo:0,0?q=' para fijar el marcador en la ubicación del destino
     * y desplegar el botón de navegación ("Cómo llegar"), evitando errores de cálculo de ruta
     * cuando la ubicación actual/GPS no está disponible o está fuera del área (ej. emulador).
     *
     * @param context Contexto para iniciar la actividad externa.
     * @param destination Dirección de destino.
     */
    public void openMapsNavigation(@NonNull Context context, @NonNull String destination) {
        String query = destination.trim();

        // Intento 1: Búsqueda y marcador directo en Google Maps vía esquema 'geo:'
        Uri geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, geoUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        try {
            context.startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {
            // Intento 2 (Fallback): URL de búsqueda universal de Google Maps
            Uri fallbackUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query));
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, fallbackUri);
            try {
                context.startActivity(fallbackIntent);
            } catch (Exception ex) {
                Toast.makeText(context, R.string.error_no_maps_app, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
