package com.neith.subjectdemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.neith.subjectdemo.data.SharedPrefsManager;
import com.neith.subjectdemo.R;
import com.neith.subjectdemo.ui.finance.FinanceActivity;

public class MainActivity extends AppCompatActivity {
    private Button btnRoleFinance;
    private SharedPrefsManager prefsManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnRoleFinance = findViewById(R.id.btnRoleFinance);
        prefsManager = new SharedPrefsManager(this);
        btnRoleFinance.setOnClickListener(v -> {
            // 1. Nạp role FN vào SharedPreferences
            prefsManager.setUserRole(SharedPrefsManager.ROLE_FINANCE);

            Toast.makeText(MainActivity.this, "Đã cấp quyền Tài chính (FN)", Toast.LENGTH_SHORT).show();

            // 2. Chuyển sang màn hình Quản lý Tài chính bằng Intent
            Intent intent = new Intent(MainActivity.this, FinanceActivity.class);
            startActivity(intent);
        });
    }
}