package com.neith.subjectdemo.admin;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.BottomNav;
import com.neith.subjectdemo.helper.DB;

public class AdminDataActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    private LinearLayout layoutTableList;
    private EditText edtSQL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_data);

        db = DB.openDatabase(this);
        layoutTableList = findViewById(R.id.layoutTableList);
        edtSQL = findViewById(R.id.edtSQLConsole);

        BottomNav.setupAdmin(this, findViewById(R.id.bottomNavContainer), BottomNav.ADMIN_DATA);

        findViewById(R.id.btnExecuteSQL).setOnClickListener(v -> executeSQL());

        loadTableList();
    }

    private void loadTableList() {
        layoutTableList.removeAllViews();
        Cursor c = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%' ORDER BY name",
                null
        );
        while (c.moveToNext()) {
            String tableName = c.getString(0);

            TextView tv = new TextView(this);
            tv.setText(tableName);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(16);
            tv.setPadding(0,10,0,10);
            tv.setOnClickListener(v -> openTableData(tableName));

            layoutTableList.addView(tv);
        }
        c.close();
    }

    private void openTableData(String tableName) {
        String sql = "SELECT * FROM " + tableName;
        Intent intent = new Intent(this, AdminDataExecuteActivity.class);
        intent.putExtra("SQL_COMMAND", sql);
        intent.putExtra("TITLE", tableName); // Truyền tên bảng
        startActivity(intent);
    }

    private void executeSQL() {
        String sql = edtSQL.getText().toString().trim();
        if (sql.isEmpty()) {
            Toast.makeText(this, "Nhập lệnh SQL", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, AdminDataExecuteActivity.class);
        intent.putExtra("SQL_COMMAND", sql);
        intent.putExtra("TITLE", "Kết quả SQL");
        startActivity(intent);
    }
}