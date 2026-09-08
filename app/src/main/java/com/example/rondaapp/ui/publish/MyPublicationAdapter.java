package com.example.rondaapp.ui.publish;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.Publication;
import com.example.rondaapp.data.model.PublicationStatusBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lista de "Mis publicaciones" con el estado de cada una y la acción de
 * pausar / reactivar (punto 5).
 */
public class MyPublicationAdapter extends RecyclerView.Adapter<MyPublicationAdapter.MyPublicationViewHolder> {

    /** Pedido de cambio de estado; la pantalla es la que llama al backend. */
    public interface OnToggleStatusListener {
        void onToggleStatus(Publication publication, String nuevoEstado);
    }

    private List<Publication> publications = new ArrayList<>();
    private final OnToggleStatusListener listener;

    public MyPublicationAdapter(OnToggleStatusListener listener) {
        this.listener = listener;
    }

    public void setPublications(List<Publication> publications) {
        this.publications = (publications != null) ? new ArrayList<>(publications) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyPublicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_publication, parent, false);
        return new MyPublicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyPublicationViewHolder holder, int position) {
        Publication pub = publications.get(position);

        holder.tvTitle.setText(pub.getTitle() != null ? pub.getTitle() : "");
        holder.tvPrice.setText(String.format(Locale.getDefault(), "$ %.2f", pub.getPrice()));

        String estado = pub.getStatus() != null ? pub.getStatus() : PublicationStatusBody.ACTIVA;
        holder.tvStatus.setText(estado.toUpperCase(Locale.getDefault()));
        holder.tvStatus.setBackgroundColor(colorDeEstado(estado));

        int fotos = pub.getPhotoCount();
        holder.tvPhotos.setText(holder.itemView.getContext()
                .getResources().getQuantityString(R.plurals.my_publication_photos, fotos, fotos));

        // Una publicación vendida es un estado final: no se pausa ni se reactiva.
        boolean vendida = PublicationStatusBody.VENDIDA.equals(estado);
        holder.btnToggle.setVisibility(vendida ? View.GONE : View.VISIBLE);

        boolean pausada = PublicationStatusBody.PAUSADA.equals(estado);
        holder.btnToggle.setText(pausada
                ? R.string.my_publication_reactivate
                : R.string.my_publication_pause);
        holder.btnToggle.setOnClickListener(v -> listener.onToggleStatus(pub,
                pausada ? PublicationStatusBody.ACTIVA : PublicationStatusBody.PAUSADA));
    }

    private int colorDeEstado(String estado) {
        switch (estado) {
            case PublicationStatusBody.PAUSADA: return Color.parseColor("#FFE0B2");
            case PublicationStatusBody.VENDIDA: return Color.parseColor("#CFD8DC");
            default: return Color.parseColor("#C8E6C9");
        }
    }

    @Override
    public int getItemCount() {
        return publications.size();
    }

    static class MyPublicationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPrice, tvStatus, tvPhotos;
        Button btnToggle;

        MyPublicationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvMyTitle);
            tvPrice = itemView.findViewById(R.id.tvMyPrice);
            tvStatus = itemView.findViewById(R.id.tvMyStatus);
            tvPhotos = itemView.findViewById(R.id.tvMyPhotos);
            btnToggle = itemView.findViewById(R.id.btnToggleStatus);
        }
    }
}
