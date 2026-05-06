package com.neith.subjectdemo.admin;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.DB;

public class AdminDataExecuteActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    private LinearLayout layoutBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_data_execute);

        db = DB.openDatabase(this);
        layoutBody = findViewById(R.id.layoutAdminDataExecuteBody);

        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        String sql = getIntent().getStringExtra("SQL_COMMAND");
        String title = getIntent().getStringExtra("TITLE");

        if (title != null) {
            TextView tvTitle = findViewById(R.id.txtExecuteTitle);
            tvTitle.setText(title);
        }

        if (sql != null && !sql.isEmpty()) {
            executeSQL(sql);
        }
    }

    private void executeSQL(String sql) {
        Cursor c = db.rawQuery(sql, null);
        openTableDataFromCursor(c);
    }

    private void openTableDataFromCursor(Cursor c) {
        layoutBody.removeAllViews();

        LinearLayout tableLayout = new LinearLayout(this);
        tableLayout.setOrientation(LinearLayout.VERTICAL);

        int colCount = c.getColumnCount();
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < colCount; i++) {
            TextView colName = new TextView(this);
            colName.setText(c.getColumnName(i));
            colName.setTextColor(0xFFFFFFFF);
            colName.setPadding(6,6,6,6);
            header.addView(colName, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        }
        tableLayout.addView(header);

        // Dữ liệu
        while (c.moveToNext()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 0; i < colCount; i++) {
                TextView cell = new TextView(this);
                cell.setText(c.getString(i) != null ? c.getString(i) : "");
                cell.setTextColor(0xFFFFFFFF);
                cell.setPadding(6,6,6,6);
                row.addView(cell, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            }
            tableLayout.addView(row);
        }
        c.close();

        ScrollView sv = new ScrollView(this);
        sv.addView(tableLayout);
        layoutBody.addView(sv);
    }
}