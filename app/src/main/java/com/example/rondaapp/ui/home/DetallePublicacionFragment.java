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
import com.example.rondaapp.data.network.RetrofitClient;
import com.example.rondaapp.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetallePublicacionFragment extends Fragment {

    private int publicationId;
    private SessionManager sessionManager;
    private TextView tvTitle, tvPrice, tvDescription, tvSellerName, tvReputation, tvCategory, tvState, tvDate;
    private LinearLayout layoutInteresado, layoutVendedor;
    private ImageView ivImage;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            publicationId = getArguments().getInt("publicationId");
        }
        sessionManager = new SessionManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detalle_publicacion, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        tvTitle = view.findViewById(R.id.tvPublicationTitle);
        tvPrice = view.findViewById(R.id.tvPublicationPrice);
        tvDescription = view.findViewById(R.id.tvPublicationDescription);
        tvSellerName = view.findViewById(R.id.tvSellerName);
        tvReputation = view.findViewById(R.id.tvSellerReputation);
        tvCategory = view.findViewById(R.id.tvPublicationCategory);
        tvState = view.findViewById(R.id.tvPublicationState);
        tvDate = view.findViewById(R.id.tvPublicationDate);
        ivImage = view.findViewById(R.id.ivPublicationImage);
        layoutInteresado = view.findViewById(R.id.layoutInteresadoActions);
        layoutVendedor = view.findViewById(R.id.layoutVendedorActions);
        Button btnViewProfile = view.findViewById(R.id.btnViewSellerProfile);

        fetchPublicationDetails();

        // Acciones
        view.findViewById(R.id.btnOfert).setOnClickListener(v -> Toast.makeText(getContext(), "Oferta realizada", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnAsk).setOnClickListener(v -> Toast.makeText(getContext(), "Pregunta enviada", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnSave).setOnClickListener(v -> Toast.makeText(getContext(), "Guardado en favoritos", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnManage).setOnClickListener(v -> Toast.makeText(getContext(), "Gestión de publicación", Toast.LENGTH_SHORT).show());
    }

    private void fetchPublicationDetails() {
        RetrofitClient.getApiService().getPublicationById(publicationId).enqueue(new Callback<Publication>() {
            @Override
            public void onResponse(Call<Publication> call, Response<Publication> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bindData(response.body());
                } else {
                    Toast.makeText(getContext(), "Error al cargar detalle", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Publication> call, Throwable t) {
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindData(Publication pub) {
        tvTitle.setText(pub.getTitle());
        tvPrice.setText("$ " + pub.getPrice());
        tvDescription.setText(pub.getDescription());
        tvSellerName.setText(pub.getSellerName());
        tvCategory.setText(pub.getCategory());
        tvState.setText(pub.getCondition());
        tvDate.setText("Publicado el " + pub.getCreatedAt());
        
        // Simulación de reputación (debería venir del profile del seller)
        tvReputation.setText("Reputación: ⭐⭐⭐⭐ (4.5)");

        // Lógica de visibilidad según el rol (Feature 4)
        String currentUserId = sessionManager.getUserId();
        boolean isVendedor = currentUserId != null && currentUserId.equals(pub.getUserId());
        
        if (isVendedor) {
            layoutInteresado.setVisibility(View.GONE);
            layoutVendedor.setVisibility(View.VISIBLE);
        } else {
            layoutInteresado.setVisibility(View.VISIBLE);
            layoutVendedor.setVisibility(View.GONE);
        }

        getView().findViewById(R.id.btnViewSellerProfile).setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("userId", pub.getUserId());
            Navigation.findNavController(v).navigate(R.id.action_home_to_publicProfile, bundle);
        });
    }
}
