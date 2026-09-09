package com.example.rondaapp.data.network;

import androidx.annotation.NonNull;

import com.example.rondaapp.session.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Adjunta el token de la sesión a cada request como header Authorization.
 *
 * Los requests de login y OTP pasan de largo sin header, porque justamente
 * corren cuando todavía no hay sesión.
 *
 * Si el backend responde 401, el token dejó de valer del otro lado: se limpia
 * la sesión local para que la app vuelva al Login en vez de insistir con él.
 */
public class AuthInterceptor implements Interceptor {

    private final SessionManager sessionManager;

    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String token = sessionManager.getToken();

        if (token == null) {
            return chain.proceed(original);
        }

        Response response = chain.proceed(original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build());

        if (response.code() == 401) {
            // El backend no reconoce el token: la sesion murio del otro lado, asi
            // que guardarla aca ya no sirve de nada. Al proximo arranque la app
            // cae en el Login en vez de reintentar con un token muerto.
            sessionManager.clear();
        }

        return response;
    }
}
