package com.example.rondaapp.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.PublicationResponse;
import com.example.rondaapp.data.network.RetrofitClient;
import com.example.rondaapp.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla principal del Home con exploración de publicaciones, buscador, filtros, ordenamiento y logout.
 */
public class HomeFragment extends Fragment {

    private RecyclerView rvPublications;
    private PublicationAdapter adapter;
    private SearchView searchView;
    private Spinner spinnerSort;
    private Button btnFilter;
    private TextView tvWelcome;
    private Button btnLogout;

    // Estados de búsqueda y filtros
    private String currentSearch = null;
    private String currentSort = "recent";
    private String selectedCategory = null;
    private String selectedCondition = null;
    private String selectedZone = null;
    private Double selectedMinPrice = null;
    private Double selectedMaxPrice = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SessionManager sessionManager = new SessionManager(requireContext());
        String username = getArguments() != null ? getArguments().getString("username", "") : "";
        if (username.isEmpty() && sessionManager.getName() != null) {
            username = sessionManager.getName();
        }

        tvWelcome = view.findViewById(R.id.tvWelcome);
        btnLogout = view.findViewById(R.id.btnLogout);
        rvPublications = view.findViewById(R.id.rvPublications);
        searchView = view.findViewById(R.id.searchView);
        spinnerSort = view.findViewById(R.id.spinnerSort);
        btnFilter = view.findViewById(R.id.btnFilter);

        if (tvWelcome != null) {
            tvWelcome.setText(getString(R.string.home_welcome, username));
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                sessionManager.clear();
                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build();
                Navigation.findNavController(view)
                        .navigate(R.id.auth_nav_graph, null, navOptions);
            });
        }

        adapter = new PublicationAdapter();
        rvPublications.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPublications.setAdapter(adapter);

        setupSortSpinner();
        setupSearchView();

        btnFilter.setOnClickListener(v -> showFiltersDialog());

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

    private void showFiltersDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filters, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        Spinner spCategory = dialogView.findViewById(R.id.spinnerCategory);
        Spinner spCondition = dialogView.findViewById(R.id.spinnerCondition);
        EditText etZone = dialogView.findViewById(R.id.etZone);
        EditText etMinPrice = dialogView.findViewById(R.id.etMinPrice);
        EditText etMaxPrice = dialogView.findViewById(R.id.etMaxPrice);
        Button btnApply = dialogView.findViewById(R.id.btnApplyFilters);
        Button btnClear = dialogView.findViewById(R.id.btnClearFilters);

        String[] categories = {"Todas", "Deportes", "Tecnología", "Hogar", "Música", "Indumentaria"};
        String[] conditions = {"Todos", "nuevo", "como nuevo", "usado"};

        spCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories));
        spCondition.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, conditions));

        if (selectedZone != null) etZone.setText(selectedZone);
        if (selectedMinPrice != null) etMinPrice.setText(String.valueOf(selectedMinPrice));
        if (selectedMaxPrice != null) etMaxPrice.setText(String.valueOf(selectedMaxPrice));

        btnApply.setOnClickListener(v -> {
            String cat = spCategory.getSelectedItem().toString();
            selectedCategory = cat.equals("Todas") ? null : cat;

            String cond = spCondition.getSelectedItem().toString();
            selectedCondition = cond.equals("Todos") ? null : cond;

            String zone = etZone.getText().toString().trim();
            selectedZone = zone.isEmpty() ? null : zone;

            String minP = etMinPrice.getText().toString().trim();
            selectedMinPrice = minP.isEmpty() ? null : Double.parseDouble(minP);

            String maxP = etMaxPrice.getText().toString().trim();
            selectedMaxPrice = maxP.isEmpty() ? null : Double.parseDouble(maxP);

            fetchPublications();
            dialog.dismiss();
        });

        btnClear.setOnClickListener(v -> {
            selectedCategory = null;
            selectedCondition = null;
            selectedZone = null;
            selectedMinPrice = null;
            selectedMaxPrice = null;
            fetchPublications();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void fetchPublications() {
        RetrofitClient.getApiService().getPublications(
                currentSearch,
                selectedCategory,
                selectedCondition,
                selectedMinPrice,
                selectedMaxPrice,
                selectedZone,
                currentSort,
                1,
                20
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