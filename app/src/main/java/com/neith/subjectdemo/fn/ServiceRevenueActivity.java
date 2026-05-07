package com.neith.subjectdemo.fn;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.helper.SessionManager;
import com.neith.subjectdemo.hr.EmployeeProfileActivity;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class ServiceRevenueActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    private TextView tvTotalRevenue, tvOrderCount, tvAvgValue;
    private DonutChartView donutChart;
    private GridLayout glLegend;
    private ImageView ivMenu;

    private String username = "";
    private String auth = "";
    private String maNV = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_revenue);

        db = DB.openDatabase(this);

        username = getIntent().getStringExtra("USERNAME");
        auth = getIntent().getStringExtra("AUTH");
        findMaNV();

        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvOrderCount = findViewById(R.id.tvOrderCount);
        tvAvgValue = findViewById(R.id.tvAvgValue);
        donutChart = findViewById(R.id.donutChart);
        glLegend = findViewById(R.id.glLegend);
        ivMenu = findViewById(R.id.ivMenu);

        ivMenu.setOnClickListener(this::showMenu);

        findViewById(R.id.btnExport).setOnClickListener(v -> 
                Toast.makeText(this, "Đang chuẩn bị báo cáo...", Toast.LENGTH_SHORT).show());

        loadData();
    }

    private void findMaNV() {
        if (auth != null && !auth.isEmpty()) {
            try {
                Cursor c = db.rawQuery("SELECT CODE FROM CONFIRMAUTH WHERE AUTH = ?", new String[]{auth});
                if (c.moveToFirst()) {
                    maNV = c.getString(0);
                }
                c.close();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void showMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.fn_main_menu, popup.getMenu());

        android.view.MenuItem profileItem = popup.getMenu().findItem(R.id.menu_user_profile);
        if (profileItem != null && username != null && !username.isEmpty()) {
            profileItem.setTitle("Hello, " + username);
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_user_profile) {
                if (maNV != null && !maNV.isEmpty()) {
                    Intent intent = new Intent(this, EmployeeProfileActivity.class);
                    intent.putExtra("MANV", maNV);
                    startActivity(intent);
                }
                return true;
            } else if (id == R.id.menu_logout) {
                SessionManager.logout(this);
                return true;
            } else if (id == R.id.menu_revenue_project) {
                Intent intent = new Intent(this, ProjectRevenueActivity.class);
                intent.putExtra("USERNAME", username);
                intent.putExtra("AUTH", auth);
                startActivity(intent);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void loadData() {
        double totalRevenue = 0;
        int orderCount = 0;
        Map<String, Double> categoryMap = new LinkedHashMap<>();

        try {
            Cursor cursor = db.rawQuery("SELECT LHDICHVU, SUM(GIACATONG), COUNT(*) FROM DTDICHVU GROUP BY LHDICHVU", null);
            while (cursor.moveToNext()) {
                String type = cursor.getString(0);
                if (type == null) type = "Khác";
                double amount = cursor.getDouble(1);
                int count = cursor.getInt(2);
                totalRevenue += amount;
                orderCount += count;
                categoryMap.put(type, amount);
            }
            cursor.close();
        } catch (Exception e) { e.printStackTrace(); }

        tvTotalRevenue.setText(formatCurrency(totalRevenue));
        tvOrderCount.setText(String.valueOf(orderCount));
        tvAvgValue.setText(formatCurrency(orderCount > 0 ? totalRevenue / orderCount : 0));

        donutChart.setData(categoryMap);
        setupLegend(categoryMap);
    }

    private void setupLegend(Map<String, Double> data) {
        glLegend.removeAllViews();
        String[] colorPalette = {"#BD5E00", "#3B82F6", "#EC4899", "#EAB308", "#06B6D4", "#10B981"};
        int i = 0;
        for (String key : data.keySet()) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(0, 0, 20, 10);
            View dot = new View(this);
            dot.setBackgroundColor(Color.parseColor(colorPalette[i % colorPalette.length]));
            item.addView(dot, new LinearLayout.LayoutParams(24, 24));
            TextView tv = new TextView(this);
            tv.setText(" " + key.toUpperCase());
            tv.setTextColor(Color.parseColor("#8B949E"));
            tv.setTextSize(10);
            item.addView(tv);
            glLegend.addView(item);
            i++;
        }
    }

    private String formatCurrency(double amount) {
        if (amount >= 1_000_000) return String.format(Locale.US, "$%.1fM", amount / 1_000_000.0);
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }
}
