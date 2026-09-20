package com.example.rondaapp.ui.offers;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rondaapp.R;
import com.example.rondaapp.data.local.ConnectivityWatcher;
import com.example.rondaapp.data.model.MyOffer;
import com.example.rondaapp.data.model.MyOffersResponse;
import com.example.rondaapp.data.model.Offer;
import com.example.rondaapp.data.model.OfferActionBody;
import com.example.rondaapp.data.network.ApiService;
import com.example.rondaapp.ui.detail.PublicationDetailFragment;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Punto 7: "Mis ofertas", las que mandé como comprador y las que recibí como
 * vendedor, en un mismo lugar y siempre actualizadas. La caducidad es lazy:
 * el backend la recalcula en cada lectura, así que alcanza con recargar.
 */
@AndroidEntryPoint
public class MyOffersFragment extends Fragment {

    @Inject
    ApiService apiService;

    private ConnectivityWatcher connectivityWatcher;

    private ProgressBar progressMyOffers;
    private RecyclerView rvOffersSent, rvOffersReceived;
    private TextView tvNoOffersSent, tvNoOffersReceived;
    private MyOfferAdapter sentAdapter, receivedAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_offers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        connectivityWatcher = new ConnectivityWatcher(requireContext());

        progressMyOffers = view.findViewById(R.id.progressMyOffers);
        rvOffersSent = view.findViewById(R.id.rvOffersSent);
        rvOffersReceived = view.findViewById(R.id.rvOffersReceived);
        tvNoOffersSent = view.findViewById(R.id.tvNoOffersSent);
        tvNoOffersReceived = view.findViewById(R.id.tvNoOffersReceived);

        sentAdapter = new MyOfferAdapter(MyOfferAdapter.Modo.ENVIADA, this::manejarAccionOferta);
        receivedAdapter = new MyOfferAdapter(MyOfferAdapter.Modo.RECIBIDA, this::manejarAccionOferta);
        sentAdapter.setOnOfferClickListener(this::abrirDetalle);
        receivedAdapter.setOnOfferClickListener(this::abrirDetalle);

        rvOffersSent.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOffersSent.setAdapter(sentAdapter);
        rvOffersReceived.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOffersReceived.setAdapter(receivedAdapter);
        // La carga la hace onResume, que corre siempre después de esto y además
        // cubre la vuelta desde el detalle de una publicación.
    }

    @Override
    public void onResume() {
        super.onResume();
        cargar();
    }

    private void cargar() {
        progressMyOffers.setVisibility(View.VISIBLE);
        apiService.getMyOffers().enqueue(new Callback<MyOffersResponse>() {
            @Override
            public void onResponse(@NonNull Call<MyOffersResponse> call, @NonNull Response<MyOffersResponse> response) {
                if (!isAdded() || getView() == null) return;
                progressMyOffers.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(getContext(), R.string.my_offers_error, Toast.LENGTH_SHORT).show();
                    return;
                }

                mostrar(response.body().getEnviadas(), response.body().getRecibidas());
            }

            @Override
            public void onFailure(@NonNull Call<MyOffersResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getView() == null) return;
                progressMyOffers.setVisibility(View.GONE);
                Toast.makeText(getContext(), R.string.my_offers_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrar(List<MyOffer> enviadas, List<MyOffer> recibidas) {
        boolean vaciaEnviadas = enviadas == null || enviadas.isEmpty();
        sentAdapter.setOfertas(enviadas);
        tvNoOffersSent.setVisibility(vaciaEnviadas ? View.VISIBLE : View.GONE);
        rvOffersSent.setVisibility(vaciaEnviadas ? View.GONE : View.VISIBLE);

        boolean vaciaRecibidas = recibidas == null || recibidas.isEmpty();
        receivedAdapter.setOfertas(recibidas);
        tvNoOffersReceived.setVisibility(vaciaRecibidas ? View.VISIBLE : View.GONE);
        rvOffersReceived.setVisibility(vaciaRecibidas ? View.GONE : View.VISIBLE);
    }

    private void abrirDetalle(MyOffer oferta) {
        Bundle args = new Bundle();
        args.putInt(PublicationDetailFragment.ARG_PUBLICATION_ID, oferta.getPublicationId());
        Navigation.findNavController(requireView()).navigate(R.id.action_myOffers_to_detail, args);
    }

    /** Contraofertar necesita un monto nuevo antes de poder llamar al backend. */
    private void manejarAccionOferta(MyOffer oferta, String action) {
        if (OfferActionBody.CONTRAOFERTAR.equals(action)) {
            pedirMonto(oferta, action);
            return;
        }
        enviarAccionOferta(oferta, action, null);
    }

    private void pedirMonto(MyOffer oferta, String action) {
        View vista = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_text_input, null, false);
        EditText input = vista.findViewById(R.id.etDialogInput);
        input.setHint(R.string.offer_counter_hint);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.offer_counter_title)
                .setView(vista)
                .setPositiveButton(R.string.detail_send, (dialog, which) -> {
                    double monto;
                    try {
                        monto = Double.parseDouble(input.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), R.string.detail_offer_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (monto <= 0) {
                        Toast.makeText(getContext(), R.string.detail_offer_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    enviarAccionOferta(oferta, action, monto);
                })
                .setNegativeButton(R.string.detail_cancel, null)
                .show();
    }

    private void enviarAccionOferta(MyOffer oferta, String action, Double amount) {
        if (!exigirConexion()) return;

        apiService.updateOfferStatus(oferta.getPublicationId(), oferta.getId(), new OfferActionBody(action, amount))
                .enqueue(new Callback<Offer>() {
                    @Override
                    public void onResponse(@NonNull Call<Offer> call, @NonNull Response<Offer> response) {
                        if (!isAdded() || getView() == null) return;
                        if (!response.isSuccessful()) {
                            Toast.makeText(getContext(), R.string.offer_action_error, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Toast.makeText(getContext(), R.string.offer_action_success, Toast.LENGTH_SHORT).show();
                        cargar();
                    }

                    @Override
                    public void onFailure(@NonNull Call<Offer> call, @NonNull Throwable t) {
                        if (!isAdded() || getView() == null) return;
                        Toast.makeText(getContext(), R.string.offer_action_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean exigirConexion() {
        if (connectivityWatcher.hayConexion()) return true;
        Toast.makeText(requireContext(), R.string.offline_action_needs_connection, Toast.LENGTH_SHORT).show();
        return false;
    }
}
