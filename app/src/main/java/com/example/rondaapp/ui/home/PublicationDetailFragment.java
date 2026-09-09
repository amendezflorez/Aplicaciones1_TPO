package com.example.rondaapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.Publication;
import com.example.rondaapp.data.network.PublicationApiService;
import com.example.rondaapp.session.SessionManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragmento de detalle de publicación con inyección de dependencias Hilt.
 */
@AndroidEntryPoint
public class PublicationDetailFragment extends Fragment {

    private int publicationId;
    
    @Inject
    PublicationApiService apiService;
    
    @Inject
    SessionManager sessionManager;

    private TextView tvTitle, tvPrice, tvDescription, tvCondition, tvCategory, tvDate, tvSellerName, tvSellerReputation;
    private LinearLayout panelInterested, panelSeller;
    private ImageView ivGallery;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // El ID viene como argumento del navigation
            publicationId = getArguments().getInt("publicationId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_publication_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        loadPublicationData();
    }

    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tvDetailTitle);
        tvPrice = view.findViewById(R.id.tvDetailPrice);
        tvDescription = view.findViewById(R.id.tvDetailDescription);
        tvCondition = view.findViewById(R.id.tvDetailCondition);
        tvCategory = view.findViewById(R.id.tvDetailCategory);
        tvDate = view.findViewById(R.id.tvDetailDate);
        tvSellerName = view.findViewById(R.id.tvSellerNameDetail);
        tvSellerReputation = view.findViewById(R.id.tvSellerReputationDetail);
        panelInterested = view.findViewById(R.id.panelInterestedActions);
        panelSeller = view.findViewById(R.id.panelSellerActions);
        ivGallery = view.findViewById(R.id.ivPublicationGallery);

        // Click listeners para botones de interesados
        view.findViewById(R.id.btnMakeOffer).setOnClickListener(v -> handleAction("Oferta realizada"));
        view.findViewById(R.id.btnAskQuestion).setOnClickListener(v -> handleAction("Pregunta enviada"));
        view.findViewById(R.id.btnSaveFavorite).setOnClickListener(v -> handleAction("Guardado en favoritos"));

        // Click listeners para botones de vendedor
        view.findViewById(R.id.btnEditPublication).setOnClickListener(v -> handleAction("Navegar a edición"));
        view.findViewById(R.id.btnPausePublication).setOnClickListener(v -> handleAction("Publicación pausada"));
        view.findViewById(R.id.btnDeletePublication).setOnClickListener(v -> handleAction("Publicación eliminada"));
    }

    private void loadPublicationData() {
        apiService.getPublicationDetail(publicationId).enqueue(new Callback<Publication>() {
            @Override
            public void onResponse(@NonNull Call<Publication> call, @NonNull Response<Publication> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bindData(response.body());
                } else {
                    Toast.makeText(getContext(), "No se pudo cargar la información", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Publication> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Error de red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindData(Publication pub) {
        tvTitle.setText(pub.getTitle());
        tvPrice.setText(String.format("$ %.2f", pub.getPrice()));
        tvDescription.setText(pub.getDescription());
        tvCondition.setText(pub.getCondition() != null ? pub.getCondition().toUpperCase() : "");
        tvCategory.setText(pub.getCategory() != null ? pub.getCategory().toUpperCase() : "");
        tvDate.setText("Publicado el " + pub.getCreatedAt());
        tvSellerName.setText(pub.getSellerName());
        
        // Mock de reputación para el ejemplo
        tvSellerReputation.setText("Reputación: ⭐⭐⭐⭐⭐");

        // Lógica condicional según el usuario (Vendedor vs Interesado)
        String currentUserId = sessionManager.getUserId();
        boolean isOwner = currentUserId != null && currentUserId.equals(pub.getUserId());

        if (isOwner) {
            panelInterested.setVisibility(View.GONE);
            panelSeller.setVisibility(View.VISIBLE);
        } else {
            panelInterested.setVisibility(View.VISIBLE);
            panelSeller.setVisibility(View.GONE);
        }

        // Navegación al perfil del vendedor
        getView().findViewById(R.id.btnGoToSellerProfile).setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("userId", pub.getUserId());
            Navigation.findNavController(v).navigate(R.id.action_home_to_publicProfile, args);
        });
    }

    private void handleAction(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
