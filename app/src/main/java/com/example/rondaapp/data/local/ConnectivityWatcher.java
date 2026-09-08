package com.example.rondaapp.data.local;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

/**
 * Punto 6: estado de conectividad y aviso cuando vuelve la conexión.
 *
 * Las devoluciones de {@link ConnectivityManager} llegan en un hilo de sistema,
 * así que se reenvían al hilo principal: quien escucha actualiza vistas.
 */
public class ConnectivityWatcher {

    public interface Listener {
        /** @param hayConexion estado nuevo, ya en el hilo principal */
        void onConectividadCambio(boolean hayConexion);
    }

    private final ConnectivityManager connectivityManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ConnectivityManager.NetworkCallback callback;

    public ConnectivityWatcher(Context context) {
        this.connectivityManager = (ConnectivityManager)
                context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Hay internet de verdad, no solo una interfaz levantada: se exige la
     * capability INTERNET y, además, que la red esté validada. Sin eso, un wifi
     * conectado a un portal cautivo se reportaría como "con conexión".
     */
    public boolean hayConexion() {
        if (connectivityManager == null) return true; // sin forma de saber, no bloqueamos

        Network red = connectivityManager.getActiveNetwork();
        if (red == null) return false;

        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(red);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /** Empieza a avisar los cambios. Hay que cortar con {@link #dejarDeObservar()}. */
    public void observar(Listener listener) {
        if (connectivityManager == null || callback != null) return;

        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                mainHandler.post(() -> listener.onConectividadCambio(true));
            }

            @Override
            public void onLost(Network network) {
                // onLost llega por red perdida; puede quedar otra activa.
                mainHandler.post(() -> listener.onConectividadCambio(hayConexion()));
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, callback);
    }

    /** Debe llamarse en onDestroyView, igual que el CountDownTimer del OTP. */
    public void dejarDeObservar() {
        if (connectivityManager == null || callback == null) return;
        try {
            connectivityManager.unregisterNetworkCallback(callback);
        } catch (IllegalArgumentException ignored) {
            // Ya estaba dado de baja; no hay nada que hacer.
        }
        callback = null;
    }
}
