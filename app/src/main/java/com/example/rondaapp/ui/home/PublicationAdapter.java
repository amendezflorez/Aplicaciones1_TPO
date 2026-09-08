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

    private List<Publication> publications = new ArrayList<>();

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
    }

    @Override
    public int getItemCount() {
        return publications.size();
    }

    static class PublicationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPrice, tvCondition, tvZone;

        public PublicationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCondition = itemView.findViewById(R.id.tvCondition);
            tvZone = itemView.findViewById(R.id.tvZone);
        }
    }
}