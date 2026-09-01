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

    public void setPublications(List<Publication> publications) {
        this.publications = publications;
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
        holder.tvTitle.setText(pub.getTitle());
        holder.tvPrice.setText(String.format(Locale.getDefault(), "$ %.2f", pub.getPrice()));
        holder.tvCondition.setText(pub.getCondition().toUpperCase(Locale.getDefault()));
        holder.tvZone.setText(String.format("📍 %s", pub.getZone()));
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