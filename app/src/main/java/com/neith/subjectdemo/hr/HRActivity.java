package com.neith.subjectdemo.hr;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.neith.subjectdemo.R;
import com.neith.subjectdemo.auth.SignIn;
import com.neith.subjectdemo.helper.DB;
import com.neith.subjectdemo.helper.SessionManager;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.neith.subjectdemo.helper.BottomNav;
public class HRActivity extends AppCompatActivity {

    SQLiteDatabase db;
    LinearLayout layoutContent;

    final int BG = Color.parseColor("#0F172A");
    final int CARD = Color.parseColor("#303030");
    final int TEXT = Color.parseColor("#E5E7EB");
    final int SUB = Color.parseColor("#CBD5F5");
    final int PRIMARY = Color.parseColor("#FFD700");
    final int ORANGE = Color.parseColor("#F97316");
    final int GREEN = Color.parseColor("#22C55E");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hr);

        db = DB.openDatabase(this);
        layoutContent = findViewById(R.id.layoutContent);
        LinearLayout bottomNavContainer = findViewById(R.id.bottomNavContainer);
        BottomNav.setup(this, bottomNavContainer, BottomNav.HOME);
        loadDashboard();
    }

    private void loadDashboard() {
        layoutContent.removeAllViews();

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, dp(14));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        TextView title = makeText("Quản lý nhân sự", 26, PRIMARY, true);
        TextView subtitle = makeText("HR Dashboard", 13, SUB, false);

        titleBox.addView(title);
        titleBox.addView(subtitle);

        Button btnLogout = new Button(this);
        btnLogout.setText("Đăng xuất");
        btnLogout.setTextColor(Color.BLACK);
        btnLogout.setTextSize(13);
        btnLogout.setTypeface(null, Typeface.BOLD);
        btnLogout.setAllCaps(false);
        btnLogout.setBackgroundTintList(null);
        btnLogout.setBackgroundResource(R.drawable.hr_logout_bg);
        btnLogout.setPadding(dp(12), 0, dp(12), 0);

        btnLogout.setOnClickListener(v -> logout());

        topBar.addView(titleBox, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        topBar.addView(btnLogout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(44)
        ));

        layoutContent.addView(topBar);

        TextView sub = makeText(
                "Tổng quan nhân sự, lương thưởng, phòng ban và hiệu suất làm việc trong hệ thống QLNSVATC.",
                14,
                SUB,
                false
        );
        sub.setPadding(0, 0, 0, dp(18));
        layoutContent.addView(sub);

        int totalEmployees = getInt("SELECT COUNT(*) FROM NHANVIEN");
        int totalDepartments = getInt("SELECT COUNT(*) FROM PHONGBAN");
        int totalMale = getInt("SELECT COUNT(*) FROM NHANVIEN WHERE GIOITINH = 1");
        int totalFemale = getInt("SELECT COUNT(*) FROM NHANVIEN WHERE GIOITINH = 0");
        int insurance = getInt("SELECT COUNT(DISTINCT MANV) FROM THONGTINBAOHIEM");
        int health = getInt("SELECT COUNT(DISTINCT MANV) FROM THONGTINSUCKHOE");

        addStatCard(
                "Tổng số nhân viên",
                String.valueOf(totalEmployees),
                totalDepartments + " phòng ban đang quản lý",
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date())
        );

        addGenderCard(totalEmployees, totalMale, totalFemale);

        addStatCard(
                "Bảo hiểm & hồ sơ sức khỏe",
                insurance + " / " + totalEmployees,
                health + " nhân viên có hồ sơ sức khỏe.",
                "Hồ sơ"
        );

        String largestDept = getString(
                "SELECT PB.TENPB || ' (' || COUNT(NV.MANV) || ' nhân viên)' " +
                        "FROM PHONGBAN PB " +
                        "LEFT JOIN NHANVIEN NV ON PB.MAPB = NV.MAPB " +
                        "GROUP BY PB.MAPB, PB.TENPB " +
                        "ORDER BY COUNT(NV.MANV) DESC LIMIT 1"
        );

        addStatCard(
                "Phòng ban",
                String.valueOf(totalDepartments),
                "Đông nhất: " + largestDept,
                "Sắp xếp theo số lượng nhân viên"
        );

        addRankingCard(
                "Xếp hạng lương cơ bản",
                "Top 5 nhân viên",
                "SELECT NV.HOLOT || ' ' || NV.TENNV AS HOTEN, " +
                        "IFNULL(PB.TENPB, '(Chưa gán)') AS TENPB, " +
                        "IFNULL(L.LUONGCOBAN, 0) AS VALUE " +
                        "FROM NHANVIEN NV " +
                        "JOIN LUONG L ON NV.MANV = L.MANV " +
                        "LEFT JOIN PHONGBAN PB ON NV.MAPB = PB.MAPB " +
                        "ORDER BY VALUE DESC LIMIT 5",
                "VND"
        );

        addRankingCard(
                "Top thưởng",
                "Khen thưởng",
                "SELECT NV.HOLOT || ' ' || NV.TENNV AS HOTEN, " +
                        "IFNULL(PB.TENPB, '(Chưa gán)') AS TENPB, " +
                        "IFNULL(SUM(TP.TONG), 0) AS VALUE " +
                        "FROM DSTHUONGPHAT TP " +
                        "JOIN NHANVIEN NV ON TP.MANV = NV.MANV " +
                        "LEFT JOIN PHONGBAN PB ON NV.MAPB = PB.MAPB " +
                        "WHERE TP.HINHTHUC = 'KT' " +
                        "GROUP BY TP.MANV " +
                        "ORDER BY VALUE DESC LIMIT 5",
                "VND"
        );

        addRankingCard(
                "Top phạt",
                "Kỷ luật",
                "SELECT NV.HOLOT || ' ' || NV.TENNV AS HOTEN, " +
                        "IFNULL(PB.TENPB, '(Chưa gán)') AS TENPB, " +
                        "IFNULL(SUM(TP.TONG), 0) AS VALUE " +
                        "FROM DSTHUONGPHAT TP " +
                        "JOIN NHANVIEN NV ON TP.MANV = NV.MANV " +
                        "LEFT JOIN PHONGBAN PB ON NV.MAPB = PB.MAPB " +
                        "WHERE TP.HINHTHUC = 'KL' " +
                        "GROUP BY TP.MANV " +
                        "ORDER BY VALUE DESC LIMIT 5",
                "VND"
        );

        addDepartmentTable();

        addRankingCard(
                "Top tăng ca / tổng ca làm việc",
                "Dựa trên bảng chấm công",
                "SELECT NV.HOLOT || ' ' || NV.TENNV AS HOTEN, " +
                        "IFNULL(PB.TENPB, '(Chưa gán)') AS TENPB, " +
                        "IFNULL(SUM(CC.TONGCA), 0) AS VALUE " +
                        "FROM CHAMCONG CC " +
                        "JOIN NHANVIEN NV ON CC.MANV = NV.MANV " +
                        "LEFT JOIN PHONGBAN PB ON NV.MAPB = PB.MAPB " +
                        "GROUP BY CC.MANV " +
                        "ORDER BY VALUE DESC LIMIT 5",
                "ca"
        );

        addActivityLog();
    }

    private void logout() {
        SessionManager.logout(this);
        Intent intent = new Intent(this, SignIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void addStatCard(String title, String main, String desc, String extra) {
        LinearLayout card = makeCard();

        LinearLayout header = row();
        TextView t = makeText(title.toUpperCase(), 13, PRIMARY, true);
        TextView e = makeText(extra, 12, SUB, false);

        header.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        header.addView(e);
        card.addView(header);

        TextView m = makeText(main, 28, PRIMARY, true);
        m.setPadding(0, dp(8), 0, dp(4));
        card.addView(m);

        TextView d = makeText(desc, 13, SUB, false);
        card.addView(d);

        layoutContent.addView(card);
    }

    private void addGenderCard(int total, int male, int female) {
        LinearLayout card = makeCard();

        LinearLayout header = row();
        header.addView(makeText("CƠ CẤU GIỚI TÍNH", 13, PRIMARY, true),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        header.addView(makeText("Nam / Nữ", 12, SUB, false));
        card.addView(header);

        TextView main = makeText(male + " nam", 22, PRIMARY, true);
        main.setPadding(0, dp(8), 0, 0);
        card.addView(main);

        card.addView(makeText(female + " nữ", 13, SUB, false));

        int malePercent = total == 0 ? 0 : male * 100 / total;
        int femalePercent = total == 0 ? 0 : female * 100 / total;

        addProgress(card, "Tỷ lệ nam", malePercent, PRIMARY);
        addProgress(card, "Tỷ lệ nữ", femalePercent, GREEN);

        layoutContent.addView(card);
    }

    private void addRankingCard(String title, String extra, String sql, String unit) {
        LinearLayout card = makeCard();

        LinearLayout header = row();
        header.addView(makeText(title.toUpperCase(), 13, PRIMARY, true),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        header.addView(makeText(extra, 12, SUB, false));
        card.addView(header);

        Cursor c = db.rawQuery(sql, null);

        double max = 0;
        while (c.moveToNext()) {
            double value = c.getDouble(2);
            if (value > max) max = value;
        }

        c.moveToPosition(-1);

        if (c.getCount() == 0) {
            card.addView(makeText("Chưa có dữ liệu.", 13, SUB, false));
        } else {
            while (c.moveToNext()) {
                String name = c.getString(0);
                String dept = c.getString(1);
                double value = c.getDouble(2);

                int percent = max == 0 ? 0 : (int) ((value / max) * 100);

                TextView nameView = makeText(name, 14, TEXT, true);
                nameView.setPadding(0, dp(10), 0, 0);
                card.addView(nameView);

                TextView deptView = makeText(dept, 12, SUB, false);
                card.addView(deptView);

                addProgress(card, formatNumber(value) + " " + unit, percent, PRIMARY);
            }
        }

        c.close();
        layoutContent.addView(card);
    }

    private void addDepartmentTable() {
        LinearLayout card = makeCard();

        LinearLayout header = row();
        header.addView(makeText("TỔNG HỢP PHÒNG BAN", 13, PRIMARY, true),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        header.addView(makeText("Nam / Nữ theo phòng", 12, SUB, false));
        card.addView(header);

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setPadding(0, dp(8), 0, 0);

        table.addView(makeTableRow("Mã phòng", "Tên phòng ban", "Tổng", "Nam", "Nữ", true));

        Cursor c = db.rawQuery(
                "SELECT PB.MAPB, PB.TENPB, " +
                        "COUNT(NV.MANV) AS TOTAL, " +
                        "SUM(CASE WHEN NV.GIOITINH = 1 THEN 1 ELSE 0 END) AS NAM, " +
                        "SUM(CASE WHEN NV.GIOITINH = 0 THEN 1 ELSE 0 END) AS NU " +
                        "FROM PHONGBAN PB " +
                        "LEFT JOIN NHANVIEN NV ON PB.MAPB = NV.MAPB " +
                        "GROUP BY PB.MAPB, PB.TENPB " +
                        "ORDER BY TOTAL DESC",
                null
        );

        if (c.getCount() == 0) {
            table.addView(makeText("Chưa có dữ liệu phòng ban.", 13, SUB, false));
        } else {
            while (c.moveToNext()) {
                table.addView(makeTableRow(
                        c.getString(0),
                        c.getString(1),
                        String.valueOf(c.getInt(2)),
                        String.valueOf(c.getInt(3)),
                        String.valueOf(c.getInt(4)),
                        false
                ));
            }
        }

        c.close();

        hsv.addView(table);
        card.addView(hsv);
        layoutContent.addView(card);
    }

    private void addActivityLog() {
        LinearLayout card = makeCard();

        LinearLayout header = row();
        header.addView(makeText("HOẠT ĐỘNG GẦN ĐÂY", 13, PRIMARY, true),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        header.addView(makeText("Thống kê từ hệ thống", 12, SUB, false));
        card.addView(header);

        Cursor c = db.rawQuery(
                "SELECT ActionCode, Description, PerformedBy, AcTionTime " +
                        "FROM ACTIVITY_LOG " +
                        "ORDER BY AcTionTime DESC LIMIT 8",
                null
        );

        if (c.getCount() == 0) {
            card.addView(makeText("Chưa có log hoạt động.", 13, SUB, false));
        } else {
            while (c.moveToNext()) {
                String action = c.getString(0);
                String desc = c.getString(1);
                String user = c.getString(2);
                String time = c.getString(3);

                TextView log = makeText("[" + action + "] " + desc, 13, TEXT, false);
                log.setPadding(0, dp(10), 0, 0);
                card.addView(log);

                TextView meta = makeText(user + " • " + time, 11, SUB, false);
                card.addView(meta);

                View line = new View(this);
                line.setBackgroundColor(Color.parseColor("#111827"));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                );
                lp.setMargins(0, dp(8), 0, 0);

                card.addView(line, lp);
            }
        }

        c.close();
        layoutContent.addView(card);
    }

    private LinearLayout makeTableRow(String c1, String c2, String c3, String c4, String c5, boolean header) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));

        row.addView(tableCell(c1, 90, header));
        row.addView(tableCell(c2, 180, header));
        row.addView(tableCell(c3, 70, header));
        row.addView(tableCell(c4, 70, header));
        row.addView(tableCell(c5, 70, header));

        return row;
    }

    private TextView tableCell(String text, int widthDp, boolean header) {
        TextView tv = makeText(text, header ? 12 : 13, header ? SUB : TEXT, header);
        tv.setWidth(dp(widthDp));
        tv.setPadding(dp(4), 0, dp(4), 0);
        return tv;
    }

    private void addProgress(LinearLayout parent, String label, int percent, int color) {
        TextView tv = makeText(label + "  " + percent + "%", 12, SUB, false);
        tv.setPadding(0, dp(8), 0, dp(4));
        parent.addView(tv);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(percent);
        bar.setProgressTintList(ColorStateList.valueOf(color));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#111827")));

        parent.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(8)
        ));
    }

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.hr_card_bg);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(lp);

        return card;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private TextView makeText(String text, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text == null ? "" : text);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setLineSpacing(dp(2), 1.0f);

        if (bold) {
            tv.setTypeface(null, Typeface.BOLD);
        }

        return tv;
    }

    private int getInt(String sql) {
        Cursor c = db.rawQuery(sql, null);
        int value = 0;

        if (c.moveToFirst()) {
            value = c.getInt(0);
        }

        c.close();
        return value;
    }

    private String getString(String sql) {
        Cursor c = db.rawQuery(sql, null);
        String value = "";

        if (c.moveToFirst()) {
            value = c.getString(0);
        }

        c.close();
        return value;
    }

    private String formatNumber(double number) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(number);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}