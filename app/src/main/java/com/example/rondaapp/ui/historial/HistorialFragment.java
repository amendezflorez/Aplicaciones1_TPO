package com.example.rondaapp.ui.historial;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rondaapp.R;
import com.example.rondaapp.data.model.OperacionDto;
import com.example.rondaapp.data.network.ApiService;
import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.Calendar;
import java.util.List;
import javax.inject.Inject;

@AndroidEntryPoint
public class HistorialFragment extends Fragment {

    @Inject ApiService apiService;
    private OperacionAdapter adapter;
    private String currentTipo = "TODOS";
    private String fechaInicio = null;
    private String fechaFin = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saved) {
        View view = inflater.inflate(R.layout.fragment_historial, container, false);

        RecyclerView rv = view.findViewById(R.id.rvOperaciones);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OperacionAdapter(this::onCalificarClick);
        rv.setAdapter(adapter);

        setupFilters(view);
        cargarDatos();

        return view;
    }

    private void setupFilters(View view) {
        Spinner spinner = view.findViewById(R.id.spinnerTipo);
        String[] tipos = {"TODOS", "COMPRA", "VENTA"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, tipos);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        Button btnInicio = view.findViewById(R.id.btnFechaInicio);
        Button btnFin = view.findViewById(R.id.btnFechaFin);

        btnInicio.setOnClickListener(v -> showDatePicker(date -> {
            fechaInicio = date;
            btnInicio.setText(date);
            cargarDatos();
        }));

        btnFin.setOnClickListener(v -> showDatePicker(date -> {
            fechaFin = date;
            btnFin.setText(date);
            cargarDatos();
        }));
    }

    private void showDatePicker(OnDateSelectedListener listener) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            String date = String.format(java.util.Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
            listener.onDateSelected(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void cargarDatos() {
        apiService.getOperaciones(currentTipo, fechaInicio, fechaFin).enqueue(new Callback<List<OperacionDto>>() {
            @Override
            public void onResponse(Call<List<OperacionDto>> call, Response<List<OperacionDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setItems(response.body());
                } else {
                    Toast.makeText(getContext(), "Error al cargar historial", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<OperacionDto>> call, Throwable t) {
                Toast.makeText(getContext(), "Falla de red", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onCalificarClick(OperacionDto operacion) {
        Bundle bundle = new Bundle();
        bundle.putInt("operacionId", operacion.id);
        Navigation.findNavController(requireView()).navigate(R.id.action_historial_to_calificar, bundle);
    }

    interface OnDateSelectedListener {
        void onDateSelected(String date);
    }
}
