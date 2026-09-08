package com.example.rondaapp.ui.publish;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.Publication;
import com.example.rondaapp.data.model.PublicationResponse;
import com.example.rondaapp.data.model.PublicationStatusBody;
import com.example.rondaapp.data.network.RetrofitClient;
import com.example.rondaapp.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Punto 5: "Mis publicaciones", con el estado de cada una
 * (activa / pausada / vendida) y las acciones de pausar y reactivar.
 */
public class MyPublicationsFragment extends Fragment {

    private RecyclerView rvMyPublications;
    private MyPublicationAdapter adapter;
    private TextView tvNoMyPublications;
    private ProgressBar progressMyPublications;
    private Button btnGoPublish;

    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_publications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvMyPublications = view.findViewById(R.id.rvMyPublications);
        tvNoMyPublications = view.findViewById(R.id.tvNoMyPublications);
        progressMyPublications = view.findViewById(R.id.progressMyPublications);
        btnGoPublish = view.findViewById(R.id.btnGoPublish);

        sessionManager = new SessionManager(requireContext());

        adapter = new MyPublicationAdapter(this::cambiarEstado);
        rvMyPublications.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMyPublications.setAdapter(adapter);

        btnGoPublish.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_myPublications_to_publish));
        // La carga la hace onResume, que corre siempre después de esto y además
        // cubre la vuelta desde el formulario.
    }

    @Override
    public void onResume() {
        super.onResume();
        // Al volver de publicar, la lista tiene que mostrar el artículo nuevo.
        cargar();
    }

    private void cargar() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(requireContext(), R.string.my_publications_error, Toast.LENGTH_SHORT).show();
            return;
        }

        progressMyPublications.setVisibility(View.VISIBLE);
        RetrofitClient.getApiService().getMyPublications(userId).enqueue(new Callback<PublicationResponse>() {
            @Override
            public void onResponse(@NonNull Call<PublicationResponse> call,
                                   @NonNull Response<PublicationResponse> response) {
                if (!isAdded() || getView() == null) return;
                progressMyPublications.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(getContext(), R.string.my_publications_error, Toast.LENGTH_SHORT).show();
                    return;
                }

                adapter.setPublications(response.body().getData());
                boolean vacio = response.body().getData() == null || response.body().getData().isEmpty();
                tvNoMyPublications.setVisibility(vacio ? View.VISIBLE : View.GONE);
                rvMyPublications.setVisibility(vacio ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(@NonNull Call<PublicationResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getView() == null) return;
                progressMyPublications.setVisibility(View.GONE);
                Toast.makeText(getContext(), R.string.my_publications_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cambiarEstado(Publication publication, String nuevoEstado) {
        progressMyPublications.setVisibility(View.VISIBLE);

        RetrofitClient.getApiService()
                .updatePublicationStatus(publication.getId(), new PublicationStatusBody(nuevoEstado))
                .enqueue(new Callback<Publication>() {
                    @Override
                    public void onResponse(@NonNull Call<Publication> call, @NonNull Response<Publication> response) {
                        if (!isAdded() || getView() == null) return;

                        if (!response.isSuccessful()) {
                            progressMyPublications.setVisibility(View.GONE);
                            Toast.makeText(getContext(), R.string.my_publications_error_status,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Toast.makeText(getContext(),
                                PublicationStatusBody.PAUSADA.equals(nuevoEstado)
                                        ? R.string.my_publication_paused
                                        : R.string.my_publication_reactivated,
                                Toast.LENGTH_SHORT).show();
                        // Se recarga en vez de mutar en memoria, así la lista queda
                        // consistente con lo que realmente quedó en el backend.
                        cargar();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Publication> call, @NonNull Throwable t) {
                        if (!isAdded() || getView() == null) return;
                        progressMyPublications.setVisibility(View.GONE);
                        Toast.makeText(getContext(), R.string.my_publications_error_status,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
