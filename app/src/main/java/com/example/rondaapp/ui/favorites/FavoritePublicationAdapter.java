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

        // El botón de eliminar no depende de que haya llegado la publicación
        // anidada: si el backend algún día manda menos datos, igual se puede
        // sacar de favoritos.
        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemoveFavorite(favorite.getId());
            }
        });

        if (favorite.getPublication() == null) {
            holder.tvTitle.setText("");
            holder.tvPrice.setText("");
            holder.tvCondition.setVisibility(View.GONE);
            holder.tvZone.setVisibility(View.GONE);
            holder.tvPriceChange.setVisibility(View.GONE);
            return;
        }

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

        // Punto 11: indicador de novedad — compara el precio de cuando se
        // guardó el favorito contra el precio actual de la publicación.
        double precioActual = favorite.getPublication().getPrice();
        double precioGuardado = favorite.getSavedPrice();

        if (precioActual < precioGuardado) {
            holder.tvPriceChange.setText(holder.itemView.getContext()
                    .getString(R.string.price_dropped, precioGuardado, precioActual));
            holder.tvPriceChange.setTextColor(0xFF2E7D32); // verde
            holder.tvPriceChange.setVisibility(View.VISIBLE);
        } else if (precioActual > precioGuardado) {
            holder.tvPriceChange.setText(holder.itemView.getContext()
                    .getString(R.string.price_increased, precioGuardado, precioActual));
            holder.tvPriceChange.setTextColor(0xFFD32F2F); // rojo
            holder.tvPriceChange.setVisibility(View.VISIBLE);
        } else {
            holder.tvPriceChange.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPrice, tvCondition, tvZone, tvPriceChange;
        ImageButton btnRemove;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCondition = itemView.findViewById(R.id.tvCondition);
            tvZone = itemView.findViewById(R.id.tvZone);
            tvPriceChange = itemView.findViewById(R.id.tvPriceChange);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }

    public interface OnRemoveFavoriteListener {
        void onRemoveFavorite(int favoriteId);
    }
}