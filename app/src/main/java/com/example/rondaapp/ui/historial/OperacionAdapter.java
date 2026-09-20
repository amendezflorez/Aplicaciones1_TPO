package com.example.rondaapp.ui.historial;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rondaapp.R;
import com.example.rondaapp.data.model.OperacionDto;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OperacionAdapter extends RecyclerView.Adapter<OperacionAdapter.ViewHolder> {

    private List<OperacionDto> items = new ArrayList<>();
    private OnOperacionActionListener listener;

    public interface OnOperacionActionListener {
        void onCalificarClick(OperacionDto operacion);
        void onContraparteClick(String userId);
    }

    public OperacionAdapter(OnOperacionActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<OperacionDto> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_operacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OperacionDto item = items.get(position);
        holder.tvArticulo.setText(item.articuloNombre);
        holder.tvMonto.setText(String.format(Locale.getDefault(), "$ %.2f", item.montoFinal));
        
        String detalle = String.format("%s: %s | Fecha: %s", 
            item.tipo.equals("COMPRA") ? "Vendedor" : "Comprador",
            item.contraparteNombre,
            item.fecha);
        holder.tvDetalle.setText(detalle);

        holder.tvDetalle.setOnClickListener(v -> {
            if (item.contraparteId != null) listener.onContraparteClick(item.contraparteId);
        });

        if (puedeCalificar(item)) {
            holder.btnCalificar.setVisibility(View.VISIBLE);
            holder.btnCalificar.setOnClickListener(v -> listener.onCalificarClick(item));
        } else {
            holder.btnCalificar.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private boolean puedeCalificar(OperacionDto op) {
        if (op.calificada || op.fechaEntrega == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date entrega = sdf.parse(op.fechaEntrega);
            if (entrega == null) return false;
            long diff = System.currentTimeMillis() - entrega.getTime();
            long dias = diff / (24 * 60 * 60 * 1000);
            return dias >= 0 && dias <= 7;
        } catch (Exception e) {
            return false;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvArticulo, tvDetalle, tvMonto;
        Button btnCalificar;

        ViewHolder(View itemView) {
            super(itemView);
            tvArticulo = itemView.findViewById(R.id.tvArticulo);
            tvDetalle = itemView.findViewById(R.id.tvDetalle);
            tvMonto = itemView.findViewById(R.id.tvMonto);
            btnCalificar = itemView.findViewById(R.id.btnCalificar);
        }
    }
}
