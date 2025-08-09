// src/main/java/com/hfad/personaldictionary/models/WordHistoryEntry.java
package com.hfad.personaldictionary.models;

public class WordHistoryEntry {
    private long id; // This is the history table ID, not the word's ID
    private String word;
    private int wordEnglishId; // NEW: Stores the ID of the word from the main dictionary
    private long timestamp;

    public WordHistoryEntry(long id, String word, int wordEnglishId, long timestamp) {
        this.id = id;
        this.word = word;
        this.wordEnglishId = wordEnglishId;
        this.timestamp = timestamp;
    }

    // Constructor for adding a new entry
    public WordHistoryEntry(String word, int wordEnglishId) {
        this.word = word;
        this.wordEnglishId = wordEnglishId;
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

    public int getWordEnglishId() {
        return wordEnglishId;
    }

    public void setWordEnglishId(int wordEnglishId) {
        this.wordEnglishId = wordEnglishId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}