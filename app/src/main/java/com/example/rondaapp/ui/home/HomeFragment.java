package com.example.rondaapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rondaapp.R;
import com.example.rondaapp.data.model.PublicationResponse;
import com.example.rondaapp.data.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView rvPublications;
    private PublicationAdapter adapter;
    private SearchView searchView;
    private Spinner spinnerSort;

    private String currentSearch = null;
    private String currentSort = "recent";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvPublications = view.findViewById(R.id.rvPublications);
        searchView = view.findViewById(R.id.searchView);
        spinnerSort = view.findViewById(R.id.spinnerSort);

        // Configurar RecyclerView
        adapter = new PublicationAdapter();
        rvPublications.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPublications.setAdapter(adapter);

        // Configurar Spinner de Ordenamiento
        setupSortSpinner();

        // Configurar Buscador de texto
        setupSearchView();

        // Carga inicial de publicaciones
        fetchPublications();
    }

    private void setupSortSpinner() {
        String[] options = {"Más recientes", "Menor precio", "Mayor precio"};
        String[] sortKeys = {"recent", "price_asc", "price_desc"};

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, options);
        spinnerSort.setAdapter(spinnerAdapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSort = sortKeys[position];
                fetchPublications();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearch = query.trim().isEmpty() ? null : query.trim();
                fetchPublications();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty() && currentSearch != null) {
                    currentSearch = null;
                    fetchPublications();
                }
                return true;
            }
        });
    }

    private void fetchPublications() {
        RetrofitClient.getApiService().getPublications(
                currentSearch, null, null, null, null, null, currentSort, 1, 20
        ).enqueue(new Callback<PublicationResponse>() {
            @Override
            public void onResponse(Call<PublicationResponse> call, Response<PublicationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setPublications(response.body().getData());
                } else {
                    Toast.makeText(getContext(), "Error al cargar publicaciones", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PublicationResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}