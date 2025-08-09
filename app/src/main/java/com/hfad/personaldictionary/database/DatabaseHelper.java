package com.hfad.personaldictionary.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper {
    private static final String DB_NAME = "our_database.db";
    private final Context context;
    private final String DB_PATH;
    private SQLiteDatabase database;

    // --- DATABASE VERSIONING ---
    // IMPORTANT: Increment this version number every time you make a schema change
    // or add new data via migration files.
    // Version 1: Initial state (eng_urdu_words table from asset, favorites table created by app)
    // Version 2: (Future) Example: Added 'example_sentence' column and populated new/updated data from CSV.
    // Set to 1 for your current initial release.
    private static final int DATABASE_VERSION = 1; // Current expected database schema version

    // Keys for storing the database version in SharedPreferences
    private static final String PREFS_NAME = "app_database_prefs";
    private static final String KEY_DB_VERSION = "db_version";

    public DatabaseHelper(Context context) {
        this.context = context;
        this.DB_PATH = context.getDatabasePath(DB_NAME).getPath();
        // createFavoritesTable(); // Original line, now called within openDatabase() lifecycle
    }

    // Copies the database from assets if not already present
    public void copyDatabaseIfNeeded() throws IOException {
        File dbFile = new File(DB_PATH);
        File dbDir = dbFile.getParentFile(); // Get parent directory for mkdirs

        if (dbFile.exists()) {
            Log.d("DatabaseHelper", "Database file already exists at: " + DB_PATH + ". No need to copy.");
        } else {
            Log.d("DatabaseHelper", "Database file does NOT exist at: " + DB_PATH + ". Attempting to copy from assets.");

            // Ensure parent directories exist before attempting to copy the file
            if (dbDir != null && !dbDir.exists()) {
                dbDir.mkdirs(); // Create necessary directories
                Log.d("DatabaseHelper", "Created database directory: " + dbDir.getAbsolutePath());
            }

            try (InputStream is = context.getAssets().open(DB_NAME);
                 OutputStream os = new FileOutputStream(DB_PATH)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
                os.flush();
            }
            Log.d("DatabaseHelper", "Database copied successfully from assets to: " + DB_PATH);

            // After the initial copy, record the current DATABASE_VERSION in SharedPreferences.
            // This marks the newly copied database as being at the app's current expected version.
            updateDatabaseVersion(DATABASE_VERSION);
            Log.d("DatabaseHelper", "Initial database version set to " + DATABASE_VERSION + " in SharedPreferences.");
        }
    }

    // Open DB only once and reuse it
    public void openDatabase() {
        if (database == null || !database.isOpen()) {
            try {
                // Step 1: Ensure the database file is present on the device.
                // This will copy it from assets if it doesn't exist and set its initial version.
                copyDatabaseIfNeeded();

                // Step 2: Open the SQLite database connection.
                database = SQLiteDatabase.openDatabase(DB_PATH, null, SQLiteDatabase.OPEN_READWRITE);
                Log.d("DatabaseHelper", "Database connection opened successfully.");

                // Step 3: Get the current database version stored on the user's device.
                int currentDeviceDbVersion = getCurrentDatabaseVersion();
                Log.d("DatabaseHelper", "Device DB version: " + currentDeviceDbVersion +
                        ", App's expected DB version: " + DATABASE_VERSION);

                // Step 4: If the device's database version is older than the app's expected version,
                // perform the necessary migrations.
                if (currentDeviceDbVersion < DATABASE_VERSION) {
                    Log.d("DatabaseHelper", "Migration required. Starting migration from version " +
                            currentDeviceDbVersion + " to " + DATABASE_VERSION);
                    performDatabaseMigration(database, currentDeviceDbVersion, DATABASE_VERSION);
                    // After successful migration, update the stored version in SharedPreferences.
                    updateDatabaseVersion(DATABASE_VERSION);
                    Log.d("DatabaseHelper", "Database migration completed and version updated to " +
                            DATABASE_VERSION + " in SharedPreferences.");
                }



            } catch (IOException e) {
                Log.e("DatabaseHelper", "Error during database copy or open: " + e.getMessage(), e);
                // Consider showing a user-friendly error message or gracefully degrading functionality
            } catch (Exception e) {
                Log.e("DatabaseHelper", "An unexpected error occurred during database operations: " + e.getMessage(), e);
            }
        }
    }

    public void closeDatabase() {
        if (database != null && database.isOpen()) {
            database.close();
            Log.d("DatabaseHelper", "Database connection closed.");
        }
    }

    /**
     * Retrieves the current database version stored in SharedPreferences.
     * This value represents the version of the database schema currently
     * present on the user's device.
     *
     * @return The stored database version, or 0 if no version has been stored yet
     * (indicating a fresh install before the initial copy).
     */
    private int getCurrentDatabaseVersion() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_DB_VERSION, 0); // Default to 0 if the key is not found
    }

    /**
     * Updates the database version stored in SharedPreferences.
     * This should be called after a successful initial copy or a successful migration.
     *
     * @param newVersion The new database version number to store.
     */
    private void updateDatabaseVersion(int newVersion) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_DB_VERSION, newVersion).apply();
    }

    /**
     * Performs database migration steps. This method contains the SQL commands
     * and data population logic required to update the database schema and data
     * from an older version to a newer one.
     *
     * @param db         The SQLiteDatabase instance on which to perform the migration.
     * @param oldVersion The current version of the database on the device.
     * @param newVersion The target version of the database (DATABASE_VERSION).
     */
    private void performDatabaseMigration(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DatabaseHelper", "Initiating migration steps from " + oldVersion + " to " + newVersion);

        // --- MIGRATION FROM VERSION 1 TO 2 ---
        // This block will execute ONLY IF oldVersion is less than 2
        // and DATABASE_VERSION (at the top of this class) is 2 or higher.
        // For your initial release (DATABASE_VERSION = 1), this block will NOT run.
        // When you plan your first migration (e.g., adding 'example_sentence'),
        // you will change DATABASE_VERSION to 2 and uncomment/fill this block.
        /*
        if (oldVersion < 2) {
            Log.d("DatabaseHelper", "Applying migration: Version 1 to Version 2.");

            // 1. Schema Change: Add the new 'example_sentence' column to 'eng_urdu_words' table.
            // It's crucial to provide a DEFAULT value to avoid issues with existing rows.
            db.execSQL("ALTER TABLE eng_urdu_words ADD COLUMN example_sentence TEXT DEFAULT '';");
            Log.d("DatabaseHelper", "Schema updated: 'example_sentence' column added to 'eng_urdu_words'.");

            // 2. Data Change: Populate the new column and/or insert new words from a CSV file.
            // This reads the 'delta' data for this specific migration step.
            try {
                // Ensure 'migration_data_v1_to_v2.csv' is in your app's 'assets' folder
                InputStream is = context.getAssets().open("migration_data_v1_to_v2.csv");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                boolean isFirstLine = true; // Flag to skip the header row in CSV

                db.beginTransaction(); // Start a database transaction for efficiency and atomicity
                try {
                    while ((line = reader.readLine()) != null) {
                        if (isFirstLine) {
                            isFirstLine = false; // Skip the header row
                            continue;
                        }

                        // Basic CSV parsing: split by comma.
                        // WARNING: This simple split will break if data fields contain unquoted commas.
                        // For more robust CSV parsing with quoted fields, consider a dedicated library
                        // or a more advanced parsing logic.
                        String[] columns = line.split(",");

                        // Ensure the line has enough columns as expected
                        if (columns.length >= 4) { // Assuming: word, pos, urdu_meaning, example_sentence
                            String word = columns[0].trim();
                            String pos = columns[1].trim();
                            String urduMeaning = columns[2].trim();
                            String exampleSentence = columns[3].trim();

                            ContentValues values = new ContentValues();
                            values.put("pos", pos);
                            values.put("urdu_meaning", urduMeaning);
                            values.put("example_sentence", exampleSentence);

                            // Attempt to UPDATE an existing word.
                            // Using LOWER() for case-insensitive matching of the word.
                            int rowsAffected = db.update("eng_urdu_words", values, "LOWER(word) = LOWER(?)", new String[]{word});

                            if (rowsAffected == 0) {
                                // If no rows were updated, it means this is a new word, so INSERT it.
                                values.put("word", word); // Add the word to ContentValues for insertion
                                db.insert("eng_urdu_words", null, values);
                                Log.d("DatabaseHelper", "Data Migration: Inserted new word: " + word);
                            } else {
                                Log.d("DatabaseHelper", "Data Migration: Updated existing word: " + word);
                            }
                        } else {
                            Log.w("DatabaseHelper", "Data Migration: Skipping malformed CSV line (not enough columns): " + line);
                        }
                    }
                    db.setTransactionSuccessful(); // Mark the transaction as successful
                    Log.d("DatabaseHelper", "Data population from migration_data_v1_to_v2.csv completed.");
                } finally {
                    // End the transaction. If successful, changes are committed; otherwise, rolled back.
                    db.endTransaction();
                    reader.close(); // Close the BufferedReader
                }

            } catch (IOException e) {
                Log.e("DatabaseHelper", "Data Migration Error: Could not read migration_data_v1_to_v2.csv: " + e.getMessage(), e);
            } catch (Exception e) { // Catch any other unexpected exceptions during parsing or DB operations
                Log.e("DatabaseHelper", "Data Migration Error: An unexpected error occurred during CSV processing: " + e.getMessage(), e);
            }
        }
        */

        // --- MIGRATION FROM VERSION 2 TO 3 (FUTURE EXAMPLE) ---
        // When you need to perform your next update (e.g., add another column,
        // or add more new data), you would:
        // 1. Increment DATABASE_VERSION to 3 at the top of this class.
        // 2. Create a new CSV file: assets/migration_data_v2_to_v3.csv
        // 3. Add your schema and data changes within this new 'if' block.
        if (oldVersion < 3) {
            Log.d("DatabaseHelper", "Applying migration: Version 2 to Version 3.");
            // Example Schema Change:
            // db.execSQL("ALTER TABLE eng_urdu_words ADD COLUMN another_new_column TEXT DEFAULT '';");

            // Example Data Change (similar CSV reading logic, but for migration_data_v2_to_v3.csv):
            /*
            try {
                InputStream is = context.getAssets().open("migration_data_v2_to_v3.csv");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                // ... read, parse, and INSERT/UPDATE data ...
            } catch (IOException e) {
                Log.e("DatabaseHelper", "Data Migration Error: Could not read migration_data_v2_to_v3.csv: " + e.getMessage(), e);
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Data Migration Error: An unexpected error occurred during CSV processing for V3: " + e.getMessage(), e);
            }
            */
        }

        // Continue adding 'if (oldVersion < X)' blocks for each subsequent database version.
        // This ensures that all necessary migrations are applied sequentially if a user
        // updates from a very old version to the latest.

        Log.d("DatabaseHelper", "All specified database migration steps completed.");
    }

    /**
     * Ensures the 'favorites' table exists in the database.
     * This table is managed by the application and its schema is defined here,
     * not part of the external pre-populated database asset.
     * This method is called after the main database is opened and any migrations are applied.
     */


    // --- Original Database Operations (modified to call openDatabase() and closeDatabase() consistently) ---

    // Search query
//    public Cursor readFromDatabase(String queryWord, String searchLangCode) {
//        openDatabase(); // Ensure database is open and migrated before querying
//        String query = "SELECT word, MIN(rowid) AS _id " +
//                "FROM (" +
//                "  SELECT LOWER(TRIM(word)) AS word, rowid " +
//                "  FROM eng_urdu_words " +
//                "  WHERE LOWER(TRIM(word)) LIKE ?" +
//                ") AS cleaned " +
//                "GROUP BY word " +
//                "ORDER BY CASE WHEN word = ? THEN 0 ELSE 1 END, word";
//
//        String cleanedWord = queryWord.trim().toLowerCase();
//        return database.rawQuery(query, new String[]{cleanedWord + "%", cleanedWord});
//    }
    public Cursor readFromDatabase(String queryWord, String searchLangCode) {
        openDatabase(); // Ensure database is open and migrated before querying

        String tableName;
        String columnName;
        String query; // Declare query variable here
        String[] queryArgs; // Declare queryArgs here

        // Determine which table and column to query based on searchLangCode

        if (searchLangCode.equals("ur")) {
            tableName = "eng_urdu_words";
            columnName = "urdu_meaning";

            // Urdu Query (simpler query as per your request)
            // This example performs a simple LIKE search and orders alphabetically
            query = "SELECT urdu_meaning AS word, MIN(rowid) AS _id " + // Inlined "_id" and "urdu_word"
                    "FROM " + tableName +
                    " WHERE urdu_meaning LIKE ? " +
                    " ORDER BY urdu_meaning ASC";

            queryArgs = new String[]{"%" + queryWord + "%"};
        }else{
            tableName = "eng_urdu_words";
            columnName = "word";

            // English Query (your existing complex query)
            query = "SELECT " + columnName + " AS word, MIN(rowid) AS _id " +
                    "FROM (" +
                    "  SELECT LOWER(TRIM(" + columnName + ")) AS word, rowid " +
                    "  FROM " + tableName +
                    "  WHERE LOWER(TRIM(" + columnName + ")) LIKE ?" +
                    ") AS cleaned " +
                    "GROUP BY word " +
                    "ORDER BY CASE WHEN word = ? THEN 0 ELSE 1 END, word";

            String cleanedWord = queryWord.trim().toLowerCase();
            queryArgs = new String[]{cleanedWord + "%", cleanedWord};

        }

        // Execute the determined query
        Cursor cursor = database.rawQuery(query, queryArgs);

        return cursor;
    }

    public Cursor getAllDefinitionsForWord(String word) {
        openDatabase(); // Ensure database is open and migrated

        // Original query (assuming 'example_sentence' does not exist in Version 1)
        String query = "SELECT * " +
                "FROM eng_urdu_words " +
                "WHERE LOWER(word) = LOWER(?)";

        return database.rawQuery(query, new String[]{word});
    }

    public Map<String, Object> getTodaysWord() {
        Cursor wordCursor = null;
        SpannableStringBuilder todaysWordString = new SpannableStringBuilder();
        int wordId = -1;
        String plainWord = ""; // Initialize to an empty string

        try {
            openDatabase();
            if (database == null) {
                Log.e("DatabaseHelper", "Database is null after openDatabase(). Returning empty data.");
                return new HashMap<>(); // Return an empty map to prevent an NPE
            }

            String query = "SELECT rowid AS _id, word, pos, urdu_meaning FROM eng_urdu_words ORDER BY RANDOM() LIMIT 1";
            wordCursor = database.rawQuery(query, null);

            if (wordCursor != null && wordCursor.moveToFirst()) {
                wordId = wordCursor.getInt(wordCursor.getColumnIndexOrThrow("_id"));
                String word = wordCursor.getString(wordCursor.getColumnIndexOrThrow("word"));
                String pos = wordCursor.getString(wordCursor.getColumnIndexOrThrow("pos"));
                String urduMeaning = wordCursor.getString(wordCursor.getColumnIndexOrThrow("urdu_meaning"));

                plainWord = word; // Store the plain word for SharedPreferences and TTS

                String formattedPos = "";
                if (pos != null) {
                    formattedPos = pos.toLowerCase();
                } else {
                    formattedPos = "N/A";
                }

                // 1. Add the Word
                int wordStart = todaysWordString.length();
                todaysWordString.append(word);
                int wordEnd = todaysWordString.length();
                todaysWordString.setSpan(new StyleSpan(Typeface.BOLD), wordStart, wordEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                todaysWordString.setSpan(new ForegroundColorSpan(Color.parseColor("#3F51B5")), wordStart, wordEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                // 2. Add Part of Speech
                todaysWordString.append(" ");
                int posStyledStart = todaysWordString.length();
                todaysWordString.append("(" + formattedPos + ")");
                int posStyledEnd = todaysWordString.length();
                todaysWordString.setSpan(new StyleSpan(Typeface.ITALIC), posStyledStart, posStyledEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                todaysWordString.setSpan(new ForegroundColorSpan(Color.GRAY), posStyledStart, posStyledEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                // 3. Add Urdu Meaning
                todaysWordString.append("\n");
                todaysWordString.append("\n");
                int urduStart = todaysWordString.length();
                todaysWordString.append(urduMeaning);
                int urduEnd = todaysWordString.length();
                todaysWordString.setSpan(new ForegroundColorSpan(Color.parseColor("#000000")), urduStart, urduEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "An error occurred while getting today's word.", e);
            // On any error, return an empty map to prevent an app crash
            return new HashMap<>();
        } finally {
            if (wordCursor != null) {
                wordCursor.close();
            }
            closeDatabase();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("word_id", wordId);
        map.put("wordString", todaysWordString);
        map.put("plainWord", plainWord); // Add the plain word to the map
        return map;
    }
    public Cursor getAllWords() {
        openDatabase();
        return database.rawQuery("SELECT rowid AS _id, word, urdu_meaning FROM eng_urdu_words", null);
    }
    public Cursor getWordsWithSynonymsAntonyms() {
        openDatabase();
        return database.query(
                "eng_urdu_words",
                new String[]{"word", "urdu_meaning", "synonym", "antonym"},
                null, null, null, null, null
        );
    }
    // In your DatabaseHelper.java
    public Cursor getWordsWithMeanings() {
        openDatabase(); // Make sure the database is open
        // Assuming your table is 'eng_urdu_words' and columns are 'word' and 'meaning'
        return database.query(
                "eng_urdu_words",
                new String[]{"word", "urdu_meaning"}, // Select word and meaning
                null, null, null, null, null
        );
    }

}