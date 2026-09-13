package com.example.rondaapp.ui.publish;

import android.app.AlertDialog;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.CreatePublicationBody;
import com.example.rondaapp.data.model.Publication;
import com.example.rondaapp.data.network.ApiService;
import com.example.rondaapp.session.SessionManager;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Punto 5: carga guiada en pasos para publicar un artículo.
 *
 * El borrador se persiste en cada cambio de paso y en onPause, así que si la
 * persona sale de la app a mitad de camino, al volver retoma en el mismo paso
 * con los datos y las fotos que había cargado.
 */
@AndroidEntryPoint
public class PublishFragment extends Fragment {

    @Inject
    ApiService apiService;

    private static final int CANTIDAD_PASOS = 3;
    private static final int MAX_FOTOS = 5;

    private static final String[] CATEGORIAS =
            {"Deportes", "Tecnología", "Hogar", "Música", "Indumentaria"};
    private static final String[] CONDICIONES = {"nuevo", "como nuevo", "usado"};

    private ViewFlipper flipperSteps;
    private TextView tvStepTitle, tvStepHint, tvNoPhotos;
    private Button btnBack, btnNext, btnAddPhotos, btnDiscardDraft;
    private ProgressBar progressPublish;
    private ViewGroup containerPhotos;
    private EditText etTitle, etDescription, etPrice, etZone;
    private Spinner spinnerCategory, spinnerCondition;

    private PublishDraft draft;
    private SessionManager sessionManager;
    private ActivityResultLauncher<PickVisualMediaRequest> selectorFotos;
    private boolean publicando = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // El selector debe registrarse antes de que el fragment esté RESUMED.
        selectorFotos = registerForActivityResult(
                new ActivityResultContracts.PickMultipleVisualMedia(MAX_FOTOS),
                this::onFotosElegidas);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_publish, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        flipperSteps = view.findViewById(R.id.flipperSteps);
        tvStepTitle = view.findViewById(R.id.tvStepTitle);
        tvStepHint = view.findViewById(R.id.tvStepHint);
        tvNoPhotos = view.findViewById(R.id.tvNoPhotos);
        btnBack = view.findViewById(R.id.btnBack);
        btnNext = view.findViewById(R.id.btnNext);
        btnAddPhotos = view.findViewById(R.id.btnAddPhotos);
        btnDiscardDraft = view.findViewById(R.id.btnDiscardDraft);
        progressPublish = view.findViewById(R.id.progressPublish);
        containerPhotos = view.findViewById(R.id.containerPhotos);
        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        etPrice = view.findViewById(R.id.etPrice);
        etZone = view.findViewById(R.id.etZone);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        spinnerCondition = view.findViewById(R.id.spinnerCondition);

        sessionManager = new SessionManager(requireContext());
        spinnerCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, CATEGORIAS));
        spinnerCondition.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, CONDICIONES));

        draft = new PublishDraft(requireContext());
        draft.cargar();
        volcarBorradorEnFormulario();

        engancharAutoguardado();
        btnAddPhotos.setOnClickListener(v -> abrirGaleria());
        btnBack.setOnClickListener(v -> irAlPaso(draft.paso - 1));
        btnNext.setOnClickListener(v -> avanzar());
        btnDiscardDraft.setOnClickListener(v -> confirmarDescarte());

        if (draft.hayBorrador()) {
            Toast.makeText(requireContext(), R.string.publish_draft_restored, Toast.LENGTH_SHORT).show();
        }
        irAlPaso(draft.paso);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Este es el punto que cubre "salir de la app a mitad de la carga".
        if (!publicando) {
            leerFormularioEnBorrador();
            draft.guardar();
        }
    }

    /**
     * Persiste el borrador a medida que se escribe.
     *
     * Guardar solo en onPause no alcanza: si el sistema mata el proceso sin pasar
     * por onPause se pierde lo tipeado en el paso actual, que es justo el caso que
     * el TP pide cubrir. SharedPreferences.apply() escribe en memoria al instante
     * y baja a disco en background, así que el costo por tecla es despreciable.
     */
    private void engancharAutoguardado() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                leerFormularioEnBorrador();
                draft.guardar();
            }
        };

        etTitle.addTextChangedListener(watcher);
        etDescription.addTextChangedListener(watcher);
        etPrice.addTextChangedListener(watcher);
        etZone.addTextChangedListener(watcher);

        AdapterView.OnItemSelectedListener spinnerWatcher = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                leerFormularioEnBorrador();
                draft.guardar();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        spinnerCategory.setOnItemSelectedListener(spinnerWatcher);
        spinnerCondition.setOnItemSelectedListener(spinnerWatcher);
    }

    // ---------- Pasos ----------

    private void irAlPaso(int paso) {
        leerFormularioEnBorrador();
        draft.paso = Math.max(0, Math.min(paso, CANTIDAD_PASOS - 1));
        draft.guardar();

        flipperSteps.setDisplayedChild(draft.paso);
        tvStepTitle.setText(getString(R.string.publish_step_title,
                draft.paso + 1, CANTIDAD_PASOS, tituloDePaso(draft.paso)));
        tvStepHint.setText(pistaDePaso(draft.paso));

        btnBack.setEnabled(draft.paso > 0);
        btnNext.setText(esUltimoPaso()
                ? getString(R.string.publish_submit)
                : getString(R.string.publish_next));
    }

    private boolean esUltimoPaso() {
        return draft.paso == CANTIDAD_PASOS - 1;
    }

    private String tituloDePaso(int paso) {
        switch (paso) {
            case 0: return getString(R.string.publish_step_photos);
            case 1: return getString(R.string.publish_step_data);
            default: return getString(R.string.publish_step_price);
        }
    }

    private String pistaDePaso(int paso) {
        switch (paso) {
            case 0: return getString(R.string.publish_hint_photos, MAX_FOTOS);
            case 1: return getString(R.string.publish_hint_data);
            default: return getString(R.string.publish_hint_price);
        }
    }

    private void avanzar() {
        if (!validarPasoActual()) return;

        if (esUltimoPaso()) {
            publicar();
        } else {
            irAlPaso(draft.paso + 1);
        }
    }

    /** Cada paso valida solo lo suyo, para no bloquear al usuario con errores de más adelante. */
    private boolean validarPasoActual() {
        if (draft.paso == 1) {
            if (etTitle.getText().toString().trim().isEmpty()) {
                etTitle.setError(getString(R.string.publish_error_title));
                return false;
            }
        } else if (draft.paso == 2) {
            String precio = etPrice.getText().toString().trim();
            if (precio.isEmpty()) {
                etPrice.setError(getString(R.string.publish_error_price));
                return false;
            }
            if (parsearPrecio(precio) == null) {
                etPrice.setError(getString(R.string.publish_error_price_invalid));
                return false;
            }
        }
        return true;
    }

    /** Acepta coma como separador decimal, que es lo que escribe cualquiera acá. */
    private Double parsearPrecio(String texto) {
        try {
            double valor = Double.parseDouble(texto.replace(',', '.'));
            return valor >= 0 ? valor : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------- Borrador <-> formulario ----------

    private void volcarBorradorEnFormulario() {
        etTitle.setText(draft.titulo);
        etDescription.setText(draft.descripcion);
        etPrice.setText(draft.precio);
        etZone.setText(draft.zona.isEmpty() && sessionManager.getZone() != null
                ? sessionManager.getZone()   // por defecto, la zona del perfil
                : draft.zona);
        seleccionarEnSpinner(spinnerCategory, CATEGORIAS, draft.categoria);
        seleccionarEnSpinner(spinnerCondition, CONDICIONES, draft.condicion);
        pintarFotos();
    }

    private void leerFormularioEnBorrador() {
        if (getView() == null) return;
        draft.titulo = etTitle.getText().toString().trim();
        draft.descripcion = etDescription.getText().toString().trim();
        draft.precio = etPrice.getText().toString().trim();
        draft.zona = etZone.getText().toString().trim();
        draft.categoria = (String) spinnerCategory.getSelectedItem();
        draft.condicion = (String) spinnerCondition.getSelectedItem();
    }

    private void seleccionarEnSpinner(Spinner spinner, String[] opciones, String valor) {
        if (valor == null || valor.isEmpty()) return;
        for (int i = 0; i < opciones.length; i++) {
            if (opciones[i].equals(valor)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    // ---------- Fotos ----------

    private void abrirGaleria() {
        if (draft.fotos.size() >= MAX_FOTOS) {
            Toast.makeText(requireContext(),
                    getString(R.string.publish_error_max_photos, MAX_FOTOS), Toast.LENGTH_SHORT).show();
            return;
        }
        selectorFotos.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void onFotosElegidas(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;

        int fallidas = 0;
        for (Uri uri : uris) {
            if (draft.fotos.size() >= MAX_FOTOS) break;

            File archivo = PhotoStore.guardarDesdeUri(requireContext(), uri);
            if (archivo != null) {
                draft.fotos.add(archivo);
            } else {
                fallidas++;
            }
        }

        if (fallidas > 0) {
            Toast.makeText(requireContext(), R.string.publish_error_photo, Toast.LENGTH_SHORT).show();
        }
        draft.guardar();
        pintarFotos();
    }

    private void pintarFotos() {
        containerPhotos.removeAllViews();
        tvNoPhotos.setVisibility(draft.fotos.isEmpty() ? View.VISIBLE : View.GONE);

        for (int i = 0; i < draft.fotos.size(); i++) {
            File foto = draft.fotos.get(i);
            View fila = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_draft_photo, containerPhotos, false);

            ImageView iv = fila.findViewById(R.id.ivDraftPhoto);
            iv.setImageBitmap(BitmapFactory.decodeFile(foto.getAbsolutePath()));
            ((TextView) fila.findViewById(R.id.tvDraftPhotoPosition))
                    .setText(getString(R.string.publish_photo_position, i + 1));

            fila.findViewById(R.id.btnRemovePhoto).setOnClickListener(v -> {
                draft.fotos.remove(foto);
                PhotoStore.borrar(foto);
                draft.guardar();
                pintarFotos();
            });

            containerPhotos.addView(fila);
        }
    }

    // ---------- Publicar ----------

    private void publicar() {
        leerFormularioEnBorrador();

        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(requireContext(), R.string.publish_error_generic, Toast.LENGTH_SHORT).show();
            return;
        }

        Double precio = parsearPrecio(draft.precio);
        if (precio == null) {
            etPrice.setError(getString(R.string.publish_error_price_invalid));
            return;
        }

        publicando = true;
        mostrarProgreso(true);
        btnNext.setEnabled(false);

        List<String> fotosBase64 = new ArrayList<>();
        for (File foto : draft.fotos) {
            String dataUri = PhotoStore.aDataUri(foto);
            if (dataUri != null) fotosBase64.add(dataUri);
        }

        CreatePublicationBody body = new CreatePublicationBody(
                userId, draft.titulo, draft.descripcion, precio,
                draft.condicion, draft.categoria, draft.zona, fotosBase64);

        apiService.createPublication(body).enqueue(new Callback<Publication>() {
            @Override
            public void onResponse(@NonNull Call<Publication> call, @NonNull Response<Publication> response) {
                if (!isAdded() || getView() == null) return;
                mostrarProgreso(false);
                btnNext.setEnabled(true);

                if (!response.isSuccessful() || response.body() == null) {
                    publicando = false;
                    Toast.makeText(getContext(), R.string.publish_error_generic, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Recién con la publicación confirmada se descarta el borrador.
                draft.limpiar();
                Toast.makeText(getContext(), R.string.publish_success, Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_publish_to_myPublications);
            }

            @Override
            public void onFailure(@NonNull Call<Publication> call, @NonNull Throwable t) {
                if (!isAdded() || getView() == null) return;
                publicando = false;
                mostrarProgreso(false);
                btnNext.setEnabled(true);
                Toast.makeText(getContext(), R.string.publish_error_generic, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmarDescarte() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.publish_discard_title)
                .setMessage(R.string.publish_discard_message)
                .setPositiveButton(R.string.publish_discard_confirm, (d, w) -> {
                    draft.limpiar();
                    volcarBorradorEnFormulario();
                    irAlPaso(0);
                    Toast.makeText(requireContext(), R.string.publish_discarded, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.logout_dialog_cancel, (d, w) -> d.dismiss())
                .show();
    }

    private void mostrarProgreso(boolean visible) {
        if (progressPublish != null) {
            progressPublish.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
