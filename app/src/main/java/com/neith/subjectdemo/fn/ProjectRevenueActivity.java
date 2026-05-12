package com.neith.subjectdemo.fn;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.helper.ExcelExporter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProjectRevenueActivity extends AppCompatActivity {

    private SQLiteDatabase db;
    private TextView tvTotalRevenue, tvCompletedCount, tvAvgRevenue;
    private CombinedChartView combinedChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_revenue);

        db = DB.openDatabase(this);

        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvAvgRevenue = findViewById(R.id.tvAvgRevenue);
        combinedChart = findViewById(R.id.combinedChart);

        findViewById(R.id.btnExport).setOnClickListener(v -> exportProjectRevenueReport());

        loadData();
    }

    private void loadData() {
        double totalRevenue = 0;
        int completedProjects = 0;
        Map<String, Double> monthlyRevenue = new LinkedHashMap<>();
        List<Double> marketIndices = new ArrayList<>();

        try {
            Cursor cTotal = db.rawQuery("SELECT SUM(TIENNGHIEMTHU_TONG) FROM DTDUAN", null);
            if (cTotal.moveToFirst()) totalRevenue = cTotal.getDouble(0);
            cTotal.close();

            Cursor cCompleted = db.rawQuery(
                    "SELECT COUNT(*) FROM DUANTHEOHOPDONG WHERE NGAYKT < '2026-03-12'", null);
            if (cCompleted.moveToFirst()) completedProjects = cCompleted.getInt(0);
            cCompleted.close();

            Cursor cMonthly = db.rawQuery(
                    "SELECT strftime('%m/%Y', NGAYKT) as month, SUM(TIENNGHIEMTHU_TONG) " +
                            "FROM DUANTHEOHOPDONG DTHD " +
                            "JOIN DTDUAN DT ON DTHD.MADA = DT.MADA " +
                            "GROUP BY month ORDER BY NGAYKT", null
            );
            while (cMonthly.moveToNext()) {
                monthlyRevenue.put(cMonthly.getString(0), cMonthly.getDouble(1) / 1_000_000_000.0);
            }
            cMonthly.close();

            Cursor cMarket = db.rawQuery("SELECT HSTHITRUONG FROM BIENDONGTHITRUONG ORDER BY THOIGIAN", null);
            while (cMarket.moveToNext()) {
                marketIndices.add(cMarket.getDouble(0));
            }
            cMarket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        tvTotalRevenue.setText(formatCurrencyB(totalRevenue));
        tvCompletedCount.setText(String.valueOf(completedProjects));
        tvAvgRevenue.setText(formatCurrencyB(completedProjects > 0 ? totalRevenue / completedProjects : 0));

        if (monthlyRevenue.isEmpty()) {
            monthlyRevenue.put("04/2025", 5.0);
            monthlyRevenue.put("06/2025", 12.0);
            monthlyRevenue.put("08/2025", 8.0);
            monthlyRevenue.put("10/2025", 4.0);
            monthlyRevenue.put("12/2025", 10.0);
            monthlyRevenue.put("02/2026", 45.0);
        }
        if (marketIndices.isEmpty()) {
            marketIndices.add(1.0);
            marketIndices.add(1.1);
            marketIndices.add(0.4);
            marketIndices.add(1.5);
            marketIndices.add(1.4);
            marketIndices.add(0.8);
            marketIndices.add(1.8);
        }

        combinedChart.setData(monthlyRevenue, marketIndices);
    }

    private void exportProjectRevenueReport() {
        String query = "SELECT IFNULL(HD.TENHD, DTHD.MADAHD), DTHD.MADAHD, IFNULL(DT.TIENNGHIEMTHU_TONG, 0), DTHD.NGAYKT " +
                "FROM DUANTHEOHOPDONG DTHD " +
                "JOIN DTDUAN DT ON DTHD.MADA = DT.MADA " +
                "JOIN HOPDONG HD ON DTHD.MAHD = HD.MAHD " +
                "ORDER BY DTHD.NGAYKT DESC";
        
        Cursor cursor = db.rawQuery(query, null);
        String[] columns = {"Tên Dự Án", "Mã Dự Án", "Doanh Thu Thực Tế", "Ngày Kết Thúc"};
        ExcelExporter.exportCursorToExcel(this, cursor, "Bao_Cao_Doanh_Thu_Du_An", columns);
        cursor.close();
    }

    private String formatCurrencyB(double amount) {
        return String.format(Locale.US, "$%.1fB", amount / 1_000_000_000.0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null && db.isOpen()) db.close();
    }
}
