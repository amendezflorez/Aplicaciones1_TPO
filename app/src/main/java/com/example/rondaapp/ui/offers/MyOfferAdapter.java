package com.example.rondaapp.ui.offers;

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
import com.example.rondaapp.data.model.MyOffer;
import com.example.rondaapp.data.model.Offer;
import com.example.rondaapp.data.model.OfferActionBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Punto 7: una fila de "Mis ofertas". Según el modo, es una oferta que mandé
 * (comprador) o que recibí (vendedor), y solo ofrece botones de acción cuando
 * le toca actuar a quien está mirando: el vendedor sobre una 'pendiente', el
 * comprador sobre la 'contraofertada' que le devolvió el vendedor.
 */
public class MyOfferAdapter extends RecyclerView.Adapter<MyOfferAdapter.MyOfferViewHolder> {

    public enum Modo { ENVIADA, RECIBIDA }

    /** La pantalla es la que llama al backend; acá solo se avisa qué se tocó. */
    public interface OnOfferActionListener {
        void onOfferAction(MyOffer oferta, String action);
    }

    /** Aviso de que se tocó la fila, para abrir el detalle de esa publicación. */
    public interface OnOfferClickListener {
        void onOfferClick(MyOffer oferta);
    }

    private final Modo modo;
    private final OnOfferActionListener listener;
    private final List<MyOffer> ofertas = new ArrayList<>();
    private OnOfferClickListener clickListener;

    public MyOfferAdapter(Modo modo, OnOfferActionListener listener) {
        this.modo = modo;
        this.listener = listener;
    }

    /** Si no se setea, la fila se muestra pero no abre el detalle. */
    public void setOnOfferClickListener(OnOfferClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setOfertas(List<MyOffer> nuevas) {
        ofertas.clear();
        if (nuevas != null) ofertas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyOfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_offer, parent, false);
        return new MyOfferViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyOfferViewHolder holder, int position) {
        MyOffer oferta = ofertas.get(position);
        holder.tvTitle.setText(oferta.getPublicationTitle() != null ? oferta.getPublicationTitle() : "");

        String contraparte = modo == Modo.ENVIADA ? oferta.getSellerName() : oferta.getBuyerName();
        int labelRes = modo == Modo.ENVIADA ? R.string.my_offer_seller_label : R.string.my_offer_buyer_label;
        holder.tvCounterpart.setText(holder.itemView.getContext()
                .getString(labelRes, contraparte != null ? contraparte : ""));

        int amountRes = modo == Modo.ENVIADA ? R.string.my_offer_amount_sent : R.string.my_offer_amount_received;
        holder.tvAmount.setText(holder.itemView.getContext().getString(amountRes,
                String.format(Locale.getDefault(), "%.2f", oferta.getAmount()),
                String.format(Locale.getDefault(), "%.2f", oferta.getPublicationPrice())));

        boolean hayMensaje = oferta.getMessage() != null && !oferta.getMessage().trim().isEmpty();
        holder.tvMessage.setVisibility(hayMensaje ? View.VISIBLE : View.GONE);
        if (hayMensaje) holder.tvMessage.setText(oferta.getMessage());

        String estado = oferta.getStatus();
        holder.tvStatus.setText(estado != null ? estado.toUpperCase(Locale.getDefault()) : "");
        holder.tvStatus.setBackgroundColor(colorDeEstado(estado));

        // El vendedor actúa sobre una pendiente; el comprador, sobre la
        // contraoferta que le devolvió el vendedor. En cualquier otro estado
        // ninguno de los dos tiene nada que tocar.
        boolean puedeActuar = (modo == Modo.RECIBIDA && Offer.PENDIENTE.equals(estado))
                || (modo == Modo.ENVIADA && Offer.CONTRAOFERTADA.equals(estado));
        holder.containerActions.setVisibility(puedeActuar ? View.VISIBLE : View.GONE);
        // Contraofertar es una acción exclusiva del vendedor.
        holder.btnCounter.setVisibility(modo == Modo.RECIBIDA ? View.VISIBLE : View.GONE);

        holder.btnAccept.setOnClickListener(puedeActuar
                ? v -> listener.onOfferAction(oferta, OfferActionBody.ACEPTAR) : null);
        holder.btnReject.setOnClickListener(puedeActuar
                ? v -> listener.onOfferAction(oferta, OfferActionBody.RECHAZAR) : null);
        holder.btnCounter.setOnClickListener(puedeActuar && modo == Modo.RECIBIDA
                ? v -> listener.onOfferAction(oferta, OfferActionBody.CONTRAOFERTAR) : null);

        holder.itemView.setOnClickListener(clickListener == null ? null
                : v -> clickListener.onOfferClick(oferta));
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

    static class MyOfferViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvCounterpart, tvAmount, tvMessage, tvStatus;
        final LinearLayout containerActions;
        final Button btnAccept, btnReject, btnCounter;

        MyOfferViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvMyOfferTitle);
            tvCounterpart = itemView.findViewById(R.id.tvMyOfferCounterpart);
            tvAmount = itemView.findViewById(R.id.tvMyOfferAmount);
            tvMessage = itemView.findViewById(R.id.tvMyOfferMessage);
            tvStatus = itemView.findViewById(R.id.tvMyOfferStatus);
            containerActions = itemView.findViewById(R.id.containerMyOfferActions);
            btnAccept = itemView.findViewById(R.id.btnMyOfferAccept);
            btnReject = itemView.findViewById(R.id.btnMyOfferReject);
            btnCounter = itemView.findViewById(R.id.btnMyOfferCounter);
        }
    }
}
