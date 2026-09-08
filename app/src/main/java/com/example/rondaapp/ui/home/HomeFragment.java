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
import android.widget.ProgressBar;
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
import com.example.rondaapp.data.model.Publication;
import com.example.rondaapp.data.model.PublicationResponse;
import com.example.rondaapp.data.network.RetrofitClient;
import com.example.rondaapp.session.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla principal del Home con exploración de publicaciones, buscador, filtros, ordenamiento y cierre de sesión.
 */
public class HomeFragment extends Fragment {

    /** Cantidad de publicaciones que se piden por página. */
    private static final int PAGE_SIZE = 20;
    /** Cuántos ítems antes del final disparan la carga de la página siguiente. */
    private static final int PREFETCH_THRESHOLD = 5;

    private RecyclerView rvPublications;
    private PublicationAdapter adapter;
    private LinearLayoutManager layoutManager;
    private SearchView searchView;
    private Spinner spinnerSort;
    private Button btnFilter;
    private TextView tvWelcome;
    private Button btnLogout;
    private Button btnMyProfile;
    private ProgressBar progressPaging;

    // Estados de búsqueda y filtros
    private String currentSearch = null;
    private String currentSort = "recent";
    private String selectedCategory = null;
    private String selectedCondition = null;
    private String selectedZone = null;
    private Double selectedMinPrice = null;
    private Double selectedMaxPrice = null;

    // Estado de la paginación
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

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
        btnMyProfile = view.findViewById(R.id.btnMyProfile);
        rvPublications = view.findViewById(R.id.rvPublications);
        searchView = view.findViewById(R.id.searchView);
        spinnerSort = view.findViewById(R.id.spinnerSort);
        btnFilter = view.findViewById(R.id.btnFilter);
        progressPaging = view.findViewById(R.id.progressPaging);

        if (tvWelcome != null) {
            tvWelcome.setText(getString(R.string.home_welcome, username));
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog(view, sessionManager));
        }

        if (btnMyProfile != null) {
            btnMyProfile.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.action_home_to_myProfile));
        }

        adapter = new PublicationAdapter();
        // Punto 2: desde la tarjeta se llega al perfil público del vendedor.
        adapter.setOnSellerClickListener(publication -> {
            Bundle args = new Bundle();
            args.putString("userId", publication.getUserId());
            Navigation.findNavController(view).navigate(R.id.action_home_to_publicProfile, args);
        });

        // Punto 4: navegación al detalle de la publicación
        adapter.setOnPublicationClickListener(publication -> {
            Bundle args = new Bundle();
            args.putInt("publicationId", publication.getId());
            Navigation.findNavController(view).navigate(R.id.action_home_to_detallePublicacion, args);
        });

        layoutManager = new LinearLayoutManager(getContext());
        rvPublications.setLayoutManager(layoutManager);
        rvPublications.setAdapter(adapter);
        setupInfiniteScroll();

        setupSortSpinner();
        setupSearchView();

        btnFilter.setOnClickListener(v -> showFiltersDialog());

        fetchPublications(true);
    }

    /**
     * Scroll infinito: cuando faltan pocos ítems para llegar al final de la lista,
     * pide la página siguiente. Los guardas de {@link #fetchPublications(boolean)}
     * evitan pedidos duplicados o pedir de más cuando ya no quedan resultados.
     */
    private void setupInfiniteScroll() {
        rvPublications.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy <= 0) return; // solo interesa el scroll hacia abajo
                if (isLoading || !hasMore) return;

                int visibles = layoutManager.getChildCount();
                int total = layoutManager.getItemCount();
                int primeroVisible = layoutManager.findFirstVisibleItemPosition();

                if (primeroVisible + visibles + PREFETCH_THRESHOLD >= total) {
                    fetchPublications(false);
                }
            }
        });
    }

    private void showLogoutConfirmationDialog(View view, SessionManager sessionManager) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout_dialog_title)
                .setMessage(R.string.logout_dialog_message)
                .setPositiveButton(R.string.logout_dialog_confirm, (dialog, which) -> {
                    // 1. Limpiar sesión guardada en SharedPreferences
                    sessionManager.clear();
                    Toast.makeText(requireContext(), R.string.logout_success_toast, Toast.LENGTH_SHORT).show();

                    // 2. Limpiar la pila de navegación y volver a la pantalla de Login
                    NavOptions navOptions = new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .build();

                    Navigation.findNavController(view)
                            .navigate(R.id.auth_nav_graph, null, navOptions);
                })
                .setNegativeButton(R.string.logout_dialog_cancel, (dialog, which) -> dialog.dismiss())
                .show();
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
                fetchPublications(true);
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
                fetchPublications(true);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty() && currentSearch != null) {
                    currentSearch = null;
                    fetchPublications(true);
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

            fetchPublications(true);
            dialog.dismiss();
        });

        btnClear.setOnClickListener(v -> {
            selectedCategory = null;
            selectedCondition = null;
            selectedZone = null;
            selectedMinPrice = null;
            selectedMaxPrice = null;
            fetchPublications(true);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * @param reset {@code true} cuando cambian búsqueda, filtros u ordenamiento: vuelve a la
     *              página 1 y reemplaza la lista. {@code false} cuando el scroll pide la
     *              página siguiente y hay que anexarla al final.
     */
    private void fetchPublications(boolean reset) {
        if (isLoading) return;
        if (!reset && !hasMore) return;

        if (reset) {
            currentPage = 1;
            hasMore = true;
        }

        final int paginaPedida = reset ? 1 : currentPage + 1;
        isLoading = true;
        mostrarProgreso(!reset);

        RetrofitClient.getApiService().getPublications(
                currentSearch,
                selectedCategory,
                selectedCondition,
                selectedMinPrice,
                selectedMaxPrice,
                selectedZone,
                currentSort,
                paginaPedida,
                PAGE_SIZE
        ).enqueue(new Callback<PublicationResponse>() {
            @Override
            public void onResponse(Call<PublicationResponse> call, Response<PublicationResponse> response) {
                if (!isAdded() || getView() == null) return; // la vista ya se destruyó
                isLoading = false;
                mostrarProgreso(false);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(getContext(), "Error al cargar publicaciones", Toast.LENGTH_SHORT).show();
                    return;
                }

                PublicationResponse body = response.body();
                List<Publication> pagina = body.getData();
                int recibidas = (pagina != null) ? pagina.size() : 0;

                if (reset) {
                    adapter.setPublications(pagina);
                } else {
                    adapter.addPublications(pagina);
                }
                currentPage = paginaPedida;

                // El backend devuelve el total que matchea los filtros; si por lo que sea
                // no viniera, caemos en la heurística de "vino una página incompleta".
                if (body.getTotal() > 0) {
                    hasMore = adapter.getItemCountLoaded() < body.getTotal();
                } else {
                    hasMore = recibidas == PAGE_SIZE;
                }
            }

            @Override
            public void onFailure(Call<PublicationResponse> call, Throwable t) {
                if (!isAdded() || getView() == null) return;
                isLoading = false;
                mostrarProgreso(false);
                Toast.makeText(getContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarProgreso(boolean visible) {
        if (progressPaging != null) {
            progressPaging.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}