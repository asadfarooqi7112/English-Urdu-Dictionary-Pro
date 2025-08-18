package com.hfad.personaldictionary.models;

public class FavoriteWord {
    private String word;
    private long wordId;
    private boolean isLanguageEnglish;
    private long timestamp;

    public FavoriteWord(String word, long wordId, boolean isLanguageEnglish, long timestamp) {
        this.word = word;
        this.wordId = wordId;
        this.isLanguageEnglish = isLanguageEnglish;
        this.timestamp = timestamp;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public long getWordId() {
        return wordId;
    }

    public void setWordId(long wordId) {
        this.wordId = wordId;
    }

    public boolean isLanguageEnglish() {
        return isLanguageEnglish;
    }

    public void setLanguageEnglish(boolean languageEnglish) {
        isLanguageEnglish = languageEnglish;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
