package com.example.rondaapp.ui.historial;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.rondaapp.R;
import com.example.rondaapp.data.model.RatingRequestDto;
import com.example.rondaapp.data.network.ApiService;
import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import javax.inject.Inject;

@AndroidEntryPoint
public class CalificarDialogFragment extends DialogFragment {

    @Inject ApiService apiService;
    private int operacionId;

    @Override
    public void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved);
        if (getArguments() != null) {
            operacionId = getArguments().getInt("operacionId");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle saved) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_calificar, null);
        RatingBar rb = v.findViewById(R.id.ratingBar);
        EditText et = v.findViewById(R.id.etComentario);

        return new AlertDialog.Builder(requireContext())
                .setTitle("Calificar Operación")
                .setView(v)
                .setPositiveButton("Enviar", (d, w) -> {
                    enviarCalificacion((int) rb.getRating(), et.getText().toString());
                })
                .setNegativeButton("Cancelar", null)
                .create();
    }

    private void enviarCalificacion(int estrellas, String comentario) {
        apiService.calificarOperacion(operacionId, new RatingRequestDto(estrellas, comentario))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Calificación enviada", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Error al enviar", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(getContext(), "Falla de red", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
