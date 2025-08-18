package com.hfad.personaldictionary.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.hfad.personaldictionary.models.WordHistoryEntry;

import java.util.ArrayList;
import java.util.List;

public class HistoryDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "History.db";
    private static final int DATABASE_VERSION = 3; // MODIFIED: Increment the version to trigger onUpgrade

    public static final String TABLE_NAME = "word_history";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_WORD = "word";
    public static final String COLUMN_WORD_ENGLISH_ID = "word_english_id";
    public static final String COLUMN_IS_ENGLISH = "is_english"; // NEW: Column for language flag
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public HistoryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_WORD + " TEXT NOT NULL, " +
                COLUMN_WORD_ENGLISH_ID + " INTEGER NOT NULL, " +
                COLUMN_IS_ENGLISH + " INTEGER NOT NULL, " + // ADDED COLUMN
                COLUMN_TIMESTAMP + " INTEGER NOT NULL);";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop old table and create new one to handle the schema change
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // --- CRUD Operations ---

    public void addHistoryEntry(String word, long wordEnglishId, boolean isLanguageEnglish) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_WORD, word);
        values.put(COLUMN_WORD_ENGLISH_ID, wordEnglishId);
        values.put(COLUMN_IS_ENGLISH, isLanguageEnglish ? 1 : 0); // NEW: Save boolean as an integer
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public List<WordHistoryEntry> getAllHistory() {
        List<WordHistoryEntry> historyList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String word = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WORD));
                long wordEnglishId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_WORD_ENGLISH_ID)); // MODIFIED: Use long to match data type
                boolean isEnglish = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ENGLISH)) == 1; // NEW: Retrieve boolean
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                historyList.add(new WordHistoryEntry(id, word, wordEnglishId, isEnglish, timestamp));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return historyList;
    }

    public void deleteHistoryEntry(String word) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_WORD + " = ?", new String[]{word});
        db.close();
    }

    public void clearHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
        db.close();
    }
}
