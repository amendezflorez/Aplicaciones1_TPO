package com.example.rondaapp.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rondaapp.R;
import com.example.rondaapp.data.model.Publication;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PublicationAdapter extends RecyclerView.Adapter<PublicationAdapter.PublicationViewHolder> {

    private List<Publication> publications = new ArrayList<>();
    private Set<Integer> favoriteIds = new HashSet<>();
    private OnPublicationActionListener actionListener;

    public void setPublications(List<Publication> publications) {
        this.publications = (publications != null) ? publications : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setActionListener(OnPublicationActionListener listener) {
        this.actionListener = listener;
    }

    public void updateFavorites(Set<Integer> favorites) {
        this.favoriteIds = new HashSet<>(favorites);
        notifyDataSetChanged();
    }

    public void addFavorite(int publicationId) {
        favoriteIds.add(publicationId);
        notifyDataSetChanged();
    }

    public void removeFavorite(int publicationId) {
        favoriteIds.remove(publicationId);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PublicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_publication, parent, false);
        return new PublicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PublicationViewHolder holder, int position) {
        Publication pub = publications.get(position);
        holder.tvTitle.setText(pub.getTitle() != null ? pub.getTitle() : "");
        holder.tvPrice.setText(String.format(Locale.getDefault(), "$ %.2f", pub.getPrice()));

        if (pub.getCondition() != null && !pub.getCondition().isEmpty()) {
            holder.tvCondition.setText(pub.getCondition().toUpperCase(Locale.getDefault()));
            holder.tvCondition.setVisibility(View.VISIBLE);
        } else {
            holder.tvCondition.setVisibility(View.GONE);
        }

        if (pub.getZone() != null && !pub.getZone().isEmpty()) {
            holder.tvZone.setText(String.format("📍 %s", pub.getZone()));
            holder.tvZone.setVisibility(View.VISIBLE);
        } else {
            holder.tvZone.setVisibility(View.GONE);
        }

        // Actualizar estado del botón de favorito
        boolean isFavorite = favoriteIds.contains(pub.getId());
        holder.btnFavorite.setSelected(isFavorite);
        holder.btnFavorite.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onFavoriteClicked(pub, !isFavorite);
            }
        });

        // Listener para click en el item (ir a detalle)
        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onPublicationClicked(pub);
            }
        });
    }

    @Override
    public int getItemCount() {
        return publications.size();
    }

    static class PublicationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPrice, tvCondition, tvZone;
        ImageButton btnFavorite;

        public PublicationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCondition = itemView.findViewById(R.id.tvCondition);
            tvZone = itemView.findViewById(R.id.tvZone);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }

    // Interface para manejar acciones en las publicaciones
    public interface OnPublicationActionListener {
        void onPublicationClicked(Publication publication);
        void onFavoriteClicked(Publication publication, boolean isFavorite);
    }
}