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
import java.util.List;
import java.util.Locale;

public class PublicationAdapter extends RecyclerView.Adapter<PublicationAdapter.PublicationViewHolder> {

    /** Aviso de que se tocó el vendedor de una publicación, para abrir su perfil público. */
    public interface OnSellerClickListener {
        void onSellerClick(Publication publication);
    }

    /** Aviso de que se tocó la publicación entera, para abrir su detalle (punto 4). */
    public interface OnPublicationClickListener {
        void onPublicationClick(Publication publication);
    }

    /** Listener para acciones con favoritos (Punto 11). */
    public interface OnPublicationActionListener {
        void onFavoriteClicked(Publication publication, boolean isFavorite);
    }

    private List<Publication> publications = new ArrayList<>();
    private OnSellerClickListener sellerClickListener;
    private OnPublicationClickListener publicationClickListener;
    private OnPublicationActionListener actionListener;

    public void setOnSellerClickListener(OnSellerClickListener listener) {
        this.sellerClickListener = listener;
    }

    public void setOnPublicationClickListener(OnPublicationClickListener listener) {
        this.publicationClickListener = listener;
    }

    public void setActionListener(OnPublicationActionListener listener) {
        this.actionListener = listener;
    }

    public void setPublications(List<Publication> publications) {
        this.publications = (publications != null) ? new ArrayList<>(publications) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addPublications(List<Publication> nuevas) {
        if (nuevas == null || nuevas.isEmpty()) return;
        int desde = publications.size();
        publications.addAll(nuevas);
        notifyItemRangeInserted(desde, nuevas.size());
    }

    public int getItemCountLoaded() {
        return publications.size();
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

        holder.itemView.setOnClickListener(publicationClickListener == null ? null
                : v -> publicationClickListener.onPublicationClick(pub));

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

        bindVendedor(holder, pub);

        // Botón de favorito (Punto 11)
        holder.btnFavorite.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onFavoriteClicked(pub, true);
            }
        });
    }

    private void bindVendedor(PublicationViewHolder holder, Publication pub) {
        boolean hayVendedor = pub.getSellerName() != null && !pub.getSellerName().isEmpty();
        if (!hayVendedor) {
            holder.tvSeller.setVisibility(View.GONE);
            holder.tvSeller.setOnClickListener(null);
            return;
        }

        holder.tvSeller.setVisibility(View.VISIBLE);
        holder.tvSeller.setText(holder.itemView.getContext()
                .getString(R.string.publication_seller, pub.getSellerName()));

        boolean navegable = sellerClickListener != null && pub.getUserId() != null;
        holder.tvSeller.setClickable(navegable);
        holder.tvSeller.setOnClickListener(navegable
                ? v -> sellerClickListener.onSellerClick(pub)
                : null);
    }

    @Override
    public int getItemCount() {
        return publications.size();
    }

    static class PublicationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPrice, tvCondition, tvZone, tvSeller;
        ImageButton btnFavorite;

        public PublicationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCondition = itemView.findViewById(R.id.tvCondition);
            tvZone = itemView.findViewById(R.id.tvZone);
            tvSeller = itemView.findViewById(R.id.tvSeller);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}