package com.example.rondaapp.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.OtpRequestBody;
import com.example.rondaapp.data.model.SimpleResponse;
import com.example.rondaapp.data.network.ApiService;
import com.example.rondaapp.data.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Paso 1 del flujo OTP: solicitar código de verificación por email.
 */
public class EmailAuthFragment extends Fragment {

    private static final String TAG = "EmailAuthFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                              @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_email_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etEmail = view.findViewById(R.id.etEmail);
        Button btnSendCode = view.findViewById(R.id.btnSendCode);
        ProgressBar progressEmailAuth = view.findViewById(R.id.progressEmailAuth);

        btnSendCode.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), R.string.email_error_invalid, Toast.LENGTH_SHORT).show();
                return;
            }

            progressEmailAuth.setVisibility(View.VISIBLE);
            btnSendCode.setEnabled(false);

            ApiService apiService = RetrofitClient.getApiService();

            apiService.requestOtp(new OtpRequestBody(email))
                    .enqueue(new Callback<SimpleResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<SimpleResponse> call,
                                                @NonNull Response<SimpleResponse> response) {
                            progressEmailAuth.setVisibility(View.GONE);
                            btnSendCode.setEnabled(true);

                            if (response.isSuccessful()) {
                                Bundle args = new Bundle();
                                args.putString("email", email);
                                Navigation.findNavController(view)
                                        .navigate(R.id.action_emailAuth_to_otp, args);
                            } else {
                                Toast.makeText(requireContext(), R.string.email_error_generic, Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error HTTP: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<SimpleResponse> call, @NonNull Throwable t) {
                            progressEmailAuth.setVisibility(View.GONE);
                            btnSendCode.setEnabled(true);
                            Toast.makeText(requireContext(), R.string.email_error_generic, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "onFailure: " + t.getMessage());
                        }
                    });
        });
    }
}
