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
import androidx.navigation.Navigation;
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
    private SessionManager sessionManager;

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

        sessionManager = new SessionManager(requireContext());
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

        apiService.getSavedSearches().enqueue(new Callback<SavedSearchResponse>() {
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
        // Punto 11: le paso los filtros al Home por FragmentResult y vuelvo,
        // para que los aplique de verdad sobre el listado.
        Bundle result = new Bundle();
        result.putString("searchTerm", search.getSearchTerm());
        result.putString("category", search.getCategory());
        result.putString("condition", search.getCondition());
        result.putString("zone", search.getZone());
        if (search.getMinPrice() != null) result.putDouble("minPrice", search.getMinPrice());
        if (search.getMaxPrice() != null) result.putDouble("maxPrice", search.getMaxPrice());
        result.putString("sort", search.getSort() != null ? search.getSort() : "recent");

        // Ejecutarla es ver sus resultados: las novedades se vuelven a contar desde
        // ahora. Si el pedido falla el indicador sigue, que es preferible a
        // borrarlo sin que el backend lo sepa. La respuesta llega cuando este
        // fragment ya se fue, por eso los callbacks no tocan vistas.
        apiService.markSavedSearchSeen(search.getId()).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(@NonNull Call<SimpleResponse> call, @NonNull Response<SimpleResponse> response) { }

            @Override
            public void onFailure(@NonNull Call<SimpleResponse> call, @NonNull Throwable t) { }
        });

        getParentFragmentManager().setFragmentResult("execute_saved_search", result);
        Navigation.findNavController(requireView()).popBackStack(R.id.homeFragment, false);
    }

    private void showEmpty(String message) {
        tvEmptyState.setText(message);
        tvEmptyState.setVisibility(View.VISIBLE);
    }
}