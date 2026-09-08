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
import com.example.rondaapp.data.network.RetrofitClient;
import com.example.rondaapp.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment para mostrar las búsquedas guardadas del usuario.
 * Permite ejecutar una búsqueda guardada o eliminarla.
 */
public class SavedSearchesFragment extends Fragment {

    private RecyclerView rvSavedSearches;
    private SavedSearchAdapter adapter;
    private TextView tvEmptyState;
    private SessionManager sessionManager;
    private List<SavedSearch> savedSearches = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved_searches, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        rvSavedSearches = view.findViewById(R.id.rvSavedSearches);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        adapter = new SavedSearchAdapter();
        adapter.setOnDeleteListener(this::deleteSavedSearch);
        adapter.setOnExecuteListener(this::executeSavedSearch);
        rvSavedSearches.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSavedSearches.setAdapter(adapter);

        loadSavedSearches();
    }

    private void loadSavedSearches() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            tvEmptyState.setText(R.string.error_session_expired);
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        RetrofitClient.getApiService().getSavedSearches(userId).enqueue(new Callback<SavedSearchResponse>() {
            @Override
            public void onResponse(Call<SavedSearchResponse> call, Response<SavedSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    savedSearches = response.body().getData();
                    if (savedSearches != null && !savedSearches.isEmpty()) {
                        adapter.setSavedSearches(savedSearches);
                        tvEmptyState.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setText(R.string.no_saved_searches);
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvEmptyState.setText(R.string.error_loading_searches);
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<SavedSearchResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                tvEmptyState.setText(R.string.error_loading_searches);
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void deleteSavedSearch(int searchId) {
        RetrofitClient.getApiService().deleteSavedSearch(searchId).enqueue(new Callback<com.example.rondaapp.data.model.SimpleResponse>() {
            @Override
            public void onResponse(Call<com.example.rondaapp.data.model.SimpleResponse> call, Response<com.example.rondaapp.data.model.SimpleResponse> response) {
                if (response.isSuccessful()) {
                    savedSearches.removeIf(s -> s.getId() == searchId);
                    adapter.setSavedSearches(savedSearches);
                    Toast.makeText(getContext(), R.string.search_deleted, Toast.LENGTH_SHORT).show();

                    if (savedSearches.isEmpty()) {
                        tvEmptyState.setText(R.string.no_saved_searches);
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(getContext(), R.string.error_deleting_search, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.rondaapp.data.model.SimpleResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void executeSavedSearch(SavedSearch search) {
        // Aquí iría la lógica para ejecutar la búsqueda guardada
        // Por ahora, mostrar un toast
        Toast.makeText(getContext(), "Ejecutando búsqueda: " + search.getSearchTerm(), Toast.LENGTH_SHORT).show();
    }
}
