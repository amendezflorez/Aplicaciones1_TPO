package com.example.rondaapp.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    public interface OnPublicationClickListener {
        void onPublicationClick(Publication publication);
    }

    private List<Publication> publications = new ArrayList<>();
    private OnSellerClickListener sellerClickListener;
    private OnPublicationClickListener publicationClickListener;

    /**
     * Si no se setea, el nombre del vendedor se muestra pero no es clickeable.
     * El perfil público usa el adapter así, para no navegar al mismo perfil.
     */
    public void setOnSellerClickListener(OnSellerClickListener listener) {
        this.sellerClickListener = listener;
    }

    public void setOnPublicationClickListener(OnPublicationClickListener listener) {
        this.publicationClickListener = listener;
    }

    /** Reemplaza la lista completa. Se usa al cargar la primera pagina. */
    public void setPublications(List<Publication> publications) {
        this.publications = (publications != null) ? new ArrayList<>(publications) : new ArrayList<>();
        notifyDataSetChanged();
    }

    /** Anexa una pagina al final de la lista. Se usa en el scroll infinito. */
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

        holder.itemView.setOnClickListener(v -> {
            if (publicationClickListener != null) {
                publicationClickListener.onPublicationClick(pub);
            }
        });

        bindVendedor(holder, pub);
    }

    /**
     * Muestra el vendedor y, si hay listener y la publicación tiene dueño,
     * lo deja clickeable para abrir su perfil público.
     */
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

        public PublicationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCondition = itemView.findViewById(R.id.tvCondition);
            tvZone = itemView.findViewById(R.id.tvZone);
            tvSeller = itemView.findViewById(R.id.tvSeller);
        }
    }
}