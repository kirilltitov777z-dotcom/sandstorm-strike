package com.sandstorm.strike;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

public class MainActivity extends Activity {
    private GameView gameView;
    private static final int PICK_IMAGE = 42;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameView = new GameView(this, null);
        setContentView(gameView);
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
                // downscale to 256px wide for sky texture
                int tw = 256, th = Math.max(1, bmp.getHeight() * tw / bmp.getWidth());
                Bitmap small = Bitmap.createScaledBitmap(bmp, tw, th, true);
                gameView.setSkyPhoto(small);
            } catch (Exception ignored) {}
        }
    }

    @Override protected void onResume() { super.onResume(); if (gameView != null) gameView.running = true; }
    @Override protected void onPause() { super.onPause(); if (gameView != null) gameView.running = false; }
    @Override public void onBackPressed() {
        if (gameView != null && gameView.screen != GameView.S_MENU) gameView.screen = GameView.S_MENU;
        else super.onBackPressed();
    }
}
