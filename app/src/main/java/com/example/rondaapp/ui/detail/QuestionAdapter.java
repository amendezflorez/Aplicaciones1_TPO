package com.example.rondaapp.ui.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rondaapp.R;
import com.example.rondaapp.data.model.Question;
import com.example.rondaapp.ui.profile.ProfileFormatter;

import java.util.ArrayList;
import java.util.List;

/** Preguntas dejadas en la publicación, de la más nueva a la más vieja. */
public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder> {

    private final List<Question> preguntas = new ArrayList<>();

    public void setPreguntas(List<Question> nuevas) {
        preguntas.clear();
        if (nuevas != null) preguntas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_question, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        Question pregunta = preguntas.get(position);
        String autor = pregunta.getUserName() != null ? pregunta.getUserName() : "";
        String fecha = ProfileFormatter.fechaCorta(pregunta.getCreatedAt());

        holder.tvAuthor.setText(holder.itemView.getContext()
                .getString(R.string.detail_question_author, autor, fecha != null ? fecha : ""));
        holder.tvText.setText(pregunta.getText());
    }

    @Override
    public int getItemCount() {
        return preguntas.size();
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAuthor, tvText;

        QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvQuestionAuthor);
            tvText = itemView.findViewById(R.id.tvQuestionText);
        }
    }
}
