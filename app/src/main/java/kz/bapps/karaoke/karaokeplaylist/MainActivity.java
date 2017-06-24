package kz.bapps.karaoke.karaokeplaylist;

import android.Manifest;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        SimpleItemTouchHelperCallback.OnItemTouchHelperAdapter,
        OnListFragmentInteractionListener {

    private static final int FILE_SELECT_CODE = 13515;
    private static final int REQUEST_CODE = 1;

    private static final String KEY_TEXT_VALUE = "SEARCH-FIELD";
    private static final String TEMP_PSWD = "12345";
    private static final String APP_NAME = "karaoke";
    private static final String FILE_PATH = "path";
    private static final String BLOCK_PSWD = "password";
    private static final String TAG = "MainActivity";

    private SharedPreferences pref;
    private RecyclerView mRecyclerView;
    private FrameLayout layoutBar;
    private SqliteHelper helper;

    private String currentQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        //=========== SQLite
        helper = new SqliteHelper(this);

        //=========== Toolbar
        getSupportActionBar().setTitle("Всего композиций " + helper.getKaraokeCount());
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        //=========== ProgressBar
        layoutBar = (FrameLayout) findViewById(R.id.layoutBar);

        //=========== RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        //=========== Touch Listeners for RecyclerView
//        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(this);
//        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
//        touchHelper.attachToRecyclerView(mRecyclerView);

        //=========== Get preferences
        pref = getSharedPreferences(APP_NAME,MODE_PRIVATE);
//        String path = pref.getString(FILE_PATH,"");
        String password = pref.getString(BLOCK_PSWD,"");

        if(password.isEmpty()) {
            try {

                MessageDigest md = MessageDigest.getInstance("MD5");
                password = new String(md.digest(TEMP_PSWD.getBytes()), Charset.forName("UTF-8"));

                pref.edit().putString(BLOCK_PSWD,password).apply();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        if (savedInstanceState != null) {
            currentQuery = savedInstanceState.getString(KEY_TEXT_VALUE);
        }
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(KEY_TEXT_VALUE, currentQuery);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(brReadXLS,new IntentFilter(KaraokeIntentService.BROADCAST_RECEIVER));
    }

    @Override
    public void onPause() {
        unregisterReceiver(brReadXLS);
        super.onPause();
    }

    /**
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat
                .getActionView(searchItem);
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        if (!TextUtils.isEmpty(currentQuery)) {
            searchItem.expandActionView();
            searchView.onActionViewExpanded();
            searchView.setQuery(currentQuery, true);
            searchView.clearFocus();

            List<Karaoke> karaokeList = helper.getKaraokesByQuery(currentQuery);
            RecyclerView.Adapter mAdapter = new KaraokeAdapter(MainActivity.this,
                    karaokeList,MainActivity.this);
            mRecyclerView.setAdapter(mAdapter);
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                currentQuery = query;
                List<Karaoke> karaokeList = helper.getKaraokesByQuery(currentQuery);
                RecyclerView.Adapter mAdapter = new KaraokeAdapter(MainActivity.this,
                        karaokeList,MainActivity.this);
                mRecyclerView.setAdapter(mAdapter);

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                currentQuery = newText;
                List<Karaoke> karaokeList = helper.getKaraokesByQuery(currentQuery);
                RecyclerView.Adapter mAdapter = new KaraokeAdapter(MainActivity.this,
                        karaokeList,MainActivity.this);
                mRecyclerView.setAdapter(mAdapter);

                return false;
            }
        });
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_load_file:
                if(isStoragePermissionGranted()) {
                    unblock(new OnAlertUnblock() {
                        @Override
                        public void onAlertUnblock() {
                            showFileChooser();
                        }
                    });
                }
                return true;
            case R.id.action_clear_all:
                unblock(new OnAlertUnblock() {
                    @Override
                    public void onAlertUnblock() {
                        clearKaraoke();
                    }
                });
                return true;
            case R.id.action_change_pswd:
                unblock(new OnAlertUnblock() {
                    @Override
                    public void onAlertUnblock() {
                        changePassword();
                    }
                });
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Окно выбора файла
     */
    private void showFileChooser() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.ms-excel");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Выбран файл"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Пожалуйста, установите файловый менеджер.",
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void clearKaraoke() {

        AlertDialog.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }

        builder.setTitle("Удаление")
                .setMessage("Вы действительно хотите всё удалить?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        helper.deleteKaraoke();
                        getSupportActionBar().setTitle("Всего композиций " + helper.getKaraokeCount());

                        Toast.makeText(MainActivity.this, "Все списки удалены!",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
            unblock(new OnAlertUnblock() {
                @Override
                public void onAlertUnblock() {
                    showFileChooser();
                }
            });
        }
    }

    /**
     *
     * Чтение файла Excel
     */
    BroadcastReceiver brReadXLS = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Integer count = intent.getIntExtra(KaraokeIntentService.EXTRA_COUNT, 0);
            Boolean finished = intent.getBooleanExtra(KaraokeIntentService.EXTRA_FINISHED, false);
            Boolean notFinished = intent.getBooleanExtra(KaraokeIntentService.EXTRA_NOT_FINISHED, false);

            MainActivity.this.getSupportActionBar().setTitle("Загружено " + count);

            if(finished || notFinished) {
                layoutBar.setVisibility(View.GONE);
                MainActivity.this.getSupportActionBar()
                        .setTitle("Всего композиций " + helper.getKaraokeCount());
            } else {
                layoutBar.setVisibility(View.VISIBLE);
            }

            if(notFinished) {
                Toast.makeText(MainActivity.this, "Файл не найден.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     *
     * @param requestCode
     * @param resultCode
     * @param data
     *
     * Получение результата активити
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d(TAG, "File Uri: " + uri.toString());
                    // Get the path
                    String path = null;
                    try {
                        path = this.getPath(this, uri);

                        pref.edit()
                                .putString(FILE_PATH,path)
                                .apply();

                        Log.d(TAG, "File Path: " + path);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    // Get the file instance

                    //====================

                    layoutBar.setVisibility(View.VISIBLE);
                    getSupportActionBar().setTitle("Загрузка файла..");

                    KaraokeIntentService.startActionReadXLS(this, path);

                    //====================

                    // Initiate the upload
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /**
     *
     * @param context
     * @param uri
     * @return
     * @throws URISyntaxException
     *
     * Получение файла в обзоре
     */
    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {

    }

    @Override
    public void onItemDismiss(int position) {

    }

    /**
     *
     * @param karaoke
     *
     * Получение окна при нажатии
     */
    @Override
    public void getWindow(Karaoke karaoke) {

        AlertDialog.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }

        builder.setTitle(karaoke.getId())
                .setMessage(karaoke.getArtist() +  " " + karaoke.getSong() +
                        " " + karaoke.getQuality())
                .setIcon(R.drawable.ic_action_music)
                .show();
    }

    /**
     * Callback for Alert
     */
    private interface OnAlertUnblock {
        void onAlertUnblock();
    }

    /**
     * Разблокировка
     */
    private void unblock(final OnAlertUnblock callback) {

        AlertDialog.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }

        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        builder .setTitle("Система")
                .setView(input)
                .setMessage("Введите пароль для разблокировки")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            MessageDigest md = MessageDigest.getInstance("MD5");
                            String password = new String(md.digest(input.getText()
                                    .toString().getBytes()), Charset.forName("UTF-8"));

                            if(pref.getString(BLOCK_PSWD,"").equals(password)) {
                                Toast.makeText(MainActivity.this, "Разблокировано.",
                                        Toast.LENGTH_SHORT).show();
                                callback.onAlertUnblock();
                            } else {
                                Toast.makeText(MainActivity.this, "Пароль не верен.",
                                        Toast.LENGTH_SHORT).show();

                                unblock(callback);
                            }

//                            pref.edit().putString(BLOCK_PSWD,password).apply();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "Отмена действия",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Смена пароля
     */
    private void changePassword() {

        AlertDialog.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText input = new EditText(this);
        layout.addView(input);
        final EditText input2 = new EditText(this);
        layout.addView(input2);

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        builder .setTitle("Новый пароль блокировки")
                .setView(layout)
                .setMessage("Введите новый пароль и подтвердите его")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(input.getText().toString().equals(input2.getText().toString())) {
                            try {

                                MessageDigest md = MessageDigest.getInstance("MD5");
                                String password = new String(md.digest(input.getText()
                                        .toString().getBytes()), Charset.forName("UTF-8"));

                                pref.edit().putString(BLOCK_PSWD,password).apply();

                                Toast.makeText(MainActivity.this, "Парол изменён.",
                                        Toast.LENGTH_SHORT).show();

                            } catch (NoSuchAlgorithmException e) {
                                e.printStackTrace();
                            }
                        } else {
                            changePassword();

                            Toast.makeText(MainActivity.this, "Пароли не совпадают.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        Toast.makeText(MainActivity.this, "Пароль не изменён.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


}
