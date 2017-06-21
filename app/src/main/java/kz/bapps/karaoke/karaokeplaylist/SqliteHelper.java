package kz.bapps.karaoke.karaokeplaylist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by danixoid on 16.06.17.
 */

public class SqliteHelper extends SQLiteOpenHelper {

    // Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "karaoke_list";

    // Karaokes table name
    public static final String TABLE_KARAOKE = "karaoke";

    // Karaokes Table Columns names
    public static final String KEY_ID = "id";
    public static final String KEY_COMP_ID = "comp_id";
    public static final String KEY_ARTIST = "artist";
    public static final String KEY_SONG = "song";

    public static final String[] COLUMNS = {KEY_ID,KEY_COMP_ID,KEY_ARTIST,KEY_SONG};
    
    // SQL statement to create karaoke table
    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_KARAOKE + " ( " +
                    KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    KEY_COMP_ID + " TEXT, "+
                    KEY_ARTIST + " TEXT, "+
                    KEY_SONG + " TEXT )";

    public SqliteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // create Karaokes table
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older karaoke table if existed
        db.execSQL("DROP TABLE IF EXISTS karaoke");

        // create fresh Karaokes table
        this.onCreate(db);
    }


    public void addKaraoke(Karaoke karaoke) {
        //for logging
        Log.d("addKaraoke", karaoke.toString());
        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_COMP_ID, karaoke.getId()); // get title
        values.put(KEY_ARTIST, karaoke.getArtist()); // get author
        values.put(KEY_SONG, karaoke.getSong()); // get author

        // 3. insert
        db.insert(TABLE_KARAOKE, // table
                null, //nullColumnHack
                values); // key/value -> keys = column names/ values = column values

        // 4. close
        db.close();
    }

    public List<Karaoke> getKaraokesByQuery(String search) {

        if(search.length() < 2) return new ArrayList<>();

        List<Karaoke> Karaokes = new LinkedList<>();

        String[] searches = search.split("\\s");

        // 1. build the query
        String query =
                       "SELECT  * FROM " + TABLE_KARAOKE +
                       " WHERE ";

        boolean next_line = false;
        for(String str : searches) {
            query += (next_line ? " AND " : "") +
                    "(" + KEY_ARTIST + " LIKE '%" + str +
                    "%' OR " + KEY_SONG + " LIKE '%" + str + "%')";
            next_line = true;
        }

//        query += " LIMIT 50";

        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        // 3. go over each row, build Karaoke and add it to list
        Karaoke karaoke = null;
        if (cursor.moveToFirst()) {
            do {

                karaoke = new Karaoke();
                karaoke.setId(cursor.getString(1));
                karaoke.setArtist(cursor.getString(2));
                karaoke.setSong(cursor.getString(3));

                // Add Karaoke to Karaokes
                Karaokes.add(karaoke);
            } while (cursor.moveToNext());
        }

        Log.d("getAllKaraokes()", Karaokes.toString());

        // return Karaokes
        return Karaokes;
   }


   public void deleteKaraoke() {

       SQLiteDatabase db = this.getWritableDatabase();
       db.delete(TABLE_KARAOKE,"",null);
       db.close();
   }

   public int getKaraokeCount() {

       String countQuery = "SELECT  * FROM " + TABLE_KARAOKE;
       SQLiteDatabase db = this.getReadableDatabase();
       Cursor cursor = db.rawQuery(countQuery, null);
       int cnt = cursor.getCount();
       cursor.close();
       return cnt;
   }

   public Karaoke getKaraokeByCompId(String compId) {

       String countQuery = "SELECT  * FROM " + TABLE_KARAOKE + " WHERE comp_id = '" + compId + "'";
       SQLiteDatabase db = this.getReadableDatabase();
       Cursor cursor = db.rawQuery(countQuery, null);

       Karaoke karaoke = new Karaoke();

       if (cursor.moveToFirst()) {
           karaoke.setId(cursor.getString(1));
           karaoke.setArtist(cursor.getString(2));
           karaoke.setSong(cursor.getString(3));
       }

       cursor.close();
       db.close();

       return karaoke;
   }

}
