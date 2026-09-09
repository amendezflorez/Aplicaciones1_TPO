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
 * Antes el token se guardaba en {@link SessionManager} y no salía nunca de ahí.
 * Los requests de login y OTP pasan de largo sin header, porque justamente
 * corren cuando todavía no hay sesión.
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

        return chain.proceed(original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build());
    }
}
