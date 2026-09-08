package com.example.rondaapp.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.example.rondaapp.data.local.ConnectivityWatcher;
import com.example.rondaapp.data.local.OfflineCache;
import com.example.rondaapp.data.model.Publication;
import com.example.rondaapp.data.model.PublicationResponse;
import com.example.rondaapp.data.network.ApiService;
import com.example.rondaapp.session.SessionManager;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla principal del Home con exploración de publicaciones, buscador, filtros, ordenamiento y cierre de sesión.
 */
@AndroidEntryPoint
public class HomeFragment extends Fragment {

    @Inject
    ApiService apiService;

    @Inject
    OfflineCache offlineCache;

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
    private Button btnPublish;
    private Button btnMyPublications;
    private ProgressBar progressPaging;
    private SessionManager sessionManager;
    private TextView tvOfflineBanner;
    private ConnectivityWatcher connectivityWatcher;
    /** Se está mostrando el listado cacheado en vez del del servidor. */
    private boolean mostrandoCache = false;

    // Estados de búsqueda y filtros
    private String currentSearch = null;
    private String currentSort = "recent";
    private String selectedCategory = null;
    private String selectedCondition = null;
    private String selectedZone = null;
    private Double selectedMinPrice = null;
    private Double selectedMaxPrice = null;
    /** Zona propia cuando el filtro de cercanía está activo; null si no lo está. */
    private String selectedNearZone = null;

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

        sessionManager = new SessionManager(requireContext());
        connectivityWatcher = new ConnectivityWatcher(requireContext());
        String username = getArguments() != null ? getArguments().getString("username", "") : "";
        if (username.isEmpty() && sessionManager.getName() != null) {
            username = sessionManager.getName();
        }

        tvWelcome = view.findViewById(R.id.tvWelcome);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnMyProfile = view.findViewById(R.id.btnMyProfile);
        btnPublish = view.findViewById(R.id.btnPublish);
        btnMyPublications = view.findViewById(R.id.btnMyPublications);
        rvPublications = view.findViewById(R.id.rvPublications);
        searchView = view.findViewById(R.id.searchView);
        spinnerSort = view.findViewById(R.id.spinnerSort);
        btnFilter = view.findViewById(R.id.btnFilter);
        progressPaging = view.findViewById(R.id.progressPaging);
        tvOfflineBanner = view.findViewById(R.id.tvOfflineBanner);

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

        // Punto 5: dos entradas distintas, el formulario y la lista.
        if (btnPublish != null) {
            btnPublish.setOnClickListener(v -> {
                if (!exigirConexion()) return;
                Navigation.findNavController(v).navigate(R.id.action_home_to_publish);
            });
        }

        if (btnMyPublications != null) {
            btnMyPublications.setOnClickListener(v ->
                    Navigation.findNavController(v).navigate(R.id.action_home_to_myPublications));
        }

        adapter = new PublicationAdapter();
        // Punto 2: desde la tarjeta se llega al perfil público del vendedor.
        adapter.setOnSellerClickListener(publication -> {
            // El perfil público se pide al servidor: sin conexión no hay nada que mostrar.
            if (!exigirConexion()) return;
            Bundle args = new Bundle();
            args.putString("userId", publication.getUserId());
            Navigation.findNavController(view).navigate(R.id.action_home_to_publicProfile, args);
        });
        layoutManager = new LinearLayoutManager(getContext());
        rvPublications.setLayoutManager(layoutManager);
        rvPublications.setAdapter(adapter);
        setupInfiniteScroll();

        setupSortSpinner();
        setupSearchView();

        btnFilter.setOnClickListener(v -> showFiltersDialog());

        observarConectividad();
        fetchPublications(true);
    }

    /**
     * Punto 6: cuando vuelve la conexión se refresca solo, sin que el usuario
     * tenga que hacer nada. El watcher se da de baja en onDestroyView, igual que
     * el CountDownTimer del OTP.
     */
    private void observarConectividad() {
        connectivityWatcher.observar(hayConexion -> {
            if (!isAdded() || getView() == null) return;

            if (hayConexion && mostrandoCache) {
                Toast.makeText(getContext(), R.string.offline_reconnected, Toast.LENGTH_SHORT).show();
                fetchPublications(true);
            } else if (!hayConexion) {
                actualizarBanner();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        connectivityWatcher.dejarDeObservar();
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
                    // 1. Limpiar sesión y el caché offline, que es de esta persona
                    sessionManager.clear();
                    offlineCache.limpiar();
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
        CheckBox cbNearMyZone = dialogView.findViewById(R.id.cbNearMyZone);
        TextView tvNearZoneHint = dialogView.findViewById(R.id.tvNearZoneHint);

        configurarCercania(cbNearMyZone, tvNearZoneHint, etZone);

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

            // Si el usuario pidió cercanía, esa gana: no tiene sentido combinarla
            // con una zona exacta escrita a mano.
            if (cbNearMyZone.isChecked()) {
                selectedNearZone = sessionManager.getZone();
                selectedZone = null;
            } else {
                selectedNearZone = null;
                String zone = etZone.getText().toString().trim();
                selectedZone = zone.isEmpty() ? null : zone;
            }

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
            selectedNearZone = null;
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
    /**
     * El checkbox de cercanía solo sirve si el usuario tiene zona cargada, así que
     * queda deshabilitado con una explicación si no la tiene. Mientras esté tildado
     * se apaga el campo de zona exacta, porque los dos filtros se pisan.
     */
    private void configurarCercania(CheckBox cbNearMyZone, TextView tvNearZoneHint, EditText etZone) {
        String miZona = sessionManager.getZone();
        boolean tieneZona = miZona != null && !miZona.trim().isEmpty();

        cbNearMyZone.setEnabled(tieneZona);
        cbNearMyZone.setChecked(tieneZona && selectedNearZone != null);
        tvNearZoneHint.setText(tieneZona
                ? getString(R.string.filter_near_zone_hint, miZona)
                : getString(R.string.filter_near_zone_missing));

        etZone.setEnabled(!cbNearMyZone.isChecked());
        cbNearMyZone.setOnCheckedChangeListener((v, tildado) -> etZone.setEnabled(!tildado));
    }

    private void fetchPublications(boolean reset) {
        if (isLoading) return;
        if (!reset && !hasMore) return;

        if (reset) {
            currentPage = 1;
            hasMore = true;
        }

        // Punto 6: sin conexión no tiene sentido esperar el timeout de la request.
        if (!connectivityWatcher.hayConexion()) {
            mostrarDesdeCache(reset);
            return;
        }

        final int paginaPedida = reset ? 1 : currentPage + 1;
        isLoading = true;
        mostrarProgreso(!reset);

        apiService.getPublications(
                currentSearch,
                selectedCategory,
                selectedCondition,
                selectedMinPrice,
                selectedMaxPrice,
                selectedZone,
                selectedNearZone,
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
                    // Solo se cachea la primera página: es lo que se muestra al
                    // abrir el Home sin conexión.
                    offlineCache.guardarListado(pagina);
                    mostrandoCache = false;
                    // Una respuesta exitosa prueba que hay conexión mejor que
                    // NET_CAPABILITY_VALIDATED, que tarda unos segundos más en
                    // llegar y dejaría el aviso puesto sobre datos ya frescos.
                    tvOfflineBanner.setVisibility(View.GONE);
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

                // Punto 6: si el servidor no responde vale lo mismo que estar sin
                // conexión, así que se muestra lo último que se cargó bien.
                if (!reset) return;
                offlineCache.hayListado(hayCache -> {
                    if (!isAdded() || getView() == null) return;
                    if (hayCache) {
                        mostrarDesdeCache(true);
                    } else {
                        Toast.makeText(getContext(), "Error de red: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * Punto 6: muestra el último listado que llegó bien del servidor, avisando
     * que puede estar desactualizado. Sin paginación: lo cacheado es una sola
     * página, así que no hay más para pedir.
     */
    private void mostrarDesdeCache(boolean reset) {
        isLoading = false;
        mostrarProgreso(false);

        if (!reset) return; // el scroll no puede traer más de lo que hay guardado

        offlineCache.leerListado(filtrosActuales(), cacheadas -> {
            if (!isAdded() || getView() == null) return;
            adapter.setPublications(cacheadas);
            hasMore = false;
            mostrandoCache = true;
            actualizarBanner();
        });
    }

    /**
     * Traduce el estado de búsqueda, filtros y orden al mismo formato que se le
     * manda al backend, para que sin conexión la lista responda igual.
     */
    private OfflineCache.Filtros filtrosActuales() {
        OfflineCache.Filtros filtros = new OfflineCache.Filtros();
        filtros.busqueda = currentSearch;
        filtros.categoria = selectedCategory;
        filtros.condicion = selectedCondition;
        filtros.minPrice = selectedMinPrice;
        filtros.maxPrice = selectedMaxPrice;
        filtros.orden = currentSort;

        // La tabla de barrios linderos vive en el backend, así que sin conexión
        // la cercanía se resuelve como zona exacta: devuelve menos resultados,
        // nunca de más.
        if (selectedNearZone != null) {
            filtros.zonas = java.util.Collections.singletonList(selectedNearZone);
        } else if (selectedZone != null) {
            filtros.zonas = java.util.Collections.singletonList(selectedZone);
        }
        return filtros;
    }

    private void actualizarBanner() {
        if (tvOfflineBanner == null) return;

        boolean hayConexion = connectivityWatcher.hayConexion();
        if (hayConexion && !mostrandoCache) {
            tvOfflineBanner.setVisibility(View.GONE);
            return;
        }

        tvOfflineBanner.setVisibility(View.VISIBLE);
        offlineCache.guardadoEn(guardadoEn -> {
            if (!isAdded() || getView() == null) return;
            tvOfflineBanner.setText(guardadoEn > 0
                    ? getString(R.string.offline_banner, antiguedadDelCache(guardadoEn))
                    : getString(R.string.offline_banner_no_cache));
        });
    }

    /** "recién", "hace 5 min", "hace 2 h"… para que el aviso diga qué tan viejo es. */
    private String antiguedadDelCache(long guardadoEn) {
        if (guardadoEn <= 0) return getString(R.string.offline_just_now);

        long minutos = (System.currentTimeMillis() - guardadoEn) / 60000L;
        if (minutos < 1) return getString(R.string.offline_just_now);
        if (minutos < 60) return getString(R.string.offline_minutes_ago, minutos);

        long horas = minutos / 60;
        if (horas < 24) return getString(R.string.offline_hours_ago, horas);
        return getString(R.string.offline_days_ago, horas / 24);
    }

    /** Punto 6: las acciones que necesitan servidor se bloquean sin conexión. */
    private boolean exigirConexion() {
        if (connectivityWatcher.hayConexion()) return true;
        Toast.makeText(requireContext(), R.string.offline_action_needs_connection,
                Toast.LENGTH_SHORT).show();
        return false;
    }

    private void mostrarProgreso(boolean visible) {
        if (progressPaging != null) {
            progressPaging.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}