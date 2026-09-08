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
import com.example.rondaapp.data.network.RetrofitClient;
import com.example.rondaapp.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment para mostrar las publicaciones marcadas como favoritas por el usuario.
 * Permite eliminar favoritos y sincronizar con el servidor.
 */
public class FavoritesFragment extends Fragment {

    private RecyclerView rvFavorites;
    private FavoritePublicationAdapter adapter;
    private TextView tvEmptyState;
    private SessionManager sessionManager;
    private List<Favorite> favorites = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        rvFavorites = view.findViewById(R.id.rvFavorites);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        adapter = new FavoritePublicationAdapter();
        adapter.setOnRemoveFavoriteListener(this::removeFavorite);
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFavorites.setAdapter(adapter);

        loadFavorites();
    }

    private void loadFavorites() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            tvEmptyState.setText(R.string.error_session_expired);
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        RetrofitClient.getApiService().getFavorites(userId).enqueue(new Callback<FavoriteResponse>() {
            @Override
            public void onResponse(Call<FavoriteResponse> call, Response<FavoriteResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    favorites = response.body().getData();
                    if (favorites != null && !favorites.isEmpty()) {
                        adapter.setFavorites(favorites);
                        tvEmptyState.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setText(R.string.no_favorites);
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvEmptyState.setText(R.string.error_loading_favorites);
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<FavoriteResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                tvEmptyState.setText(R.string.error_loading_favorites);
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void removeFavorite(int favoriteId) {
        RetrofitClient.getApiService().deleteFavorite(favoriteId).enqueue(new Callback<com.example.rondaapp.data.model.SimpleResponse>() {
            @Override
            public void onResponse(Call<com.example.rondaapp.data.model.SimpleResponse> call, Response<com.example.rondaapp.data.model.SimpleResponse> response) {
                if (response.isSuccessful()) {
                    favorites.removeIf(f -> f.getId() == favoriteId);
                    adapter.setFavorites(favorites);
                    Toast.makeText(getContext(), R.string.favorite_removed, Toast.LENGTH_SHORT).show();

                    if (favorites.isEmpty()) {
                        tvEmptyState.setText(R.string.no_favorites);
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(getContext(), R.string.error_removing_favorite, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.rondaapp.data.model.SimpleResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
