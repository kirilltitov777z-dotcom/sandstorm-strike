package com.sandstorm.strike;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.graphics.Color;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Sandstorm Strike — build OK");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(24);
        setContentView(tv);
    }
}
