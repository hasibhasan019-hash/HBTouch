package com.hb.touch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(48, 96, 48, 48);

        TextView tv = new TextView(this);
        tv.setTextSize(16);
        tv.setText("HB Touch\n\nAccessibility e giye \"HB Touch\" ON koro.\n"
                + "Tarpor Home screen er khali jaygay double tap korle display off hobe.\n");

        Button btn = new Button(this);
        btn.setText("Accessibility Settings kholo");
        btn.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        root.addView(tv);
        root.addView(btn);
        setContentView(root);
    }
}
