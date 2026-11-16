package com.sandstorm.strike;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private GameView gameView;
    private static final int PICK_IMAGE = 42;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        installCrashHandler();
        try {
            gameView = new GameView(this, null);
            setContentView(gameView);
        } catch (Throwable t) {
            saveCrash(t);
            Toast.makeText(this, "Ошибка запуска: " + t, Toast.LENGTH_LONG).show();
            throw t;
        }
    }

    private void installCrashHandler() {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            saveCrash(ex);
            if (prev != null) prev.uncaughtException(thread, ex);
        });
    }

    private void saveCrash(Throwable t) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("Sandstorm Strike crash " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
            pw.println("Android " + Build.VERSION.RELEASE + " / SDK " + Build.VERSION.SDK_INT);
            pw.println("Device " + Build.MANUFACTURER + " " + Build.MODEL);
            t.printStackTrace(pw);
            pw.flush();
            String text = sw.toString();

            // app-specific external file (always writable)
            File f = new File(getExternalFilesDir(null), "sandstorm_crash.txt");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(text.getBytes());
            fos.close();

            // try public Downloads too
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Downloads.DISPLAY_NAME, "sandstorm_crash.txt");
                cv.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri != null) {
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    if (os != null) { os.write(text.getBytes()); os.close(); }
                }
            } else {
                File pub = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "sandstorm_crash.txt");
                FileOutputStream o2 = new FileOutputStream(pub);
                o2.write(text.getBytes());
                o2.close();
            }
        } catch (Throwable ignored) {}
    }

    public void promptIp() {
        final EditText et = new EditText(this);
        et.setHint("192.168.1.10");
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
                .setTitle("IP хоста")
                .setMessage("Введи IP телефона-хоста (виден в LAN-меню у хоста или в настройках Wi-Fi)")
                .setView(et)
                .setPositiveButton("Войти", (d, which) -> {
                    String ip = et.getText().toString().trim();
                    if (!ip.isEmpty() && gameView != null) gameView.startLanJoin(ip);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    public void pickSky() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        try { startActivityForResult(intent, PICK_IMAGE); } catch (Exception ignored) {}
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK_IMAGE && res == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri uri = data.getData();
                Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                int tw = 512, th = Math.max(1, bmp.getHeight() * tw / Math.max(1, bmp.getWidth()));
                Bitmap small = Bitmap.createScaledBitmap(bmp, tw, th, true);
                gameView.setSkyPhoto(small);
            } catch (Exception ignored) {}
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (gameView != null) { gameView.running = true; gameView.postInvalidateOnAnimation(); }
    }
    @Override protected void onPause() {
        super.onPause();
        if (gameView != null) gameView.running = false;
    }
    @Override public void onBackPressed() {
        if (gameView != null && gameView.screen != GameView.S_MENU) {
            gameView.leaveLan();
            gameView.screen = GameView.S_MENU;
            gameView.postInvalidateOnAnimation();
        } else super.onBackPressed();
    }
}
