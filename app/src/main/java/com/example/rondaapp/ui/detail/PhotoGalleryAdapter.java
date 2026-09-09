package com.example.rondaapp.ui.detail;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rondaapp.R;
import com.example.rondaapp.data.local.OfflineCache;

import java.util.ArrayList;
import java.util.List;

/**
 * Galería de fotos del artículo. Las fotos llegan como data URI en base64,
 * así que se decodifican acá; el resultado queda cacheado para no repetir el
 * trabajo cada vez que la foto vuelve a entrar en pantalla.
 */
public class PhotoGalleryAdapter extends RecyclerView.Adapter<PhotoGalleryAdapter.PhotoViewHolder> {

    private final List<String> fotos = new ArrayList<>();
    private final SparseArray<Bitmap> decodificadas = new SparseArray<>();

    public void setFotos(List<String> nuevas) {
        fotos.clear();
        decodificadas.clear();
        if (nuevas != null) fotos.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery_photo, parent, false);
        // Cada foto ocupa el ancho completo: así el snap la deja centrada y el
        // contador "1 de 3" coincide con lo que se ve.
        view.getLayoutParams().width = parent.getMeasuredWidth();
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        holder.ivPhoto.setImageBitmap(bitmapDe(position));
    }

    private Bitmap bitmapDe(int position) {
        Bitmap cacheada = decodificadas.get(position);
        if (cacheada != null) return cacheada;

        byte[] bytes = OfflineCache.decodificarDataUri(fotos.get(position));
        if (bytes == null) return null;

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap != null) decodificadas.put(position, bitmap);
        return bitmap;
    }

    @Override
    public int getItemCount() {
        return fotos.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPhoto;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivGalleryPhoto);
        }
    }
}
