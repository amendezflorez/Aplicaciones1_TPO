package com.example.rondaapp.ui.detail;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.Offer;
import com.example.rondaapp.data.model.OfferActionBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Punto 7: ofertas recibidas sobre esta publicación, con las acciones del
 * vendedor (aceptar / rechazar / contraofertar) mientras siguen 'pendiente'.
 * En cualquier otro estado se muestra el resultado, sin botones: si está
 * 'contraofertada' la pelota ya es del comprador, que responde desde
 * "Mis ofertas".
 */
public class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.OfferViewHolder> {

    /** La pantalla es la que llama al backend; acá solo se avisa qué se tocó. */
    public interface OnOfferActionListener {
        void onOfferAction(Offer oferta, String action);
    }

    private final List<Offer> ofertas = new ArrayList<>();
    private final OnOfferActionListener listener;

    public OfferAdapter(OnOfferActionListener listener) {
        this.listener = listener;
    }

    public void setOfertas(List<Offer> nuevas) {
        ofertas.clear();
        if (nuevas != null) ofertas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_offer, parent, false);
        return new OfferViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfferViewHolder holder, int position) {
        Offer oferta = ofertas.get(position);
        holder.tvBuyer.setText(oferta.getUserName() != null ? oferta.getUserName() : "");
        holder.tvAmount.setText(String.format(Locale.getDefault(), "$ %.2f", oferta.getAmount()));

        boolean hayMensaje = oferta.getMessage() != null && !oferta.getMessage().trim().isEmpty();
        holder.tvMessage.setVisibility(hayMensaje ? View.VISIBLE : View.GONE);
        if (hayMensaje) holder.tvMessage.setText(oferta.getMessage());

        String estado = oferta.getStatus();
        holder.tvStatus.setText(estado != null ? estado.toUpperCase(Locale.getDefault()) : "");
        holder.tvStatus.setBackgroundColor(colorDeEstado(estado));

        boolean pendiente = Offer.PENDIENTE.equals(estado);
        holder.containerActions.setVisibility(pendiente ? View.VISIBLE : View.GONE);

        holder.btnAccept.setOnClickListener(pendiente
                ? v -> listener.onOfferAction(oferta, OfferActionBody.ACEPTAR) : null);
        holder.btnReject.setOnClickListener(pendiente
                ? v -> listener.onOfferAction(oferta, OfferActionBody.RECHAZAR) : null);
        holder.btnCounter.setOnClickListener(pendiente
                ? v -> listener.onOfferAction(oferta, OfferActionBody.CONTRAOFERTAR) : null);
    }

    private int colorDeEstado(String estado) {
        if (estado == null) return Color.parseColor("#BBDEFB");
        switch (estado) {
            case Offer.ACEPTADA: return Color.parseColor("#C8E6C9");
            case Offer.RECHAZADA: return Color.parseColor("#FFCDD2");
            case Offer.VENCIDA: return Color.parseColor("#CFD8DC");
            case Offer.CONTRAOFERTADA: return Color.parseColor("#FFE0B2");
            default: return Color.parseColor("#BBDEFB");
        }
    }

    @Override
    public int getItemCount() {
        return ofertas.size();
    }

    static class OfferViewHolder extends RecyclerView.ViewHolder {
        final TextView tvBuyer, tvAmount, tvMessage, tvStatus;
        final LinearLayout containerActions;
        final Button btnAccept, btnReject, btnCounter;

        OfferViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBuyer = itemView.findViewById(R.id.tvOfferBuyer);
            tvAmount = itemView.findViewById(R.id.tvOfferAmount);
            tvMessage = itemView.findViewById(R.id.tvOfferMessage);
            tvStatus = itemView.findViewById(R.id.tvOfferStatus);
            containerActions = itemView.findViewById(R.id.containerOfferActions);
            btnAccept = itemView.findViewById(R.id.btnOfferAccept);
            btnReject = itemView.findViewById(R.id.btnOfferReject);
            btnCounter = itemView.findViewById(R.id.btnOfferCounter);
        }
    }
}
