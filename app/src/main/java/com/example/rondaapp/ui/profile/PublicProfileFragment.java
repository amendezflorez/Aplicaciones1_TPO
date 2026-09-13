package com.example.rondaapp.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.Publication;
import com.example.rondaapp.data.model.UserProfile;
import com.example.rondaapp.data.network.ApiService;
import com.example.rondaapp.ui.home.PublicationAdapter;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Punto 2: perfil público de la otra parte, consultable antes de operar.
 * Muestra su reputación, su antigüedad en la plataforma y sus publicaciones
 * activas.
 */
@AndroidEntryPoint
public class PublicProfileFragment extends Fragment {

    @Inject
    ApiService apiService;

    /** Id del usuario a mostrar; llega como argumento de navegación. */
    static final String ARG_USER_ID = "userId";

    private TextView tvPublicName, tvPublicZone, tvPublicSeniority;
    private TextView tvPublicReputationAverage, tvPublicReputationCount, tvPublicReputationOperations;
    private TextView tvActivePublicationsTitle, tvNoPublications;
    private RecyclerView rvPublicPublications;
    private ProgressBar progressPublicProfile;
    private PublicationAdapter adapter;

    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_public_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvPublicName = view.findViewById(R.id.tvPublicName);
        tvPublicZone = view.findViewById(R.id.tvPublicZone);
        tvPublicSeniority = view.findViewById(R.id.tvPublicSeniority);
        tvPublicReputationAverage = view.findViewById(R.id.tvPublicReputationAverage);
        tvPublicReputationCount = view.findViewById(R.id.tvPublicReputationCount);
        tvPublicReputationOperations = view.findViewById(R.id.tvPublicReputationOperations);
        tvActivePublicationsTitle = view.findViewById(R.id.tvActivePublicationsTitle);
        tvNoPublications = view.findViewById(R.id.tvNoPublications);
        rvPublicPublications = view.findViewById(R.id.rvPublicPublications);
        progressPublicProfile = view.findViewById(R.id.progressPublicProfile);

        adapter = new PublicationAdapter();
        rvPublicPublications.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPublicPublications.setAdapter(adapter);

        userId = getArguments() != null ? getArguments().getString(ARG_USER_ID) : null;
        cargarPerfil();
    }

    private void cargarPerfil() {
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.profile_error_load, Toast.LENGTH_SHORT).show();
            return;
        }

        progressPublicProfile.setVisibility(View.VISIBLE);
        apiService.getUserProfile(userId).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(@NonNull Call<UserProfile> call, @NonNull Response<UserProfile> response) {
                if (!isAdded() || getView() == null) return;
                progressPublicProfile.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(getContext(), R.string.profile_error_load, Toast.LENGTH_SHORT).show();
                    return;
                }
                pintarPerfil(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<UserProfile> call, @NonNull Throwable t) {
                if (!isAdded() || getView() == null) return;
                progressPublicProfile.setVisibility(View.GONE);
                Toast.makeText(getContext(), R.string.profile_error_load, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pintarPerfil(UserProfile perfil) {
        tvPublicName.setText(perfil.getName() != null ? perfil.getName() : "");

        boolean tieneZona = perfil.getZone() != null && !perfil.getZone().trim().isEmpty();
        tvPublicZone.setText(tieneZona ? getString(R.string.public_profile_zone, perfil.getZone()) : "");
        tvPublicZone.setVisibility(tieneZona ? View.VISIBLE : View.GONE);

        String antiguedad = ProfileFormatter.antiguedad(requireContext(), perfil.getCreatedAt());
        tvPublicSeniority.setText(antiguedad != null ? antiguedad : "");
        tvPublicSeniority.setVisibility(antiguedad != null ? View.VISIBLE : View.GONE);

        tvPublicReputationAverage.setText(ProfileFormatter.promedio(requireContext(), perfil.getReputation()));
        tvPublicReputationCount.setText(ProfileFormatter.cantidadCalificaciones(requireContext(), perfil.getReputation()));
        tvPublicReputationOperations.setText(ProfileFormatter.operaciones(requireContext(), perfil.getReputation()));

        List<Publication> activas = perfil.getActivePublications();
        int cantidad = (activas != null) ? activas.size() : 0;
        tvActivePublicationsTitle.setText(getString(R.string.public_profile_active_publications, cantidad));

        adapter.setPublications(activas);
        tvNoPublications.setVisibility(cantidad == 0 ? View.VISIBLE : View.GONE);
        rvPublicPublications.setVisibility(cantidad == 0 ? View.GONE : View.VISIBLE);
    }
}
