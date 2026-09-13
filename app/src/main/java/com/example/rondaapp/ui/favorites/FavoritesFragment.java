package com.example.rondaapp.ui.favorites;

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
import com.example.rondaapp.data.model.FavoriteResponse;
import com.example.rondaapp.data.network.ApiService;
import com.example.rondaapp.session.SessionManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class FavoritesFragment extends Fragment {

    @Inject
    ApiService apiService;

    @Inject
    SessionManager sessionManager;

    private RecyclerView rvFavorites;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private FavoritePublicationAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFavorites = view.findViewById(R.id.rvFavorites);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        adapter = new FavoritePublicationAdapter();
        rvFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFavorites.setAdapter(adapter);

        loadFavorites();
    }

    private void loadFavorites() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            showError(getString(R.string.error_session_expired));
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        apiService.getFavorites(userId).enqueue(new Callback<FavoriteResponse>() {
            @Override
            public void onResponse(@NonNull Call<FavoriteResponse> call, @NonNull Response<FavoriteResponse> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getData().isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText(R.string.no_favorites);
                    } else {
                        adapter.setFavorites(response.body().getData());
                    }
                } else {
                    showError(getString(R.string.error_loading_favorites));
                }
            }

            @Override
            public void onFailure(@NonNull Call<FavoriteResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                showError(getString(R.string.error_loading_favorites));
            }
        });
    }

    private void showError(String message) {
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}