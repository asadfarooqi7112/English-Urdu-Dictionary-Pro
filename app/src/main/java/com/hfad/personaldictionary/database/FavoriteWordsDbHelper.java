package com.hfad.personaldictionary.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.hfad.personaldictionary.models.FavoriteWord;

import java.util.ArrayList;
import java.util.List;

public class FavoriteWordsDbHelper extends SQLiteOpenHelper {

    // Database name and version
    private static final String DATABASE_NAME = "favorite_words.db";
    private static final int DATABASE_VERSION = 3; // Incremented version to add 'is_english' column

    // Table name and column names
    public static final String TABLE_FAVORITES = "favorites";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_WORD = "word";
    public static final String COLUMN_WORD_ID = "word_id";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_IS_ENGLISH = "is_english"; // New column for language flag

    // SQL statement to create the favorites table
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_FAVORITES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_WORD + " TEXT NOT NULL UNIQUE," +
                    COLUMN_WORD_ID + " INTEGER," +
                    COLUMN_TIMESTAMP + " INTEGER," +
                    COLUMN_IS_ENGLISH + " INTEGER DEFAULT 1)";

    public FavoriteWordsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This is a proper schema migration for adding a new column.
        if (oldVersion < 3) {
            // Add the new column without losing old data.
            // Old entries will get the default value (1 for English).
            db.execSQL("ALTER TABLE " + TABLE_FAVORITES + " ADD COLUMN " + COLUMN_IS_ENGLISH + " INTEGER NOT NULL DEFAULT 1;");
        }
    }

    // --- CRUD Operations ---

    /**
     * Adds a new favorite word to the database.
     * @param favoriteWord The FavoriteWord object to add.
     * @return The row ID of the newly inserted row, or -1 if an error occurred.
     */
    public long addFavoriteWord(FavoriteWord favoriteWord) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_WORD, favoriteWord.getWord());
        values.put(COLUMN_WORD_ID, favoriteWord.getWordId());
        values.put(COLUMN_TIMESTAMP, favoriteWord.getTimestamp());
        values.put(COLUMN_IS_ENGLISH, favoriteWord.isLanguageEnglish() ? 1 : 0);

        long newRowId = -1;
        try {
            newRowId = db.insertWithOnConflict(TABLE_FAVORITES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        } catch (Exception e) {
            // Log or handle any insertion errors
        }
        db.close();
        return newRowId;
    }

    /**
     * Retrieves all favorite words from the database, ordered by timestamp (most recent first).
     * @return A list of FavoriteWord objects.
     */
    public List<FavoriteWord> getAllFavoriteWords() {
        List<FavoriteWord> favoriteWordsList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_FAVORITES + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String word = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORD));
                long wordId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_WORD_ID));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                int isEnglish = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ENGLISH));

                // Create a new FavoriteWord object with all retrieved data
                FavoriteWord favoriteWord = new FavoriteWord(word, wordId, isEnglish == 1, timestamp);
                favoriteWordsList.add(favoriteWord);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return favoriteWordsList;
    }

    /**
     * Deletes a favorite word from the database based on its word string.
     * @param word The word string to delete.
     * @return The number of rows affected (should be 1 if successful, 0 otherwise).
     */
    public int deleteFavoriteWord(String word) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_FAVORITES, COLUMN_WORD + " = ?", new String[]{word});
        db.close();
        return rowsAffected;
    }

    /**
     * Checks if a word is already in the favorites list.
     * @param word The word to check.
     * @return true if the word is a favorite, false otherwise.
     */
    public boolean isFavorite(String word) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_ID};
        String selection = COLUMN_WORD + " = ?";
        String[] selectionArgs = {word};
        Cursor cursor = db.query(TABLE_FAVORITES, columns, selection, selectionArgs, null, null, null);
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Clears all favorite words from the database.
     */
    public void clearAllFavorites() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FAVORITES, null, null);
        db.close();
    }
}
