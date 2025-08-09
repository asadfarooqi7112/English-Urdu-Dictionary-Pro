package com.hfad.personaldictionary.models;

public class FavoriteWord {
    private int id;
    private String word;
    private long wordId; // The ID of this word from your main dictionary source
    private long timestamp;

    // MODIFIED: Corrected the wordId parameter to be a long
    public FavoriteWord(int id, String word, long wordId, long timestamp) {
        this.id = id;
        this.word = word;
        this.wordId = wordId;
        this.timestamp = timestamp;
    }

    // This constructor is already correct
    public FavoriteWord(String word, long wordId) {
        this.word = word;
        this.wordId = wordId;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getWord() {
        return word;
    }

    public long getWordId() {
        return wordId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FavoriteWord that = (FavoriteWord) o;
        return word.equalsIgnoreCase(that.word);
    }

    @Override
    public int hashCode() {
        return word.toLowerCase().hashCode();
    }
}