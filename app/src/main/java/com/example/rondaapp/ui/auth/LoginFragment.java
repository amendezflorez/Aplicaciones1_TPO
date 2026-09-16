package com.example.rondaapp.ui.auth;

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
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.NavController;

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.AuthResponse;
import com.example.rondaapp.data.model.LoginBody;
import com.example.rondaapp.data.network.ApiService;
import com.example.rondaapp.session.SessionManager;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla de inicio de sesión (usuario y contraseña o enlace a OTP).
 */
@AndroidEntryPoint
public class LoginFragment extends Fragment {

    @Inject
    ApiService apiService;

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

        SessionManager sessionManager = new SessionManager(requireContext());
        if (sessionManager.isLoggedIn()) {
            Bundle args = new Bundle();
            String name = sessionManager.getName();
            args.putString("username", name != null ? name : "");
            requestBiometricAuth(view, args);
            return;
        }

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
                                        auth.getToken(), auth.getUserId(), auth.getEmail(), auth.getName(), auth.getZone());

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

    /**
     * Gate de UI: pide biometría/PIN antes de dejar entrar a un usuario con sesión guardada.
     * Si el dispositivo no tiene sensor ni credencial enrolada, no bloquea el acceso.
     */
    private void requestBiometricAuth(@NonNull View view, @NonNull Bundle args) {
        int allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

        BiometricManager biometricManager = BiometricManager.from(requireContext());
        int canAuthenticate = biometricManager.canAuthenticate(allowedAuthenticators);

        NavController navController = Navigation.findNavController(view);

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            navController.navigate(R.id.action_login_to_home, args);
            return;
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setDescription(getString(R.string.biometric_prompt_description))
                .setAllowedAuthenticators(allowedAuthenticators)
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(requireContext()),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        navController.navigate(R.id.action_login_to_home, args);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Log.e(TAG, "onAuthenticationError: " + errorCode + " " + errString);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Log.e(TAG, "onAuthenticationFailed");
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }
}
