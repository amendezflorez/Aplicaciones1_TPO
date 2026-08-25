package com.ronda.app.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.ronda.app.R;
import com.ronda.app.data.model.AuthResponse;
import com.ronda.app.data.model.LoginBody;
import com.ronda.app.data.network.ApiService;
import com.ronda.app.data.network.RetrofitClient;
import com.ronda.app.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla de entrada a la app. Permite loguearse con usuario y contraseña,
 * o bien ir a la alternativa de login por email + código OTP.
 */
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                              @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etUsername = view.findViewById(R.id.etUsername);
        EditText etPassword = view.findViewById(R.id.etPassword);
        Button btnLogin = view.findViewById(R.id.btnLogin);
        TextView tvLoginWithOtp = view.findViewById(R.id.tvLoginWithOtp);
        ProgressBar progressLogin = view.findViewById(R.id.progressLogin);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), R.string.login_error_empty_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            progressLogin.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);

            // Crear el servicio directamente (sin DI)
            ApiService apiService = RetrofitClient.getInstance().create(ApiService.class);

            apiService.loginWithPassword(new LoginBody(username, password))
                    .enqueue(new Callback<AuthResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<AuthResponse> call,
                                                @NonNull Response<AuthResponse> response) {
                            progressLogin.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);

                            if (response.isSuccessful() && response.body() != null) {
                                AuthResponse auth = response.body();
                                new SessionManager(requireContext()).saveSession(
                                        auth.getToken(), auth.getUserId(), auth.getEmail(), auth.getName());

                                Bundle args = new Bundle();
                                args.putString("username", auth.getName() != null ? auth.getName() : username);

                                Navigation.findNavController(view)
                                        .navigate(R.id.action_login_to_home, args);
                            } else {
                                Toast.makeText(requireContext(), R.string.login_error_generic, Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error HTTP: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                            progressLogin.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);
                            Toast.makeText(requireContext(), R.string.login_error_generic, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "onFailure: " + t.getMessage());
                        }
                    });
        });

        tvLoginWithOtp.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_login_to_emailAuth));
    }
}
