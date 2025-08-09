package com.hfad.personaldictionary.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hfad.personaldictionary.R;
import com.hfad.personaldictionary.models.WordHistoryEntry; // Import your new data class

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<WordHistoryEntry> historyWords; // MODIFIED: List of WordHistoryEntry
    private OnItemClickListener itemClickListener;
    private OnDeleteItemClickListener deleteListener;

    // MODIFIED: OnItemClickListener now passes WordHistoryEntry
    public interface OnItemClickListener {
        void onItemClick(WordHistoryEntry entry);
    }

    // OnDeleteItemClickListener can remain String, as removeSearchTerm takes a string
    public interface OnDeleteItemClickListener {
        void onDeleteItemClick(String word);
    }

    // MODIFIED CONSTRUCTOR
    public HistoryAdapter(List<WordHistoryEntry> historyWords, OnItemClickListener itemClickListener, OnDeleteItemClickListener deleteListener) {
        this.historyWords = historyWords;
        this.itemClickListener = itemClickListener;
        this.deleteListener = deleteListener;
    }

    // MODIFIED updateData
    public void updateData(List<WordHistoryEntry> newWords) {
        this.historyWords = newWords;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_word, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        WordHistoryEntry entry = historyWords.get(position); // Get the WordHistoryEntry object
        holder.tvHistoryWord.setText(entry.getWord());       // Display the word

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(entry); // MODIFIED: Pass the WordHistoryEntry object
                }
            }
        });

        // Set click listener for the delete icon
        holder.ivDeleteHistoryItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deleteListener != null) {
                    deleteListener.onDeleteItemClick(entry.getWord()); // Pass only the word for deletion
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyWords.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvHistoryWord;
        ImageView ivDeleteHistoryItem;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryWord = itemView.findViewById(R.id.tvHistoryWord);
            ivDeleteHistoryItem = itemView.findViewById(R.id.ivDeleteHistoryItem);
        }
    }
}