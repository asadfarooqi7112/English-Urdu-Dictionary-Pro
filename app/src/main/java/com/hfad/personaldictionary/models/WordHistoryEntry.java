package com.hfad.personaldictionary.models;

public class WordHistoryEntry {
    private long id; // This is the history table ID, not the word's ID
    private String word;
    private long wordEnglishId; // Changed to long to match database
    private boolean isLanguageEnglish; // NEW: Stores the language of the word
    private long timestamp;

    public WordHistoryEntry(long id, String word, long wordEnglishId, boolean isLanguageEnglish, long timestamp) {
        this.id = id;
        this.word = word;
        this.wordEnglishId = wordEnglishId;
        this.isLanguageEnglish = isLanguageEnglish;
        this.timestamp = timestamp;
    }

    // Constructor for adding a new entry
    public WordHistoryEntry(String word, long wordEnglishId, boolean isLanguageEnglish) {
        this.word = word;
        this.wordEnglishId = wordEnglishId;
        this.isLanguageEnglish = isLanguageEnglish;
        this.timestamp = System.currentTimeMillis();
    }

    public WordHistoryEntry() {}

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public long getWordEnglishId() {
        return wordEnglishId;
    }

    public void setWordEnglishId(long wordEnglishId) {
        this.wordEnglishId = wordEnglishId;
    }

    // NEW: Getter for the language flag
    public boolean isLanguageEnglish() {
        return isLanguageEnglish;
    }

    // NEW: Setter for the language flag
    public void setLanguageEnglish(boolean isLanguageEnglish) {
        this.isLanguageEnglish = isLanguageEnglish;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
