package com.hfad.personaldictionary.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.hfad.personaldictionary.models.FavoriteWord; // Import your FavoriteWord model
import java.util.ArrayList;
import java.util.List;

public class FavoriteWordsDbHelper extends SQLiteOpenHelper {

    // Database name and version
    private static final String DATABASE_NAME = "favorite_words.db";
    private static final int DATABASE_VERSION = 1;

    // Table name and column names
    public static final String TABLE_FAVORITES = "favorites";
    public static final String COLUMN_ID = "_id"; // Recommended column name for Android CursorAdapters
    public static final String COLUMN_WORD = "word";
    public static final String COLUMN_WORD_ID = "word_id"; // Stores the ID from your main dictionary
    public static final String COLUMN_TIMESTAMP = "timestamp";

    // SQL statement to create the favorites table
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_FAVORITES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_WORD + " TEXT NOT NULL UNIQUE," + // UNIQUE constraint prevents duplicate words
                    COLUMN_WORD_ID + " INTEGER," +
                    COLUMN_TIMESTAMP + " INTEGER)"; // Store timestamp as a long (milliseconds)

    // SQL statement to drop the favorites table
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_FAVORITES;

    public FavoriteWordsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This simple upgrade policy discards the data and starts over.
        // For a real app, you'd implement proper schema migration.
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    // --- CRUD Operations ---

    /**
     * Adds a new favorite word to the database.
     * If the word already exists (due to UNIQUE constraint on 'word'), it will be ignored.
     * @param favoriteWord The FavoriteWord object to add.
     * @return The row ID of the newly inserted row, or -1 if an error occurred or row already exists.
     */
    public long addFavoriteWord(FavoriteWord favoriteWord) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_WORD, favoriteWord.getWord());
        values.put(COLUMN_WORD_ID, favoriteWord.getWordId());
        values.put(COLUMN_TIMESTAMP, favoriteWord.getTimestamp());

        // insertWithOnConflict with CONFLICT_IGNORE will not insert if a UNIQUE conflict occurs
        long newRowId = db.insertWithOnConflict(TABLE_FAVORITES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
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

        SQLiteDatabase db = this.getReadableDatabase(); // Use readable database for queries
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                // Get column indices and then retrieve values
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String word = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORD));
                int wordId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WORD_ID));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));

                FavoriteWord favoriteWord = new FavoriteWord(id, word, wordId, timestamp);
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
        String[] columns = {COLUMN_ID}; // Only need to check for existence
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