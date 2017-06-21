package kz.bapps.karaoke.karaokeplaylist;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.IOException;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class KaraokeIntentService extends IntentService {
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS

    public static final String BROADCAST_RECEIVER = "kz.bapps.karaoke.karaokeplaylist.broadcast.receiver";
    public static final String EXTRA_COUNT = "kz.bapps.karaoke.karaokeplaylist.extra.COUNT";
    public static final String EXTRA_FINISHED = "kz.bapps.karaoke.karaokeplaylist.extra.FINISHED";
    public static final String EXTRA_NOT_FINISHED = "kz.bapps.karaoke.karaokeplaylist.extra.NOT.FINISHED";

    private static final String ACTION_READ_XLS = "kz.bapps.karaoke.karaokeplaylist.action.ReadXLS";
    private static final String EXTRA_PATH = "kz.bapps.karaoke.karaokeplaylist.extra.FILE_PATH";


    public KaraokeIntentService() {
        super("KaraokeIntentService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionReadXLS(Context context, String path) {
        Intent intent = new Intent(context, KaraokeIntentService.class);
        intent.setAction(ACTION_READ_XLS);
        intent.putExtra(EXTRA_PATH, path);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_READ_XLS.equals(action)) {
                final String path = intent.getStringExtra(EXTRA_PATH);
                handleActionReadXLS(path);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionReadXLS(String path) {
        File file = new File(path);

        final Intent intent = new Intent(BROADCAST_RECEIVER);

        if(file.exists()){
            Workbook w;
            try {
                w = Workbook.getWorkbook(file);
                // Get the first sheet
                Sheet sheet = w.getSheet(0);
                // Loop over column and lines

                SqliteHelper sqHelper = new SqliteHelper(this);
                SQLiteDatabase db = sqHelper.getWritableDatabase();

                for (int j = 0; j < sheet.getRows(); j++) {

                    ContentValues values = new ContentValues();
                    values.put(SqliteHelper.KEY_COMP_ID, sheet.getCell(0, j).getContents());
                    values.put(SqliteHelper.KEY_SONG, sheet.getCell(1, j).getContents());
                    values.put(SqliteHelper.KEY_ARTIST, sheet.getCell(2, j).getContents());
                    values.put(SqliteHelper.KEY_QUALITY, sheet.getCell(3, j).getContents());
                    db.insert(SqliteHelper.TABLE_KARAOKE, null, values);

                    intent.putExtra(EXTRA_COUNT, j);
                    if(j % 100 == 0 || j + 100 > sheet.getRows()) {
                        new Runnable() {
                            public void run() {
                                sendBroadcast(intent);
                            }
                        }.run();
                    }
                }

                db.close();

                intent.putExtra(EXTRA_FINISHED, true);
                new Runnable() {
                    public void run() {
                        sendBroadcast(intent);
                    }
                }.run();

                return;

            } catch (BiffException | IOException e) {
                e.printStackTrace();
            }
        }

        intent.putExtra(EXTRA_NOT_FINISHED, true);

        new Runnable() {
            public void run() {
                sendBroadcast(intent);
            }
        }.run();
    }

}
