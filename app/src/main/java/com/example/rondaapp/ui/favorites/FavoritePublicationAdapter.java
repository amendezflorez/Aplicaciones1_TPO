package com.example.rondaapp.ui.favorites;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.Favorite;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FavoritePublicationAdapter extends RecyclerView.Adapter<FavoritePublicationAdapter.FavoriteViewHolder> {

    private List<Favorite> favorites = new ArrayList<>();
    private OnRemoveFavoriteListener removeListener;

    public void setFavorites(List<Favorite> favorites) {
        this.favorites = (favorites != null) ? favorites : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnRemoveFavoriteListener(OnRemoveFavoriteListener listener) {
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_publication, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Favorite favorite = favorites.get(position);
        if (favorite.getPublication() != null) {
            holder.tvTitle.setText(favorite.getPublication().getTitle());
            holder.tvPrice.setText(String.format(Locale.getDefault(), "$ %.2f", favorite.getPublication().getPrice()));

            if (favorite.getPublication().getCondition() != null && !favorite.getPublication().getCondition().isEmpty()) {
                holder.tvCondition.setText(favorite.getPublication().getCondition().toUpperCase(Locale.getDefault()));
                holder.tvCondition.setVisibility(View.VISIBLE);
            } else {
                holder.tvCondition.setVisibility(View.GONE);
            }

            if (favorite.getPublication().getZone() != null && !favorite.getPublication().getZone().isEmpty()) {
                holder.tvZone.setText(String.format("📍 %s", favorite.getPublication().getZone()));
                holder.tvZone.setVisibility(View.VISIBLE);
            } else {
                holder.tvZone.setVisibility(View.GONE);
            }

            holder.btnRemove.setOnClickListener(v -> {
                if (removeListener != null) {
                    removeListener.onRemoveFavorite(favorite.getId());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPrice, tvCondition, tvZone;
        ImageButton btnRemove;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCondition = itemView.findViewById(R.id.tvCondition);
            tvZone = itemView.findViewById(R.id.tvZone);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }

    public interface OnRemoveFavoriteListener {
        void onRemoveFavorite(int favoriteId);
    }
}
