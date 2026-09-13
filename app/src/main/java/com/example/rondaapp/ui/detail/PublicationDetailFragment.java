package com.example.rondaapp.ui.detail;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rondaapp.R;
import com.example.rondaapp.data.local.ConnectivityWatcher;
import com.example.rondaapp.data.local.OfflineCache;
import com.example.rondaapp.data.model.Offer;
import com.example.rondaapp.data.model.OfferBody;
import com.example.rondaapp.data.model.OffersResponse;
import com.example.rondaapp.data.model.PhotosResponse;
import com.example.rondaapp.data.model.Publication;
import com.example.rondaapp.data.model.PublicationDetailResponse;
import com.example.rondaapp.data.model.PublicationPhoto;
import com.example.rondaapp.data.model.PublicationStatusBody;
import com.example.rondaapp.data.model.Question;
import com.example.rondaapp.data.model.QuestionBody;
import com.example.rondaapp.data.model.QuestionsResponse;
import com.example.rondaapp.data.model.Reputation;
import com.example.rondaapp.data.model.Seller;
import com.example.rondaapp.data.network.ApiService;
import com.example.rondaapp.session.SessionManager;
import com.example.rondaapp.ui.profile.ProfileFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Punto 4: detalle de la publicación. Galería, datos completos, vendedor con su
 * reputación y acceso a su perfil público, y acciones distintas según quién mira:
 * el interesado pregunta y oferta, el vendedor gestiona su publicación.
 *
 * Cuando no hay conexión se muestra lo último que se guardó de esta publicación
 * (punto 6) y las acciones quedan bloqueadas.
 */
@AndroidEntryPoint
public class PublicationDetailFragment extends Fragment {

    /** Nombre del argumento de navegación con el id de la publicación. */
    public static final String ARG_PUBLICATION_ID = "publicationId";

    @Inject
    ApiService apiService;

    @Inject
    OfflineCache offlineCache;

    private SessionManager sessionManager;
    private ConnectivityWatcher connectivityWatcher;

    private int publicationId;
    private Publication publicacion;
    private boolean esPropia;

    private final PhotoGalleryAdapter galleryAdapter = new PhotoGalleryAdapter();
    private final QuestionAdapter questionAdapter = new QuestionAdapter();

    private ProgressBar progressDetail;
    private View contentDetail, containerGallery;
    private TextView tvOfflineBanner, tvPhotoPosition, tvNoPhotos;
    private TextView tvTitle, tvPrice, tvCondition, tvZone, tvCategory, tvDate, tvStatus, tvDescription;
    private TextView tvSellerName, tvSellerReputation, tvSellerOperations, tvSellerSeniority;
    private TextView tvQuestionsTitle, tvNoQuestions, tvOffersTitle, tvOffers;
    private LinearLayout panelInterested, panelSeller;
    private Button btnSellerProfile, btnAskQuestion, btnMakeOffer, btnTogglePause, btnMarkSold;
    private RecyclerView rvGallery;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            publicationId = getArguments().getInt(ARG_PUBLICATION_ID, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_publication_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        connectivityWatcher = new ConnectivityWatcher(requireContext());

        vincularVistas(view);
        configurarGaleria();
        configurarPreguntas(view);
        configurarAcciones();

        cargar();

        // Al volver la conexión se recarga solo, igual que el Home.
        connectivityWatcher.observar(hayConexion -> {
            if (!isAdded() || getView() == null) return;
            if (hayConexion) cargar();
        });
    }

    private void vincularVistas(View view) {
        progressDetail = view.findViewById(R.id.progressDetail);
        contentDetail = view.findViewById(R.id.contentDetail);
        containerGallery = view.findViewById(R.id.containerGallery);
        tvOfflineBanner = view.findViewById(R.id.tvDetailOfflineBanner);
        tvPhotoPosition = view.findViewById(R.id.tvPhotoPosition);
        tvNoPhotos = view.findViewById(R.id.tvNoPhotos);

        tvTitle = view.findViewById(R.id.tvDetailTitle);
        tvPrice = view.findViewById(R.id.tvDetailPrice);
        tvCondition = view.findViewById(R.id.tvDetailCondition);
        tvZone = view.findViewById(R.id.tvDetailZone);
        tvCategory = view.findViewById(R.id.tvDetailCategory);
        tvDate = view.findViewById(R.id.tvDetailDate);
        tvStatus = view.findViewById(R.id.tvDetailStatus);
        tvDescription = view.findViewById(R.id.tvDetailDescription);

        tvSellerName = view.findViewById(R.id.tvSellerName);
        tvSellerReputation = view.findViewById(R.id.tvSellerReputation);
        tvSellerOperations = view.findViewById(R.id.tvSellerOperations);
        tvSellerSeniority = view.findViewById(R.id.tvSellerSeniority);
        btnSellerProfile = view.findViewById(R.id.btnSellerProfile);

        panelInterested = view.findViewById(R.id.panelInterested);
        panelSeller = view.findViewById(R.id.panelSeller);
        btnAskQuestion = view.findViewById(R.id.btnAskQuestion);
        btnMakeOffer = view.findViewById(R.id.btnMakeOffer);
        btnTogglePause = view.findViewById(R.id.btnTogglePause);
        btnMarkSold = view.findViewById(R.id.btnMarkSold);

        tvQuestionsTitle = view.findViewById(R.id.tvQuestionsTitle);
        tvNoQuestions = view.findViewById(R.id.tvNoQuestions);
        tvOffersTitle = view.findViewById(R.id.tvOffersTitle);
        tvOffers = view.findViewById(R.id.tvOffers);

        rvGallery = view.findViewById(R.id.rvGallery);
    }

    private void configurarGaleria() {
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvGallery.setLayoutManager(layoutManager);
        rvGallery.setAdapter(galleryAdapter);
        // Con snap, cada gesto deja una foto centrada en vez de un scroll libre.
        new PagerSnapHelper().attachToRecyclerView(rvGallery);

        rvGallery.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int visible = layoutManager.findFirstVisibleItemPosition();
                if (visible != RecyclerView.NO_POSITION) {
                    actualizarContadorFotos(visible + 1, galleryAdapter.getItemCount());
                }
            }
        });
    }

    private void configurarPreguntas(View view) {
        RecyclerView rvQuestions = view.findViewById(R.id.rvQuestions);
        rvQuestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvQuestions.setAdapter(questionAdapter);
    }

    private void configurarAcciones() {
        btnAskQuestion.setOnClickListener(v -> {
            if (!exigirConexion()) return;
            pedirTexto(R.string.detail_ask_title, R.string.detail_ask_hint,
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE,
                    this::enviarPregunta);
        });

        btnMakeOffer.setOnClickListener(v -> {
            if (!exigirConexion()) return;
            pedirTexto(R.string.detail_offer_title, R.string.detail_offer_hint,
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL,
                    this::enviarOferta);
        });

        btnTogglePause.setOnClickListener(v -> {
            if (publicacion == null) return;
            cambiarEstado(PublicationStatusBody.PAUSADA.equals(publicacion.getStatus())
                    ? PublicationStatusBody.ACTIVA
                    : PublicationStatusBody.PAUSADA);
        });

        btnMarkSold.setOnClickListener(v -> cambiarEstado(PublicationStatusBody.VENDIDA));
    }

    // ==========================================
    // CARGA
    // ==========================================

    private void cargar() {
        if (!connectivityWatcher.hayConexion()) {
            mostrarDesdeCache();
            return;
        }

        progressDetail.setVisibility(View.VISIBLE);
        apiService.getPublicationDetail(publicationId).enqueue(new Callback<PublicationDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<PublicationDetailResponse> call,
                                   @NonNull Response<PublicationDetailResponse> response) {
                if (!isAdded() || getView() == null) return;
                progressDetail.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null
                        || response.body().getPublication() == null) {
                    Toast.makeText(getContext(), R.string.detail_loading_error, Toast.LENGTH_SHORT).show();
                    mostrarDesdeCache();
                    return;
                }

                tvOfflineBanner.setVisibility(View.GONE);
                mostrar(response.body().getPublication(), response.body().getSeller());
                cargarFotos();
                cargarPreguntas();
                if (esPropia) cargarOfertas();
            }

            @Override
            public void onFailure(@NonNull Call<PublicationDetailResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getView() == null) return;
                progressDetail.setVisibility(View.GONE);
                mostrarDesdeCache();
            }
        });
    }

    private void cargarFotos() {
        apiService.getPublicationPhotos(publicationId).enqueue(new Callback<PhotosResponse>() {
            @Override
            public void onResponse(@NonNull Call<PhotosResponse> call,
                                   @NonNull Response<PhotosResponse> response) {
                if (!isAdded() || getView() == null) return;
                if (!response.isSuccessful() || response.body() == null) return;

                List<String> fotos = new ArrayList<>();
                if (response.body().getData() != null) {
                    for (PublicationPhoto foto : response.body().getData()) {
                        if (foto.getData() != null) fotos.add(foto.getData());
                    }
                }
                mostrarFotos(fotos);

                // Punto 6: recién acá se tiene la publicación completa con sus fotos,
                // que es lo que hay que poder ver después sin conexión.
                if (publicacion != null) offlineCache.guardarVista(publicacion, fotos);
            }

            @Override
            public void onFailure(@NonNull Call<PhotosResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getView() == null) return;
                mostrarFotos(new ArrayList<>());
            }
        });
    }

    private void cargarPreguntas() {
        apiService.getQuestions(publicationId).enqueue(new Callback<QuestionsResponse>() {
            @Override
            public void onResponse(@NonNull Call<QuestionsResponse> call,
                                   @NonNull Response<QuestionsResponse> response) {
                if (!isAdded() || getView() == null) return;
                if (!response.isSuccessful() || response.body() == null) return;
                mostrarPreguntas(response.body().getData());
            }

            @Override
            public void onFailure(@NonNull Call<QuestionsResponse> call, @NonNull Throwable t) {
                // Las preguntas son secundarias: si fallan, el detalle igual sirve.
            }
        });
    }

    private void cargarOfertas() {
        apiService.getOffers(publicationId).enqueue(new Callback<OffersResponse>() {
            @Override
            public void onResponse(@NonNull Call<OffersResponse> call,
                                   @NonNull Response<OffersResponse> response) {
                if (!isAdded() || getView() == null) return;
                if (!response.isSuccessful() || response.body() == null) return;
                mostrarOfertas(response.body().getData());
            }

            @Override
            public void onFailure(@NonNull Call<OffersResponse> call, @NonNull Throwable t) {
                // Idem preguntas.
            }
        });
    }

    /**
     * Punto 6: lo último que se guardó de esta publicación. Si nunca se abrió con
     * conexión no hay nada que mostrar y se avisa.
     */
    private void mostrarDesdeCache() {
        offlineCache.leerVista(publicationId, cacheada -> {
            if (!isAdded() || getView() == null) return;

            if (cacheada == null) {
                Toast.makeText(getContext(), R.string.detail_loading_error, Toast.LENGTH_SHORT).show();
                return;
            }

            tvOfflineBanner.setVisibility(View.VISIBLE);
            mostrar(cacheada, null);
            offlineCache.leerFotos(publicationId, fotos -> {
                if (!isAdded() || getView() == null) return;
                mostrarFotos(fotos);
            });
        });
    }

    // ==========================================
    // PINTADO
    // ==========================================

    private void mostrar(Publication pub, @Nullable Seller vendedor) {
        publicacion = pub;
        contentDetail.setVisibility(View.VISIBLE);

        tvTitle.setText(pub.getTitle());
        tvPrice.setText(String.format(Locale.getDefault(), "$ %.2f", pub.getPrice()));
        tvCondition.setText(pub.getCondition() != null ? pub.getCondition().toUpperCase(Locale.getDefault()) : "");
        tvZone.setText(pub.getZone() != null ? "📍 " + pub.getZone() : "");
        tvCategory.setText(getString(R.string.detail_category,
                pub.getCategory() != null ? pub.getCategory() : ""));

        String fecha = ProfileFormatter.fechaCorta(pub.getCreatedAt());
        tvDate.setText(getString(R.string.detail_published_on, fecha != null ? fecha : ""));

        tvDescription.setText(pub.getDescription() != null && !pub.getDescription().trim().isEmpty()
                ? pub.getDescription()
                : getString(R.string.detail_no_description));

        mostrarVendedor(pub, vendedor);
        mostrarPaneles(pub);
    }

    private void mostrarVendedor(Publication pub, @Nullable Seller vendedor) {
        String nombre = vendedor != null && vendedor.getName() != null
                ? vendedor.getName()
                : pub.getSellerName();
        tvSellerName.setText(nombre != null ? nombre : "");

        Reputation reputacion = vendedor != null ? vendedor.getReputation() : null;
        tvSellerReputation.setText(ProfileFormatter.promedio(requireContext(), reputacion));
        tvSellerOperations.setText(ProfileFormatter.operaciones(requireContext(), reputacion));

        String antiguedad = vendedor != null
                ? ProfileFormatter.antiguedad(requireContext(), vendedor.getCreatedAt())
                : null;
        tvSellerSeniority.setText(antiguedad != null ? antiguedad : "");
        tvSellerSeniority.setVisibility(antiguedad != null ? View.VISIBLE : View.GONE);

        // Sin datos del vendedor (caché o publicación huérfana) no hay perfil que abrir.
        String sellerId = pub.getUserId();
        btnSellerProfile.setEnabled(sellerId != null);
        btnSellerProfile.setOnClickListener(sellerId == null ? null : v -> {
            if (!exigirConexion()) return;
            Bundle args = new Bundle();
            args.putString("userId", sellerId);
            Navigation.findNavController(v).navigate(R.id.action_detail_to_publicProfile, args);
        });
    }

    /** El TP pide acciones distintas según quién mira: interesado o dueño. */
    private void mostrarPaneles(Publication pub) {
        String userId = sessionManager.getUserId();
        esPropia = userId != null && userId.equals(pub.getUserId());

        panelInterested.setVisibility(esPropia ? View.GONE : View.VISIBLE);
        panelSeller.setVisibility(esPropia ? View.VISIBLE : View.GONE);

        if (!esPropia) return;

        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(getString(R.string.detail_status,
                pub.getStatus() != null ? pub.getStatus() : ""));

        boolean vendida = PublicationStatusBody.VENDIDA.equals(pub.getStatus());
        btnTogglePause.setVisibility(vendida ? View.GONE : View.VISIBLE);
        btnMarkSold.setVisibility(vendida ? View.GONE : View.VISIBLE);
        btnTogglePause.setText(PublicationStatusBody.PAUSADA.equals(pub.getStatus())
                ? R.string.my_publication_reactivate
                : R.string.my_publication_pause);
    }

    private void mostrarFotos(List<String> fotos) {
        boolean hay = fotos != null && !fotos.isEmpty();
        containerGallery.setVisibility(View.VISIBLE);
        galleryAdapter.setFotos(fotos);

        tvNoPhotos.setVisibility(hay ? View.GONE : View.VISIBLE);
        rvGallery.setVisibility(hay ? View.VISIBLE : View.GONE);
        if (hay) actualizarContadorFotos(1, fotos.size());
        else tvPhotoPosition.setVisibility(View.GONE);
    }

    private void actualizarContadorFotos(int actual, int total) {
        // Con una sola foto el contador no aporta nada.
        if (total <= 1) {
            tvPhotoPosition.setVisibility(View.GONE);
            return;
        }
        tvPhotoPosition.setVisibility(View.VISIBLE);
        tvPhotoPosition.setText(getString(R.string.detail_photo_position, actual, total));
    }

    private void mostrarPreguntas(List<Question> preguntas) {
        int total = preguntas != null ? preguntas.size() : 0;
        tvQuestionsTitle.setText(getString(R.string.detail_questions_title, total));
        tvNoQuestions.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
        questionAdapter.setPreguntas(preguntas);
    }

    private void mostrarOfertas(List<Offer> ofertas) {
        int total = ofertas != null ? ofertas.size() : 0;
        tvOffersTitle.setText(getString(R.string.detail_offers_title, total));

        if (total == 0) {
            tvOffers.setText(R.string.detail_no_offers);
            return;
        }

        StringBuilder texto = new StringBuilder();
        for (Offer oferta : ofertas) {
            if (texto.length() > 0) texto.append('\n');
            texto.append(getString(R.string.detail_offer_row,
                    oferta.getUserName() != null ? oferta.getUserName() : "",
                    String.format(Locale.getDefault(), "%.2f", oferta.getAmount())));
        }
        tvOffers.setText(texto.toString());
    }

    // ==========================================
    // ACCIONES
    // ==========================================

    private void enviarPregunta(String texto) {
        if (texto.trim().isEmpty()) return;

        apiService.askQuestion(publicationId, new QuestionBody(texto.trim()))
                .enqueue(new Callback<Question>() {
                    @Override
                    public void onResponse(@NonNull Call<Question> call, @NonNull Response<Question> response) {
                        if (!isAdded() || getView() == null) return;
                        if (!response.isSuccessful()) {
                            Toast.makeText(getContext(), R.string.detail_ask_error, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(getContext(), R.string.detail_ask_sent, Toast.LENGTH_SHORT).show();
                        cargarPreguntas();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Question> call, @NonNull Throwable t) {
                        if (!isAdded() || getView() == null) return;
                        Toast.makeText(getContext(), R.string.detail_ask_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void enviarOferta(String texto) {
        double monto;
        try {
            monto = Double.parseDouble(texto.trim());
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), R.string.detail_offer_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        if (monto <= 0) {
            Toast.makeText(getContext(), R.string.detail_offer_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.makeOffer(publicationId, new OfferBody(monto)).enqueue(new Callback<Offer>() {
            @Override
            public void onResponse(@NonNull Call<Offer> call, @NonNull Response<Offer> response) {
                if (!isAdded() || getView() == null) return;
                Toast.makeText(getContext(),
                        response.isSuccessful() ? R.string.detail_offer_sent : R.string.detail_offer_error,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<Offer> call, @NonNull Throwable t) {
                if (!isAdded() || getView() == null) return;
                Toast.makeText(getContext(), R.string.detail_offer_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cambiarEstado(String nuevoEstado) {
        if (!exigirConexion()) return;
        progressDetail.setVisibility(View.VISIBLE);

        apiService.updatePublicationStatus(publicationId, new PublicationStatusBody(nuevoEstado))
                .enqueue(new Callback<Publication>() {
                    @Override
                    public void onResponse(@NonNull Call<Publication> call, @NonNull Response<Publication> response) {
                        if (!isAdded() || getView() == null) return;
                        progressDetail.setVisibility(View.GONE);

                        if (!response.isSuccessful()) {
                            Toast.makeText(getContext(), R.string.detail_status_error, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Toast.makeText(getContext(), mensajeDeEstado(nuevoEstado), Toast.LENGTH_SHORT).show();
                        // Se recarga en vez de mutar en memoria, para quedar consistente
                        // con lo que realmente guardó el backend.
                        cargar();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Publication> call, @NonNull Throwable t) {
                        if (!isAdded() || getView() == null) return;
                        progressDetail.setVisibility(View.GONE);
                        Toast.makeText(getContext(), R.string.detail_status_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private int mensajeDeEstado(String estado) {
        if (PublicationStatusBody.VENDIDA.equals(estado)) return R.string.detail_sold;
        if (PublicationStatusBody.PAUSADA.equals(estado)) return R.string.my_publication_paused;
        return R.string.my_publication_reactivated;
    }

    // ==========================================
    // AUXILIARES
    // ==========================================

    private interface TextoIngresado {
        void onTexto(String texto);
    }

    /** Diálogo de una sola línea reutilizado por preguntar y ofertar. */
    private void pedirTexto(int titulo, int hint, int inputType, TextoIngresado callback) {
        View vista = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_text_input, null, false);
        EditText input = vista.findViewById(R.id.etDialogInput);
        input.setHint(hint);
        input.setInputType(inputType);

        new AlertDialog.Builder(requireContext())
                .setTitle(titulo)
                .setView(vista)
                .setPositiveButton(R.string.detail_send,
                        (dialog, which) -> callback.onTexto(input.getText().toString()))
                .setNegativeButton(R.string.detail_cancel, null)
                .show();
    }

    private boolean exigirConexion() {
        if (connectivityWatcher.hayConexion()) return true;
        Toast.makeText(requireContext(), R.string.offline_action_needs_connection,
                Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        connectivityWatcher.dejarDeObservar();
    }
}
