package com.neith.subjectdemo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.fn.FNActivity;
import com.neith.subjectdemo.fn.data.SharedPrefsManager;
import com.neith.subjectdemo.helper.SessionManager;
import com.neith.subjectdemo.hr.HRActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String DATABASE_NAME = "QLNSVATC_F.db";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        copyDatabase();

        Button btnHR = findViewById(R.id.btnHR);
        Button btnFinance = findViewById(R.id.btnFinance);
        Button btnExit = findViewById(R.id.btnExit);

        btnHR.setOnClickListener(v -> {
            // Lưu session giả để test HR
            SessionManager.saveLogin(this, "Admin_HR", "HR001");
            Intent intent = new Intent(this, HRActivity.class);
            intent.putExtra("USERNAME", "Admin_HR");
            intent.putExtra("AUTH", "HR001");
            startActivity(intent);
        });

        btnFinance.setOnClickListener(v -> {
            // Lưu session giả để test Finance
            SessionManager.saveLogin(this, "Finance_User", "FN001");
            
            SharedPrefsManager prefsManager = new SharedPrefsManager(this);
            prefsManager.setUserRole(SharedPrefsManager.ROLE_FINANCE);

            Toast.makeText(this, "Đang vào hệ thống Tài chính...", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(MainActivity.this, FNActivity.class);
            intent.putExtra("USERNAME", "Finance_User");
            intent.putExtra("AUTH", "FN001");
            startActivity(intent);
        });

        btnExit.setOnClickListener(v -> finishAffinity());
    }

    private void copyDatabase() {
        File dbFile = getDatabasePath(DATABASE_NAME);
        if (dbFile.exists()) return;

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
