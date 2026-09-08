package com.example.rondaapp.ui.profile;

import android.os.Bundle;
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

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.UpdateProfileBody;
import com.example.rondaapp.data.model.UserProfile;
import com.example.rondaapp.data.network.RetrofitClient;
import com.example.rondaapp.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Punto 2: perfil propio. Permite ver y editar los datos personales
 * (nombre, email, teléfono de contacto y zona) y muestra la reputación,
 * que es de solo lectura porque la construye el backend con las
 * calificaciones recibidas.
 */
public class MyProfileFragment extends Fragment {

    private EditText etName, etEmail, etPhone, etZone;
    private Button btnSaveProfile;
    private ProgressBar progressProfile;
    private TextView tvReputationAverage, tvReputationCount, tvReputationOperations, tvSeniority;

    private SessionManager sessionManager;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etZone = view.findViewById(R.id.etZone);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);
        progressProfile = view.findViewById(R.id.progressProfile);
        tvReputationAverage = view.findViewById(R.id.tvReputationAverage);
        tvReputationCount = view.findViewById(R.id.tvReputationCount);
        tvReputationOperations = view.findViewById(R.id.tvReputationOperations);
        tvSeniority = view.findViewById(R.id.tvSeniority);

        sessionManager = new SessionManager(requireContext());
        userId = sessionManager.getUserId();

        btnSaveProfile.setOnClickListener(v -> guardarPerfil());

        cargarPerfil();
    }

    private void cargarPerfil() {
        if (userId == null) {
            Toast.makeText(requireContext(), R.string.profile_error_load, Toast.LENGTH_SHORT).show();
            return;
        }

        mostrarProgreso(true);
        RetrofitClient.getApiService().getUserProfile(userId).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(@NonNull Call<UserProfile> call, @NonNull Response<UserProfile> response) {
                if (!isAdded() || getView() == null) return;
                mostrarProgreso(false);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(getContext(), R.string.profile_error_load, Toast.LENGTH_SHORT).show();
                    return;
                }
                pintarPerfil(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<UserProfile> call, @NonNull Throwable t) {
                if (!isAdded() || getView() == null) return;
                mostrarProgreso(false);
                Toast.makeText(getContext(), R.string.profile_error_load, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pintarPerfil(UserProfile perfil) {
        etName.setText(perfil.getName() != null ? perfil.getName() : "");
        etEmail.setText(perfil.getEmail() != null ? perfil.getEmail() : "");
        etPhone.setText(perfil.getPhone() != null ? perfil.getPhone() : "");
        etZone.setText(perfil.getZone() != null ? perfil.getZone() : "");

        // El filtro de cercanía del Home lee la zona desde la sesión: se refresca acá.
        sessionManager.setZone(perfil.getZone());

        tvReputationAverage.setText(ProfileFormatter.promedio(requireContext(), perfil.getReputation()));
        tvReputationCount.setText(ProfileFormatter.cantidadCalificaciones(requireContext(), perfil.getReputation()));
        tvReputationOperations.setText(ProfileFormatter.operaciones(requireContext(), perfil.getReputation()));

        String miembroDesde = ProfileFormatter.miembroDesde(requireContext(), perfil.getCreatedAt());
        tvSeniority.setText(miembroDesde != null ? miembroDesde : "");
        tvSeniority.setVisibility(miembroDesde != null ? View.VISIBLE : View.GONE);
    }

    private void guardarPerfil() {
        String nombre = etName.getText().toString().trim();
        if (nombre.isEmpty()) {
            etName.setError(getString(R.string.profile_error_empty_name));
            return;
        }

        String email = etEmail.getText().toString().trim();
        String telefono = etPhone.getText().toString().trim();
        String zona = etZone.getText().toString().trim();

        mostrarProgreso(true);
        btnSaveProfile.setEnabled(false);

        UpdateProfileBody body = new UpdateProfileBody(
                nombre,
                email.isEmpty() ? null : email,
                telefono.isEmpty() ? null : telefono,
                zona.isEmpty() ? null : zona);

        RetrofitClient.getApiService().updateUserProfile(userId, body).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(@NonNull Call<UserProfile> call, @NonNull Response<UserProfile> response) {
                if (!isAdded() || getView() == null) return;
                mostrarProgreso(false);
                btnSaveProfile.setEnabled(true);

                if (!response.isSuccessful() || response.body() == null) {
                    // El backend responde 409 cuando el email ya está tomado y 400 si
                    // los datos no validan; en ambos casos alcanza con avisar.
                    Toast.makeText(getContext(), R.string.profile_error_save, Toast.LENGTH_SHORT).show();
                    return;
                }

                UserProfile actualizado = response.body();
                // El Home saluda con el nombre cacheado en la sesión: hay que refrescarlo.
                sessionManager.updateDatosBasicos(actualizado.getName(), actualizado.getEmail());
                sessionManager.setZone(actualizado.getZone());
                Toast.makeText(getContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<UserProfile> call, @NonNull Throwable t) {
                if (!isAdded() || getView() == null) return;
                mostrarProgreso(false);
                btnSaveProfile.setEnabled(true);
                Toast.makeText(getContext(), R.string.profile_error_save, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarProgreso(boolean visible) {
        if (progressProfile != null) {
            progressProfile.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
