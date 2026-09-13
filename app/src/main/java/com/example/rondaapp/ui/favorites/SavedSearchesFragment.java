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
import com.example.rondaapp.data.model.SavedSearch;
import com.example.rondaapp.data.model.SavedSearchResponse;
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
 * Fragment para mostrar las búsquedas guardadas del usuario.
 * Permite ejecutar una búsqueda guardada o eliminarla.
 */
@AndroidEntryPoint
public class SavedSearchesFragment extends Fragment {

    @Inject
    ApiService apiService;

    @Inject
    SessionManager sessionManager;

    private RecyclerView rvSavedSearches;
    private SavedSearchAdapter adapter;
    private TextView tvEmptyState;
    private List<SavedSearch> savedSearches = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved_searches, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvSavedSearches = view.findViewById(R.id.rvSavedSearches);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        adapter = new SavedSearchAdapter();
        adapter.setOnDeleteListener(this::deleteSavedSearch);
        adapter.setOnExecuteListener(this::executeSavedSearch);
        rvSavedSearches.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSavedSearches.setAdapter(adapter);

        loadSavedSearches();
    }

    private void loadSavedSearches() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            showEmpty(getString(R.string.error_session_expired));
            return;
        }

        apiService.getSavedSearches(userId).enqueue(new Callback<SavedSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<SavedSearchResponse> call, @NonNull Response<SavedSearchResponse> response) {
                if (!isAdded()) return;

                // Igual que en Favoritos: el backend no manda "success", solo "data".
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    savedSearches = response.body().getData();
                    if (!savedSearches.isEmpty()) {
                        adapter.setSavedSearches(savedSearches);
                        tvEmptyState.setVisibility(View.GONE);
                    } else {
                        showEmpty(getString(R.string.no_saved_searches));
                    }
                } else {
                    showEmpty(getString(R.string.error_loading_searches));
                }
            }

            @Override
            public void onFailure(@NonNull Call<SavedSearchResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                showEmpty(getString(R.string.error_loading_searches));
            }
        });
    }

    private void deleteSavedSearch(int searchId) {
        apiService.deleteSavedSearch(searchId).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(@NonNull Call<SimpleResponse> call, @NonNull Response<SimpleResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    savedSearches.removeIf(s -> s.getId() == searchId);
                    adapter.setSavedSearches(savedSearches);
                    Toast.makeText(requireContext(), R.string.search_deleted, Toast.LENGTH_SHORT).show();
                    if (savedSearches.isEmpty()) {
                        showEmpty(getString(R.string.no_saved_searches));
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.error_deleting_search, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SimpleResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executeSavedSearch(SavedSearch search) {
        // TODO: próximo paso del Punto 11 — aplicar estos filtros de verdad en el Home.
        Toast.makeText(requireContext(), "Ejecutando búsqueda: " + search.getSearchTerm(), Toast.LENGTH_SHORT).show();
    }

    private void showEmpty(String message) {
        tvEmptyState.setText(message);
        tvEmptyState.setVisibility(View.VISIBLE);
    }
}