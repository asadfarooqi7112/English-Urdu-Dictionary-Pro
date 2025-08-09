package com.hfad.personaldictionary.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hfad.personaldictionary.R; // Make sure your R path is correct
import com.hfad.personaldictionary.models.FavoriteWord;
import java.util.List;

public class FavoriteWordsAdapter extends RecyclerView.Adapter<FavoriteWordsAdapter.ViewHolder> {

    private List<FavoriteWord> favoriteWords;
    private OnItemClickListener listener;

    // Interface to handle clicks on items or specific buttons within items
    public interface OnItemClickListener {
        void onItemClick(FavoriteWord word);
        void onRemoveClick(FavoriteWord word); // For a delete/unfavorite button
    }

    public FavoriteWordsAdapter(List<FavoriteWord> favoriteWords, OnItemClickListener listener) {
        this.favoriteWords = favoriteWords;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite_word, parent, false); // Link to your item layout
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FavoriteWord favoriteWord = favoriteWords.get(position);
        holder.wordTextView.setText(favoriteWord.getWord());

        // Set click listeners
        holder.itemView.setOnClickListener(v -> listener.onItemClick(favoriteWord));
        holder.removeButton.setOnClickListener(v -> listener.onRemoveClick(favoriteWord));
    }

    @Override
    public int getItemCount() {
        return favoriteWords.size();
    }

    // ViewHolder class to hold references to the views in each item layout
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView wordTextView;
        public ImageButton removeButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            wordTextView = itemView.findViewById(R.id.tv_favorite_word); // ID from item_favorite_word.xml
            removeButton = itemView.findViewById(R.id.btn_remove_favorite); // ID from item_favorite_word.xml
        }
    }

    // Method to update the data in the adapter and refresh the RecyclerView
    public void updateData(List<FavoriteWord> newFavoriteWords) {
        this.favoriteWords.clear();
        this.favoriteWords.addAll(newFavoriteWords);
        notifyDataSetChanged();
    }
}