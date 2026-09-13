package com.example.rondaapp.ui.favorites;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.Favorite;
import com.example.rondaapp.data.model.FavoriteResponse;
import com.example.rondaapp.data.model.SimpleResponse;
import com.example.rondaapp.data.network.ApiService;
import com.example.rondaapp.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment para mostrar las publicaciones marcadas como favoritas por el usuario.
 * Permite eliminar favoritos y sincronizar con el servidor.
 */
@AndroidEntryPoint
public class FavoritesFragment extends Fragment {

    @Inject
    ApiService apiService;

    @Inject
    SessionManager sessionManager;

    private RecyclerView rvFavorites;
    private FavoritePublicationAdapter adapter;
    private TextView tvEmptyState;
    private List<Favorite> favorites = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFavorites = view.findViewById(R.id.rvFavorites);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        adapter = new FavoritePublicationAdapter();
        adapter.setOnRemoveFavoriteListener(this::removeFavorite);
        rvFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFavorites.setAdapter(adapter);

        loadFavorites();
    }

    private void loadFavorites() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            showEmpty(getString(R.string.error_session_expired));
            return;
        }

        apiService.getFavorites(userId).enqueue(new Callback<FavoriteResponse>() {
            @Override
            public void onResponse(@NonNull Call<FavoriteResponse> call, @NonNull Response<FavoriteResponse> response) {
                if (!isAdded()) return;

                // Ojo: el backend nunca manda un campo "success", solo "data".
                // Antes se chequeaba response.body().isSuccess() y por eso esto
                // nunca cargaba nada, aunque la respuesta viniera bien.
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    favorites = response.body().getData();
                    if (!favorites.isEmpty()) {
                        adapter.setFavorites(favorites);
                        tvEmptyState.setVisibility(View.GONE);
                    } else {
                        showEmpty(getString(R.string.no_favorites));
                    }
                } else {
                    showEmpty(getString(R.string.error_loading_favorites));
                }
            }

            @Override
            public void onFailure(@NonNull Call<FavoriteResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showEmpty(getString(R.string.error_loading_favorites));
            }
        });
    }

    private void removeFavorite(int favoriteId) {
        apiService.deleteFavorite(favoriteId).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(@NonNull Call<SimpleResponse> call, @NonNull Response<SimpleResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    favorites.removeIf(f -> f.getId() == favoriteId);
                    adapter.setFavorites(favorites);
                    Toast.makeText(requireContext(), R.string.favorite_removed, Toast.LENGTH_SHORT).show();
                    if (favorites.isEmpty()) {
                        showEmpty(getString(R.string.no_favorites));
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.error_removing_favorite, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SimpleResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmpty(String message) {
        tvEmptyState.setText(message);
        tvEmptyState.setVisibility(View.VISIBLE);
    }
}