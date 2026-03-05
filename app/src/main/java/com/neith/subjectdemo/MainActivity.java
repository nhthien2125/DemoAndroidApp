package com.neith.subjectdemo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String DATABASE_NAME = "QLNSVATC_v2.db";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        copyDatabase();

        Button btnHR = findViewById(R.id.btnHR);
        Button btnFinance = findViewById(R.id.btnFinance);
        Button btnExit = findViewById(R.id.btnExit);
        btnHR.setOnClickListener(v ->
                startActivity(new Intent(this, HRActivity.class)));
        btnFinance.setOnClickListener(v ->
                Toast.makeText(this, "Chức năng tài chính đang phát triển", Toast.LENGTH_SHORT).show());
        btnExit.setOnClickListener(v -> finishAffinity());
    }

    private void copyDatabase() {
        File dbFile = getDatabasePath(DATABASE_NAME);
        Log.d("DB_PATH", dbFile.getPath());

        if (dbFile.exists()) {
            Log.d("DB_COPY", "DB đã tồn tại");
            return;
        }

        try {
            InputStream input = getAssets().open(DATABASE_NAME);
            dbFile.getParentFile().mkdirs();
            OutputStream output = new FileOutputStream(dbFile);

            byte[] buffer = new byte[1024];
            int len;

            while ((len = input.read(buffer)) > 0) {
                output.write(buffer, 0, len);
            }

            output.close();
            input.close();

            Toast.makeText(this, "Copy DB OK", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Copy DB lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}