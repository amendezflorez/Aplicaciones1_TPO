package com.example.rondaapp.ui.favorites;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.SavedSearch;

import java.util.ArrayList;
import java.util.List;

public class SavedSearchAdapter extends RecyclerView.Adapter<SavedSearchAdapter.SavedSearchViewHolder> {

    private List<SavedSearch> savedSearches = new ArrayList<>();
    private OnDeleteListener deleteListener;
    private OnExecuteListener executeListener;

    public void setSavedSearches(List<SavedSearch> searches) {
        this.savedSearches = (searches != null) ? searches : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnExecuteListener(OnExecuteListener listener) {
        this.executeListener = listener;
    }

    @NonNull
    @Override
    public SavedSearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_search, parent, false);
        return new SavedSearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedSearchViewHolder holder, int position) {
        SavedSearch search = savedSearches.get(position);

        holder.tvSearchTerm.setText(search.getSearchTerm() != null ? search.getSearchTerm() : "Sin término");

        StringBuilder filters = new StringBuilder();
        if (search.getCategory() != null) {
            filters.append("Categoría: ").append(search.getCategory()).append(" • ");
        }
        if (search.getMinPrice() != null || search.getMaxPrice() != null) {
            filters.append("Precio: ");
            if (search.getMinPrice() != null) {
                filters.append("$").append(search.getMinPrice());
            }
            filters.append(" - ");
            if (search.getMaxPrice() != null) {
                filters.append("$").append(search.getMaxPrice());
            }
            filters.append(" • ");
        }
        if (search.getZone() != null) {
            filters.append("Zona: ").append(search.getZone()).append(" • ");
        }

        if (filters.length() > 0) {
            // Remover el último " • "
            filters.setLength(filters.length() - 3);
            holder.tvFilters.setText(filters.toString());
            holder.tvFilters.setVisibility(View.VISIBLE);
        } else {
            holder.tvFilters.setVisibility(View.GONE);
        }

        holder.btnExecute.setOnClickListener(v -> {
            if (executeListener != null) {
                executeListener.onExecute(search);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(search.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return savedSearches.size();
    }

    static class SavedSearchViewHolder extends RecyclerView.ViewHolder {
        TextView tvSearchTerm, tvFilters;
        Button btnExecute;
        ImageButton btnDelete;

        public SavedSearchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSearchTerm = itemView.findViewById(R.id.tvSearchTerm);
            tvFilters = itemView.findViewById(R.id.tvFilters);
            btnExecute = itemView.findViewById(R.id.btnExecute);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    public interface OnDeleteListener {
        void onDelete(int searchId);
    }

    public interface OnExecuteListener {
        void onExecute(SavedSearch search);
    }
}
