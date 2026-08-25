package com.ronda.app.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
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
import com.ronda.app.data.model.OtpRequestBody;
import com.ronda.app.data.model.OtpVerifyBody;
import com.ronda.app.data.model.SimpleResponse;
import com.ronda.app.data.network.ApiService;
import com.ronda.app.data.network.RetrofitClient;
import com.ronda.app.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Paso 2 del flujo OTP: la persona ingresa el código de 6 dígitos recibido
 * por email. Si es válido, el backend confirma y crea la sesión.
 * También permite reenviar el código, con un cooldown de 60s entre reenvíos.
 */
public class OtpVerificationFragment extends Fragment {

    private static final String TAG = "OtpVerificationFragment";
    private static final long RESEND_COOLDOWN_MS = 60_000L;

    private String email;
    private CountDownTimer resendTimer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                              @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_otp_verification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        email = getArguments() != null ? getArguments().getString("email", "") : "";

        TextView tvOtpSubtitle = view.findViewById(R.id.tvOtpSubtitle);
        EditText etOtp = view.findViewById(R.id.etOtp);
        Button btnVerifyOtp = view.findViewById(R.id.btnVerifyOtp);
        Button btnResendOtp = view.findViewById(R.id.btnResendOtp);
        ProgressBar progressOtp = view.findViewById(R.id.progressOtp);

        tvOtpSubtitle.setText(getString(R.string.otp_subtitle, email));

        btnVerifyOtp.setOnClickListener(v -> {
            String code = etOtp.getText().toString().trim();

            if (code.length() != 6) {
                Toast.makeText(requireContext(), R.string.otp_error_invalid, Toast.LENGTH_SHORT).show();
                return;
            }

            progressOtp.setVisibility(View.VISIBLE);
            btnVerifyOtp.setEnabled(false);

            ApiService apiService = RetrofitClient.getInstance().create(ApiService.class);

            apiService.verifyOtp(new OtpVerifyBody(email, code))
                    .enqueue(new Callback<AuthResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<AuthResponse> call,
                                                @NonNull Response<AuthResponse> response) {
                            progressOtp.setVisibility(View.GONE);
                            btnVerifyOtp.setEnabled(true);

                            if (response.isSuccessful() && response.body() != null) {
                                AuthResponse auth = response.body();
                                new SessionManager(requireContext()).saveSession(
                                        auth.getToken(), auth.getUserId(), auth.getEmail(), auth.getName());

                                Bundle args = new Bundle();
                                args.putString("username", auth.getName() != null ? auth.getName() : email);

                                Navigation.findNavController(view)
                                        .navigate(R.id.action_otp_to_home, args);
                            } else {
                                Toast.makeText(requireContext(), R.string.otp_error_generic, Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error HTTP: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                            progressOtp.setVisibility(View.GONE);
                            btnVerifyOtp.setEnabled(true);
                            Toast.makeText(requireContext(), R.string.otp_error_generic, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "onFailure: " + t.getMessage());
                        }
                    });
        });

        btnResendOtp.setOnClickListener(v -> {
            ApiService apiService = RetrofitClient.getInstance().create(ApiService.class);

            apiService.resendOtp(new OtpRequestBody(email))
                    .enqueue(new Callback<SimpleResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<SimpleResponse> call,
                                                @NonNull Response<SimpleResponse> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), R.string.otp_resend_success, Toast.LENGTH_SHORT).show();
                                startResendCooldown(btnResendOtp);
                            } else {
                                Toast.makeText(requireContext(), R.string.otp_error_generic, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<SimpleResponse> call, @NonNull Throwable t) {
                            Toast.makeText(requireContext(), R.string.otp_error_generic, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        startResendCooldown(btnResendOtp);
    }

    private void startResendCooldown(Button btnResendOtp) {
        btnResendOtp.setEnabled(false);
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        resendTimer = new CountDownTimer(RESEND_COOLDOWN_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnResendOtp.setText(getString(R.string.otp_resend_timer, millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                btnResendOtp.setText(R.string.otp_resend_button);
                btnResendOtp.setEnabled(true);
            }
        }.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }
}
